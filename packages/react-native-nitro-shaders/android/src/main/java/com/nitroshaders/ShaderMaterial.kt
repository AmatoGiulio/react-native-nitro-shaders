package com.margelo.nitro.nitroshaders

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Strategy for a single shader material. Each implementation owns its own Paint,
 * RuntimeShader cache, uniform binding and draw logic. [draw] is used on API >= 33
 * (Tiramisu) where AGSL RuntimeShaders are available; [drawFallback] handles API < 33.
 */
internal interface ShaderMaterial {
    val name: String

    fun isAvailable(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    fun draw(canvas: Canvas, view: ShaderSurfaceView, w: Float, h: Float)

    fun drawFallback(canvas: Canvas, view: ShaderSurfaceView, w: Float, h: Float)
}

/**
 * "materialOrb" — organic Path + soft contact shadow + AGSL material shaded with
 * an equirectangular environment BitmapShader.
 *
 * The AGSL program is assembled per material: orb-core.agsl (shared library) +
 * material-<name>.agsl (surfaceColor) + orb-main.agsl (entry point), then cached.
 * All semantic params arrive as uniforms already resolved (motion overrides applied
 * here on the Kotlin side), so patterns/materials stay swappable at runtime.
 */
internal class MaterialOrbMaterial : ShaderMaterial {
    override val name: String = "materialOrb"

    private val materialOrbPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val materialOrbShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val materialOrbPath = Path()

    // Offscreen pyramid for the soft cast shadow: iterated down/upscale passes
    // approximate a gaussian blur without MaskFilter (unreliable on HW canvases).
    private val shadowSrcBitmap = Bitmap.createBitmap(SHADOW_SRC_SIZE, SHADOW_SRC_SIZE, Bitmap.Config.ARGB_8888)
    private val shadowSrcCanvas = Canvas(shadowSrcBitmap)
    private val shadowPyramid = intArrayOf(48, 24, 12).map { s ->
        val bmp = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
        bmp to Canvas(bmp)
    }
    private val shadowBitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val shadowMatrix = android.graphics.Matrix()
    private val shadowSrcRect = android.graphics.RectF()
    private val shadowDstRect = android.graphics.RectF()

    private companion object {
        const val SHADOW_SRC_SIZE = 96

        val MATERIAL_NAMES = setOf("metal", "water", "iridescent", "glass")

        // Silhouette family: smooth wax blob (water/glass) vs liquid pebble
        // (metal/iridescent, lobes + traveling dents).
        fun isSmoothSilhouette(material: String) = material == "water" || material == "glass"

        // Perceived silhouette density per material (higher = stiffer profile).
        fun silhouetteDensity(material: String): Float = when (material) {
            "metal" -> 0.92f
            "water" -> 0.45f
            "glass" -> 0.45f
            else -> 0.62f // iridescent
        }

        // Contact shadow strength per material.
        fun shadowStrength(material: String): Int = when (material) {
            "metal" -> 30
            "water" -> 20
            "glass" -> 28
            else -> 24 // iridescent
        }

        // Default env asset per material (validated defaults).
        fun defaultEnvAsset(material: String): String = when (material) {
            "water" -> "env/water-env.png"   // sunset sea (frozen 2026-07-06)
            "glass" -> "env/glass-env.png"   // dark teal lab-2 (frozen 2026-07-06)
            else -> "env/studio.png"          // metal/iridescent: dark studio
        }

        // Default pattern per material ('auto' resolution). Mirrors MATERIAL_PRESETS.
        fun defaultPatternIndex(material: String): Float = when (material) {
            "water" -> 2f      // ripples
            "glass" -> 1f      // bands
            else -> 0f          // folds (metal/iridescent)
        }

        fun patternIndex(pattern: String, material: String): Float = when (pattern) {
            "folds" -> 0f
            "bands" -> 1f
            "ripples" -> 2f
            else -> defaultPatternIndex(material)
        }
    }

    private val fallbackPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    // One assembled+compiled RuntimeShader per material, built lazily.
    private val shaderCache = HashMap<String, RuntimeShader>()

    // Environment bitmaps: per-material defaults + runtime lab overrides, lazy.
    private val envCache = HashMap<String, Pair<Bitmap, BitmapShader>>()

    private fun loadEnvBitmap(view: ShaderSurfaceView, asset: String): Bitmap {
        val bmp = try {
            view.appContext.assets.open(asset).use { BitmapFactory.decodeStream(it) }
        } catch (e: Exception) {
            null
        }
        return bmp ?: Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888).also {
            it.eraseColor(Color.rgb(128, 130, 134))
        }
    }

    private fun envFor(view: ShaderSurfaceView, material: String): Pair<Bitmap, BitmapShader> {
        val labIndex = view.environment.toInt()
        val asset = if (labIndex >= 0) "env/lab-$labIndex.png" else defaultEnvAsset(material)
        envCache[asset]?.let { return it }
        val bmp = loadEnvBitmap(view, asset)
        val pair = bmp to BitmapShader(bmp, Shader.TileMode.REPEAT, Shader.TileMode.CLAMP)
        envCache[asset] = pair
        return pair
    }

    private fun resolvedMaterial(view: ShaderSurfaceView): String {
        return if (MATERIAL_NAMES.contains(view.material)) view.material else "metal"
    }

    private fun materialShader(view: ShaderSurfaceView, material: String): RuntimeShader? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return null
        }
        shaderCache[material]?.let { return it }
        return try {
            val assets = view.appContext.assets
            fun read(name: String): String =
                assets.open("shaders/$name").bufferedReader().use { it.readText() }
            val source = read("orb-core.agsl") + "\n" +
                read("material-$material.agsl") + "\n" +
                read("orb-main.agsl")
            RuntimeShader(source).also { shaderCache[material] = it }
        } catch (e: Exception) {
            null
        }
    }

    override fun draw(canvas: Canvas, view: ShaderSurfaceView, w: Float, h: Float) {
        val material = resolvedMaterial(view)
        val shader = materialShader(view, material) ?: return drawFallback(canvas, view, w, h)
        setMaterialOrbUniforms(view, shader, material, w, h)
        materialOrbPaint.shader = shader
        drawMaterialOrb(view, canvas, material, w, h)
    }

    override fun drawFallback(canvas: Canvas, view: ShaderSurfaceView, w: Float, h: Float) {
        fallbackPaint.color = Color.TRANSPARENT
        canvas.drawRect(0f, 0f, w, h, fallbackPaint)
    }

    private fun setMaterialOrbUniforms(
        view: ShaderSurfaceView,
        shader: RuntimeShader,
        material: String,
        w: Float,
        h: Float,
    ) {
        val (envBitmap, envShader) = envFor(view, material)
        shader.setInputShader("u_env", envShader)
        shader.setFloatUniform("u_envSize", envBitmap.width.toFloat(), envBitmap.height.toFloat())
        shader.setFloatUniform("u_resolution", w, h)
        shader.setFloatUniform("u_time", view.currentTimeSeconds())

        // Semantic params, RESOLVED here (motion overrides win when set).
        shader.setFloatUniform("u_speed", view.resolvedMotionSpeed())
        shader.setFloatUniform("u_morph", view.resolvedMotionAmp())
        shader.setFloatUniform("u_orbit", view.orbit.toFloat())
        shader.setFloatUniform("u_pattern", patternIndex(view.pattern, material))
        shader.setFloatUniform("u_patternScale", view.resolvedMotionDetail())
        shader.setFloatUniform("u_patternDistortion", view.resolvedMotionWarp())
        shader.setFloatUniform("u_tint", view.tint.toFloat())
        shader.setFloatUniform("u_opacity", view.opacity.toFloat())

        // Key light direction from azimuth/elevation (y-up; az 0 = front).
        val az = view.lightAzimuth
        val el = view.lightElevation
        shader.setFloatUniform(
            "u_lightDir",
            (sin(az) * cos(el)).toFloat(),
            sin(el).toFloat(),
            (cos(az) * cos(el)).toFloat()
        )

        shader.setFloatUniform("u_envRot", view.envRotation.toFloat())
        shader.setFloatUniform("u_hdrBoost", if (view.hdr) 1f else 0f)
        shader.setFloatUniform("u_density", view.density.toFloat())
        shader.setFloatUniform("u_smoothness", view.smoothness.toFloat())
        shader.setFloatUniform("u_silDensity", silhouetteDensity(material))
        shader.setFloatUniform("u_smoothSil", if (isSmoothSilhouette(material)) 1f else 0f)
        shader.setFloatUniform("u_motionSeed", view.motionSeed.toFloat())
    }

    private fun drawMaterialOrb(view: ShaderSurfaceView, canvas: Canvas, material: String, w: Float, h: Float) {
        val time = view.currentTimeSeconds()
        buildMaterialOrbPath(view, material, w, h, time)
        drawMaterialOrbShadow(view, canvas, material, w, h, time)
        canvas.drawPath(materialOrbPath, materialOrbPaint)
    }

    private fun buildMaterialOrbPath(view: ShaderSurfaceView, material: String, w: Float, h: Float, time: Float) {
        val size = min(w, h)
        val radius = size * 0.405f
        val cx = w * 0.5f
        val cy = h * 0.5f
        val morphAmount = view.resolvedMotionAmp().coerceIn(0f, 1.4f)
        val warpAmount = view.resolvedMotionWarp().coerceIn(0f, 1.4f)
        val speedAmount = view.resolvedMotionSpeed().coerceAtLeast(0.05f)
        val density = silhouetteDensity(material)
        // Reference (floating bubble video): broad lobes whose amplitude breathes,
        // plus localized dents that travel along the rim. Deformation is biased INWARD
        // so the silhouette never leaves the AGSL sphere domain (|p| <= 1).
        val lobeAmp = (0.048f + 0.062f * morphAmount + 0.026f * warpAmount) * radius * (1.10f - density * 0.35f)
        val dentDepth = (0.046f + 0.068f * morphAmount) * radius * (1.05f - density * 0.30f)
        val phase = time * speedAmount
        val points = 128

        // Traveling dents: angle drifts around the rim, depth breathes in and out.
        val dentAngle0 = phase * 0.23f
        val dentAngle1 = -phase * 0.17f + 2.4f
        val dentAngle2 = phase * 0.11f + 4.6f
        val dentPulse0 = (0.5f + 0.5f * sin(phase * 0.34f + 0.6f)).coerceAtLeast(0.12f)
        val dentPulse1 = (0.5f + 0.5f * sin(phase * 0.27f + 3.1f)).coerceAtLeast(0.12f)
        val dentPulse2 = (0.5f + 0.5f * sin(phase * 0.41f + 5.0f)).coerceAtLeast(0.12f)

        // Breathing amplitude per lobe harmonic + a slow global heartbeat breath,
        // mirrored 1:1 by the AGSL sphere radius (orb-main.agsl).
        val breathe0 = 0.45f + 0.55f * sin(phase * 0.26f + 1.1f)
        val breathe1 = 0.45f + 0.55f * sin(phase * 0.33f + 4.0f)
        val breathe2 = 0.45f + 0.55f * sin(phase * 0.21f + 2.3f)
        val globalBreath = orbBreath(phase)

        // Smooth wax-blob silhouette (water/glass) vs liquid-pebble (metal/iridescent).
        val smoothSilhouette = isSmoothSilhouette(material)
        materialOrbPath.reset()
        for (i in 0 until points) {
            val a = (i.toDouble() / points.toDouble() * PI * 2.0).toFloat()
            val bottom = sin(a)
            val r: Float
            if (smoothSilhouette) {
                var sw =
                    sin(a * 3.0f + phase * 0.16f) * 0.50f * breathe0 +
                    sin(a * 5.0f - phase * 0.11f + 1.0f) * 0.30f * breathe1 +
                    sin(a * 2.0f + phase * 0.08f + 2.4f) * 0.20f * breathe2
                // Slight inward bias so the profile stays inside the shader sphere.
                if (sw > 0f) sw *= 0.5f
                var rr = radius * globalBreath + lobeAmp * 0.50f * sw
                if (bottom > 0f) rr *= 1.0f + 0.020f * bottom * bottom
                r = rr
            } else {
                // Liquid-pebble profile: many short, shallow notches (harmonics 5/8/11).
                var wave =
                    sin(a * 5.0f + phase * 0.20f) * 0.42f * breathe0 +
                    sin(a * 8.0f - phase * 0.16f + 1.2f) * 0.34f * breathe1 +
                    sin(a * (11.0f + density) + phase * 0.12f + 2.1f) * 0.24f * breathe2
                // Bias inward: convex bulges are damped, concavities kept.
                if (wave > 0f) wave *= 0.30f
                var rr = radius * globalBreath + lobeAmp * 0.62f * wave
                // Localized traveling dents: narrow, like pressed-in creases.
                rr -= dentDepth * 0.72f * dentPulse0 * dentFalloff(a - dentAngle0, 0.30f)
                rr -= dentDepth * 0.72f * dentPulse1 * dentFalloff(a - dentAngle1, 0.24f)
                rr -= dentDepth * 0.55f * dentPulse2 * dentFalloff(a - dentAngle2, 0.38f)
                // Gravity sag: the lower profile bulges slightly like a hanging drop.
                if (bottom > 0f) rr *= 1.0f + 0.018f * bottom * bottom
                r = rr
            }
            val x = cx + cos(a) * r
            val y = cy + sin(a) * r
            if (i == 0) {
                materialOrbPath.moveTo(x, y)
            } else {
                materialOrbPath.lineTo(x, y)
            }
        }
        materialOrbPath.close()
    }

    // Heartbeat: slow inflate with a quicker relax (asymmetric second harmonic).
    // Keep in sync with the identical formula in orb-main.agsl.
    private fun orbBreath(phase: Float): Float {
        val pb = phase * 0.55f + 0.4f
        val beat = 0.65f * sin(pb) + 0.35f * sin(2.0f * pb + 1.2f)
        return 0.945f + 0.055f * beat
    }

    // Smooth wrapped notch centered on angle delta 0, with the given angular width.
    private fun dentFalloff(deltaAngle: Float, width: Float): Float {
        var d = deltaAngle
        val twoPi = (PI * 2.0).toFloat()
        while (d > PI) d -= twoPi
        while (d < -PI) d += twoPi
        val x = d / width
        val g = 1.0f - x * x
        return if (g <= 0f) 0f else g * g
    }

    private fun drawMaterialOrbShadow(view: ShaderSurfaceView, canvas: Canvas, material: String, w: Float, h: Float, time: Float) {
        val size = min(w, h)
        val radius = size * 0.405f
        val cy = h * 0.5f
        // Real cast shadow: the orb Path itself, squashed onto the ground and
        // blurred — every lobe and dent travels with the sphere. It breathes
        // with the orb volume: inflated body → closer to ground → darker.
        val speedAmount = view.resolvedMotionSpeed().coerceAtLeast(0.05f)
        val phase = time * speedAmount
        val breath01 = ((orbBreath(phase) - 0.890f) / 0.110f).coerceIn(0f, 1f)
        val cx = w * 0.5f
        val squashX = 0.55f
        val squashY = 0.16f
        val shadowCy = cy + radius * 1.18f
        // Key light comes from the top-left → shadow shifts slightly right.
        val shadowOffX = radius * 0.08f
        val baseStrength = shadowStrength(material)
        // Rasterize the silhouette, then run it down and back up the pyramid:
        // 96 -> 48 -> 24 -> 12 -> 24 -> 48 -> 96. Six filtered passes ≈ gaussian.
        val pad = radius * 1.35f
        shadowSrcBitmap.eraseColor(Color.TRANSPARENT)
        shadowMatrix.reset()
        shadowSrcRect.set(cx - pad, cy - pad, cx + pad, cy + pad)
        shadowDstRect.set(0f, 0f, SHADOW_SRC_SIZE.toFloat(), SHADOW_SRC_SIZE.toFloat())
        shadowMatrix.setRectToRect(shadowSrcRect, shadowDstRect, android.graphics.Matrix.ScaleToFit.FILL)
        materialOrbShadowPaint.color = Color.BLACK
        materialOrbShadowPaint.alpha = 255
        shadowSrcCanvas.save()
        shadowSrcCanvas.concat(shadowMatrix)
        shadowSrcCanvas.drawPath(materialOrbPath, materialOrbShadowPaint)
        shadowSrcCanvas.restore()
        var current: Bitmap = shadowSrcBitmap
        for ((bmp, cnv) in shadowPyramid) {
            bmp.eraseColor(Color.TRANSPARENT)
            shadowDstRect.set(0f, 0f, bmp.width.toFloat(), bmp.height.toFloat())
            cnv.drawBitmap(current, null, shadowDstRect, shadowBitmapPaint)
            current = bmp
        }
        for (i in shadowPyramid.size - 2 downTo 0) {
            val (bmp, cnv) = shadowPyramid[i]
            bmp.eraseColor(Color.TRANSPARENT)
            shadowDstRect.set(0f, 0f, bmp.width.toFloat(), bmp.height.toFloat())
            cnv.drawBitmap(current, null, shadowDstRect, shadowBitmapPaint)
            current = bmp
        }
        // The shadow follows the orb opacity too.
        val opacity = view.opacity.coerceIn(0.0, 1.0).toFloat()
        shadowBitmapPaint.alpha = (baseStrength * (0.60f + 0.50f * breath01) * opacity).toInt()
        shadowSrcRect.set(
            cx + shadowOffX - pad * squashX,
            shadowCy - pad * squashY,
            cx + shadowOffX + pad * squashX,
            shadowCy + pad * squashY
        )
        canvas.drawBitmap(current, null, shadowSrcRect, shadowBitmapPaint)
        shadowBitmapPaint.alpha = 255
    }
}

/**
 * "solid" — AGSL flat color surface (default material).
 * API < 33 fallback: flat fill with the parsed color.
 */
internal class SolidMaterial : ShaderMaterial {
    override val name: String = "solid"

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val fallbackPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var solidShaderInit = false
    private var solidRuntimeShader: RuntimeShader? = null

    private fun solidShader(view: ShaderSurfaceView): RuntimeShader? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return null
        }
        if (!solidShaderInit) {
            solidShaderInit = true
            val source = view.appContext.assets.open("shaders/shared-surface.agsl")
                .bufferedReader().use { it.readText() }
            solidRuntimeShader = RuntimeShader(source).also { paint.shader = it }
        }
        return solidRuntimeShader
    }

    override fun draw(canvas: Canvas, view: ShaderSurfaceView, w: Float, h: Float) {
        val shader = solidShader(view) ?: return drawFallback(canvas, view, w, h)
        shader.setFloatUniform("u_color", view.red(), view.green(), view.blue(), view.alpha())
        shader.setFloatUniform("u_time", view.currentTimeSeconds())
        // drawPaint fills the whole device clip when the parent disables
        // child clipping (RN default) — draw an explicit rect instead.
        canvas.drawRect(0f, 0f, w, h, paint)
    }

    override fun drawFallback(canvas: Canvas, view: ShaderSurfaceView, w: Float, h: Float) {
        fallbackPaint.color = view.parsedColor
        canvas.drawRect(0f, 0f, w, h, fallbackPaint)
    }
}
