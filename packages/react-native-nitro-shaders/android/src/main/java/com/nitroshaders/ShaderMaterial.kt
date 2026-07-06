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
 * "materialOrb" — organic Path + soft contact shadow + AGSL material shaded with a
 * studio HDRI environment BitmapShader.
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
    }
    private val fallbackPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var materialOrbShaderInit = false
    private var materialOrbRuntimeShader: RuntimeShader? = null

    // Studio environments (equirectangular) reflected/refracted by the materials.
    // metal/water/iridescent/aura share the dark studio; mercury reflects a bright
    // room so it reads real on the light app background.
    private var envInit = false
    private var envBitmap: Bitmap? = null
    private var envShader: BitmapShader? = null
    private var mercuryEnvBitmap: Bitmap? = null
    private var mercuryEnvShader: BitmapShader? = null
    // glass refracts a dark teal env (lab-2) — frozen from Giulio's on-device
    // validation 2026-07-06. BlendKit preview asset, DEV-ONLY license (replace
    // before publishing — see ASSET-LICENSES.md).
    private var glassEnvBitmap: Bitmap? = null
    private var glassEnvShader: BitmapShader? = null
    // water/gel refracts a sunset-sea env (lab-12, clouds + ocean) — frozen from
    // Giulio's on-device validation 2026-07-06. BlendKit asset, free license
    // (see ASSET-LICENSES.md).
    private var waterEnvBitmap: Bitmap? = null
    private var waterEnvShader: BitmapShader? = null
    // Runtime-switchable lab environments (env/lab-N.png), loaded lazily.
    private val labEnvs = HashMap<Int, Pair<Bitmap, BitmapShader>>()

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

    private fun ensureMaterialOrbEnv(view: ShaderSurfaceView) {
        if (envInit) {
            return
        }
        envInit = true
        val env = loadEnvBitmap(view, "env/studio.png")
        envBitmap = env
        envShader = BitmapShader(env, Shader.TileMode.REPEAT, Shader.TileMode.CLAMP)
        val mercuryEnv = loadEnvBitmap(view, "env/mercury-env.png")
        mercuryEnvBitmap = mercuryEnv
        mercuryEnvShader = BitmapShader(mercuryEnv, Shader.TileMode.REPEAT, Shader.TileMode.CLAMP)
        val glassEnv = loadEnvBitmap(view, "env/glass-env.png")
        glassEnvBitmap = glassEnv
        glassEnvShader = BitmapShader(glassEnv, Shader.TileMode.REPEAT, Shader.TileMode.CLAMP)
        val waterEnv = loadEnvBitmap(view, "env/water-env.png")
        waterEnvBitmap = waterEnv
        waterEnvShader = BitmapShader(waterEnv, Shader.TileMode.REPEAT, Shader.TileMode.CLAMP)
    }

    private fun labEnv(view: ShaderSurfaceView, index: Int): Pair<Bitmap, BitmapShader>? {
        labEnvs[index]?.let { return it }
        val bmp = try {
            view.appContext.assets.open("env/lab-$index.png").use { BitmapFactory.decodeStream(it) }
        } catch (e: Exception) {
            null
        } ?: return null
        val pair = bmp to BitmapShader(bmp, Shader.TileMode.REPEAT, Shader.TileMode.CLAMP)
        labEnvs[index] = pair
        return pair
    }

    private fun materialOrbShader(view: ShaderSurfaceView): RuntimeShader? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return null
        }
        if (!materialOrbShaderInit) {
            materialOrbShaderInit = true
            val source = view.appContext.assets.open("shaders/material-orb.agsl")
                .bufferedReader().use { it.readText() }
            materialOrbRuntimeShader = RuntimeShader(source).also { materialOrbPaint.shader = it }
        }
        return materialOrbRuntimeShader
    }

    override fun draw(canvas: Canvas, view: ShaderSurfaceView, w: Float, h: Float) {
        val materialOrb = materialOrbShader(view) ?: return drawFallback(canvas, view, w, h)
        setMaterialOrbUniforms(view, materialOrb, w, h)
        drawMaterialOrb(view, canvas, w, h)
    }

    override fun drawFallback(canvas: Canvas, view: ShaderSurfaceView, w: Float, h: Float) {
        fallbackPaint.color = Color.TRANSPARENT
        canvas.drawRect(0f, 0f, w, h, fallbackPaint)
    }

    private fun setMaterialOrbUniforms(view: ShaderSurfaceView, materialOrb: RuntimeShader, w: Float, h: Float) {
        ensureMaterialOrbEnv(view)
        // Runtime env switch (tuning lab): `repetition` >= 100 selects env/lab-N.png
        // (N = repetition - 100). Otherwise each material keeps its default env.
        val labIndex = view.repetition.toInt() - 100
        val lab = if (labIndex >= 0) labEnv(view, labIndex) else null
        val isWater = view.orbMaterial >= 0.5 && view.orbMaterial < 1.5
        val isMercury = view.orbMaterial >= 3.5 && view.orbMaterial < 4.5
        val isGlass = view.orbMaterial >= 4.5
        val defaultShader = if (isMercury) mercuryEnvShader else if (isGlass) glassEnvShader else if (isWater) waterEnvShader else envShader
        val defaultBitmap = if (isMercury) mercuryEnvBitmap else if (isGlass) glassEnvBitmap else if (isWater) waterEnvBitmap else envBitmap
        val boundShader = lab?.second ?: defaultShader
        val boundBitmap = lab?.first ?: defaultBitmap
        boundShader?.let { materialOrb.setInputShader("u_env", it) }
        boundBitmap?.let { materialOrb.setFloatUniform("u_envSize", it.width.toFloat(), it.height.toFloat()) }
        materialOrb.setFloatUniform("u_time", view.currentTimeSeconds())
        materialOrb.setFloatUniform("u_speed", view.speed.toFloat())
        materialOrb.setFloatUniform("u_resolution", w, h)
        materialOrb.setFloatUniform("u_orbMaterial", view.orbMaterial.toFloat())
        materialOrb.setFloatUniform("u_wobble", view.wobble.toFloat())
        materialOrb.setFloatUniform("u_distortion", view.distortion.toFloat())
        materialOrb.setFloatUniform("u_detail", view.detail.toFloat())
        materialOrb.setFloatUniform("u_materialColor", view.materialColor.toFloat())
        materialOrb.setFloatUniform("u_motionType", view.resolvedMotionType())
        materialOrb.setFloatUniform("u_motionSpeed", view.resolvedMotionSpeed())
        materialOrb.setFloatUniform("u_motionAmp", view.resolvedMotionAmp())
        materialOrb.setFloatUniform("u_motionWarp", view.resolvedMotionWarp())
        materialOrb.setFloatUniform("u_motionDetail", view.resolvedMotionDetail())
        materialOrb.setFloatUniform("u_motionSeed", view.motionSeed.toFloat())
        materialOrb.setFloatUniform("u_motionPeriod", view.motionPeriod.toFloat())
        // Pseudo-HDR toggle rides the legacy `grain` slot (0 = off, 1 = on).
        materialOrb.setFloatUniform("u_hdrBoost", view.grain.toFloat())
        // water live-tuning: intensity → body density, softness → smooth-patch amount
        // (legacy liquidMetal slots reused; declared debt).
        materialOrb.setFloatUniform("u_intensity", view.intensity.toFloat())
        materialOrb.setFloatUniform("u_softness", view.softness.toFloat())
        // env horizontal rotation (move the HDRI) rides the legacy `angle` slot.
        materialOrb.setFloatUniform("u_envRot", view.angle.toFloat())
    }

    private fun drawMaterialOrb(view: ShaderSurfaceView, canvas: Canvas, w: Float, h: Float) {
        val time = view.currentTimeSeconds()
        buildMaterialOrbPath(view, w, h, time)
        drawMaterialOrbShadow(view, canvas, w, h, time)
        canvas.drawPath(materialOrbPath, materialOrbPaint)
    }

    private fun buildMaterialOrbPath(view: ShaderSurfaceView, w: Float, h: Float, time: Float) {
        val size = min(w, h)
        val radius = size * 0.405f
        val cx = w * 0.5f
        val cy = h * 0.5f
        val wobbleAmount = view.resolvedMotionAmp().coerceIn(0f, 1.4f)
        val warpAmount = view.resolvedMotionWarp().coerceIn(0f, 1.4f)
        val speedAmount = view.resolvedMotionSpeed().coerceAtLeast(0.05f)
        val density = materialOrbDensity(view)
        // Reference (floating bubble video): 5-7 broad lobes whose amplitude breathes,
        // plus localized dents that travel along the rim. Deformation is biased INWARD
        // so the silhouette never leaves the AGSL sphere domain (|p| <= 1).
        val lobeAmp = (0.048f + 0.062f * wobbleAmount + 0.026f * warpAmount) * radius * (1.10f - density * 0.35f)
        val dentDepth = (0.046f + 0.068f * wobbleAmount) * radius * (1.05f - density * 0.30f)
        val phase = time * speedAmount
        val points = 128

        // Traveling dents: angle drifts around the rim, depth breathes in and out.
        val dentAngle0 = phase * 0.23f
        val dentAngle1 = -phase * 0.17f + 2.4f
        val dentAngle2 = phase * 0.11f + 4.6f
        val dentPulse0 = (0.5f + 0.5f * sin(phase * 0.34f + 0.6f)).coerceAtLeast(0.12f)
        val dentPulse1 = (0.5f + 0.5f * sin(phase * 0.27f + 3.1f)).coerceAtLeast(0.12f)
        val dentPulse2 = (0.5f + 0.5f * sin(phase * 0.41f + 5.0f)).coerceAtLeast(0.12f)

        // Breathing amplitude per lobe harmonic: wide swing so the whole body
        // visibly inflates/relaxes over time, plus a slow global breath.
        val breathe0 = 0.45f + 0.55f * sin(phase * 0.26f + 1.1f)
        val breathe1 = 0.45f + 0.55f * sin(phase * 0.33f + 4.0f)
        val breathe2 = 0.45f + 0.55f * sin(phase * 0.21f + 2.3f)
        // Heartbeat breath, mirrored 1:1 by the AGSL sphere radius so the whole
        // body (shading included) inflates and relaxes together.
        val globalBreath = orbBreath(phase)

        // Smooth wax-blob silhouette (aura/symbiote mode 3 + mercury mode 4 + glass
        // mode 5 + water mode 1). Rounder, moved by slow low harmonic swells instead
        // of the liquid-pebble notches + traveling dents, so it reads like the Blender
        // dispersion-glass / gel / symbiote sphere (wave displacement, no pressed-in
        // creases).
        val smoothSilhouette = view.orbMaterial >= 2.5 || (view.orbMaterial >= 0.5 && view.orbMaterial < 1.5)
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
                // Liquid-pebble profile (J reference): many short, shallow notches
                // (higher harmonics 5/8/11) instead of broad balloon lobes.
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
                // Gravity sag: the lower profile bulges slightly like a hanging drop
                // (screen Y grows downward, so sin(a) > 0 is the bottom half).
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
    // Keep in sync with the identical formula in material-orb.agsl.
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

    private fun materialOrbDensity(view: ShaderSurfaceView): Float {
        return when {
            view.orbMaterial < 0.5 -> 0.92f
            view.orbMaterial < 1.5 -> 0.45f
            view.orbMaterial < 3.5 -> 0.62f
            view.orbMaterial < 4.5 -> 0.90f
            else -> 0.45f
        }
    }

    private fun drawMaterialOrbShadow(view: ShaderSurfaceView, canvas: Canvas, w: Float, h: Float, time: Float) {
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
        val baseStrength = when {
            view.orbMaterial < 0.5 -> 30
            view.orbMaterial < 1.5 -> 20
            view.orbMaterial < 3.5 -> 24
            else -> 28
        }
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
        shadowBitmapPaint.alpha = (baseStrength * (0.60f + 0.50f * breath01)).toInt()
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
