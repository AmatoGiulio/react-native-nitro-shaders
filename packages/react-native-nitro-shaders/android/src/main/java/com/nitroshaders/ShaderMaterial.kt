package com.margelo.nitro.nitroshaders

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
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
    private val fallbackPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var materialOrbShaderInit = false
    private var materialOrbRuntimeShader: RuntimeShader? = null

    // Studio environment (equirectangular HDRI) reflected/refracted by the material.
    private var envInit = false
    private var envBitmap: Bitmap? = null
    private var envShader: BitmapShader? = null

    private fun ensureMaterialOrbEnv(view: ShaderSurfaceView) {
        if (envInit) {
            return
        }
        envInit = true
        val bmp = try {
            view.appContext.assets.open("env/studio.png").use { BitmapFactory.decodeStream(it) }
        } catch (e: Exception) {
            null
        }
        val safe = bmp ?: Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888).also {
            it.eraseColor(Color.rgb(128, 130, 134))
        }
        envBitmap = safe
        envShader = BitmapShader(safe, Shader.TileMode.REPEAT, Shader.TileMode.CLAMP)
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
        envShader?.let { materialOrb.setInputShader("u_env", it) }
        envBitmap?.let { materialOrb.setFloatUniform("u_envSize", it.width.toFloat(), it.height.toFloat()) }
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
    }

    private fun drawMaterialOrb(view: ShaderSurfaceView, canvas: Canvas, w: Float, h: Float) {
        val time = view.currentTimeSeconds()
        buildMaterialOrbPath(view, w, h, time)
        drawMaterialOrbShadow(view, canvas, w, h)
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
        val edgeAmp = (0.010f + 0.020f * wobbleAmount + 0.016f * warpAmount) * radius * (1.12f - density * 0.42f)
        val phase = time * speedAmount
        val points = 80

        materialOrbPath.reset()
        for (i in 0 until points) {
            val a = (i.toDouble() / points.toDouble() * PI * 2.0).toFloat()
            val wave =
                sin(a * (2.0f + density) + phase * (0.70f - density * 0.24f)) * 0.44f +
                sin(a * (3.0f + density * 1.7f) - phase * (0.58f - density * 0.18f) + 1.2f) * 0.34f +
                sin(a * (5.0f + density * 2.0f) + phase * (0.30f + density * 0.08f) + 2.1f) * 0.22f
            val r = radius + edgeAmp * wave
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

    private fun materialOrbDensity(view: ShaderSurfaceView): Float {
        return when {
            view.orbMaterial < 0.5 -> 0.92f
            view.orbMaterial < 1.5 -> 0.45f
            else -> 0.62f
        }
    }

    private fun drawMaterialOrbShadow(view: ShaderSurfaceView, canvas: Canvas, w: Float, h: Float) {
        val size = min(w, h)
        val radius = size * 0.405f
        val cx = w * 0.5f
        val cy = h * 0.5f
        val shadowCx = cx
        val shadowCy = cy + radius * 0.84f
        val shadowRx = radius * 0.74f
        val shadowRy = radius * 0.13f
        val strength = when {
            view.orbMaterial < 0.5 -> 54
            view.orbMaterial < 1.5 -> 36
            else -> 42
        }

        materialOrbShadowPaint.shader = RadialGradient(
            shadowCx,
            shadowCy,
            shadowRx,
            intArrayOf(Color.argb(strength, 0, 0, 0), Color.argb(0, 0, 0, 0)),
            floatArrayOf(0.0f, 1.0f),
            Shader.TileMode.CLAMP
        )
        canvas.save()
        canvas.scale(1.0f, shadowRy / shadowRx, shadowCx, shadowCy)
        canvas.drawCircle(shadowCx, shadowCy, shadowRx, materialOrbShadowPaint)
        canvas.restore()
        materialOrbShadowPaint.shader = null
    }
}

/**
 * "liquidMetal" — AGSL chromatic metal shaded across the full rect.
 * API < 33 fallback: flat fill with the background color.
 */
internal class LiquidMetalMaterial : ShaderMaterial {
    override val name: String = "liquidMetal"

    private val liquidMetalPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val fallbackPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var liquidMetalShaderInit = false
    private var liquidMetalRuntimeShader: RuntimeShader? = null

    private fun liquidMetalShader(view: ShaderSurfaceView): RuntimeShader? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return null
        }
        if (!liquidMetalShaderInit) {
            liquidMetalShaderInit = true
            val source = view.appContext.assets.open("shaders/liquid-metal.agsl")
                .bufferedReader().use { it.readText() }
            liquidMetalRuntimeShader = RuntimeShader(source).also { liquidMetalPaint.shader = it }
        }
        return liquidMetalRuntimeShader
    }

    override fun draw(canvas: Canvas, view: ShaderSurfaceView, w: Float, h: Float) {
        val liquidMetal = liquidMetalShader(view) ?: return drawFallback(canvas, view, w, h)
        setLiquidMetalUniforms(view, liquidMetal, w, h)
        // drawPaint fills the whole device clip when the parent disables
        // child clipping (RN default) — draw an explicit rect instead.
        canvas.drawRect(0f, 0f, w, h, liquidMetalPaint)
    }

    override fun drawFallback(canvas: Canvas, view: ShaderSurfaceView, w: Float, h: Float) {
        // API < 33 fallback: flat fill with the background color.
        fallbackPaint.color = view.parseColor(view.colorBack)
        canvas.drawRect(0f, 0f, w, h, fallbackPaint)
    }

    private fun setLiquidMetalUniforms(view: ShaderSurfaceView, liquidMetal: RuntimeShader, w: Float, h: Float) {
        liquidMetal.setFloatUniform("u_time", view.currentTimeSeconds())
        liquidMetal.setFloatUniform("u_speed", view.speed.toFloat())
        liquidMetal.setFloatUniform("u_resolution", w, h)

        val back = view.parseColor(view.colorBack)
        liquidMetal.setFloatUniform(
            "u_colorBack",
            Color.red(back) / 255f,
            Color.green(back) / 255f,
            Color.blue(back) / 255f,
            Color.alpha(back) / 255f
        )
        val tint = view.parseColor(view.colorTint)
        liquidMetal.setFloatUniform(
            "u_colorTint",
            Color.red(tint) / 255f,
            Color.green(tint) / 255f,
            Color.blue(tint) / 255f,
            Color.alpha(tint) / 255f
        )

        liquidMetal.setFloatUniform("u_repetition", view.repetition.toFloat())
        liquidMetal.setFloatUniform("u_softness", view.softness.toFloat())
        liquidMetal.setFloatUniform("u_shiftRed", view.shiftRed.toFloat())
        liquidMetal.setFloatUniform("u_shiftBlue", view.shiftBlue.toFloat())
        liquidMetal.setFloatUniform("u_distortion", view.distortion.toFloat())
        liquidMetal.setFloatUniform("u_contour", view.contour.toFloat())
        liquidMetal.setFloatUniform("u_angle", view.angle.toFloat())
        liquidMetal.setFloatUniform("u_shape", shapeToFloat(view.shape))
        liquidMetal.setFloatUniform("u_grain", view.grain.toFloat())
    }

    private fun shapeToFloat(shape: String): Float {
        return when (shape) {
            "none" -> 0.0f
            "circle" -> 1.0f
            "daisy" -> 2.0f
            "diamond" -> 3.0f
            "metaballs" -> 4.0f
            else -> 1.0f
        }
    }
}

/**
 * "fluidGradient" — AGSL fluid palette gradient across the full rect.
 * API < 33 fallback: static vertical LinearGradient from the palette.
 */
internal class FluidGradientMaterial : ShaderMaterial {
    override val name: String = "fluidGradient"

    private val fluidPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val fallbackPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var fluidShaderInit = false
    private var fluidRuntimeShader: RuntimeShader? = null

    private fun fluidShader(view: ShaderSurfaceView): RuntimeShader? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return null
        }
        if (!fluidShaderInit) {
            fluidShaderInit = true
            val source = view.appContext.assets.open("shaders/fluid-gradient.agsl")
                .bufferedReader().use { it.readText() }
            fluidRuntimeShader = RuntimeShader(source).also { fluidPaint.shader = it }
        }
        return fluidRuntimeShader
    }

    override fun draw(canvas: Canvas, view: ShaderSurfaceView, w: Float, h: Float) {
        val fluid = fluidShader(view) ?: return drawFallback(canvas, view, w, h)
        setFluidUniforms(view, fluid, w, h)
        // drawPaint fills the whole device clip when the parent disables
        // child clipping (RN default) — draw an explicit rect instead.
        canvas.drawRect(0f, 0f, w, h, fluidPaint)
    }

    override fun drawFallback(canvas: Canvas, view: ShaderSurfaceView, w: Float, h: Float) {
        // API < 33 fallback: static vertical LinearGradient from the palette.
        fallbackPaint.shader = fluidFallbackGradient(view, w, h)
        canvas.drawRect(0f, 0f, w, h, fallbackPaint)
        fallbackPaint.shader = null
    }

    private fun setFluidUniforms(view: ShaderSurfaceView, fluid: RuntimeShader, w: Float, h: Float) {
        fluid.setFloatUniform("u_time", view.currentTimeSeconds())
        fluid.setFloatUniform("u_speed", view.speed.toFloat())
        fluid.setFloatUniform("u_scale", view.scale.toFloat())
        fluid.setFloatUniform("u_warp", view.warp.toFloat())
        fluid.setFloatUniform("u_intensity", view.intensity.toFloat())
        fluid.setFloatUniform("u_grain", view.grain.toFloat())
        fluid.setFloatUniform("u_resolution", w, h)

        val count = view.parsedColors.size / 3
        fluid.setFloatUniform("u_colorCount", count.coerceAtLeast(1).toFloat())
        // u_colors is a fixed vec3[6]; fill unused slots with black.
        val flat = FloatArray(18)
        System.arraycopy(view.parsedColors, 0, flat, 0, view.parsedColors.size.coerceAtMost(18))
        fluid.setFloatUniform("u_colors", flat)
    }

    private fun fluidFallbackGradient(view: ShaderSurfaceView, w: Float, h: Float): LinearGradient {
        val count = view.parsedColors.size / 3
        val ints = if (count == 0) {
            intArrayOf(Color.BLACK, Color.BLACK)
        } else if (count == 1) {
            val c = colorFromParsed(view, 0)
            intArrayOf(c, c)
        } else {
            IntArray(count) { colorFromParsed(view, it) }
        }
        return LinearGradient(0f, 0f, 0f, h, ints, null, Shader.TileMode.CLAMP)
    }

    private fun colorFromParsed(view: ShaderSurfaceView, i: Int): Int {
        val o = i * 3
        return Color.rgb(
            (view.parsedColors[o] * 255f).toInt(),
            (view.parsedColors[o + 1] * 255f).toInt(),
            (view.parsedColors[o + 2] * 255f).toInt()
        )
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
