package com.margelo.nitro.nitroshaders

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import android.view.Choreographer
import android.view.View
import androidx.annotation.Keep
import com.facebook.proguard.annotations.DoNotStrip
import com.facebook.react.uimanager.ThemedReactContext

@Keep
@DoNotStrip
class HybridNitroShaders(val context: ThemedReactContext): HybridNitroShadersSpec() {
    override val view: View = ShaderSurfaceView(context)

    private val shaderSurfaceView: ShaderSurfaceView
        get() = view as ShaderSurfaceView

    private var _color = "#000000"
    override var color: String
        get() = _color
        set(value) {
            _color = value
            shaderSurfaceView.color = value
        }

    private var _animated = false
    override var animated: Boolean
        get() = _animated
        set(value) {
            _animated = value
            shaderSurfaceView.animated = value
        }

    private var _paused = false
    override var paused: Boolean
        get() = _paused
        set(value) {
            _paused = value
            shaderSurfaceView.paused = value
        }

    private var _debugTime = -1.0
    override var debugTime: Double
        get() = _debugTime
        set(value) {
            _debugTime = value
            shaderSurfaceView.debugTime = value
        }

    private var _shader = "solid"
    override var shader: String
        get() = _shader
        set(value) {
            _shader = value
            shaderSurfaceView.shader = value
        }

    private var _colors: Array<String> = arrayOf()
    override var colors: Array<String>
        get() = _colors
        set(value) {
            _colors = value
            shaderSurfaceView.colors = value
        }

    private var _speed = 1.0
    override var speed: Double
        get() = _speed
        set(value) {
            _speed = value
            shaderSurfaceView.speed = value
        }

    private var _intensity = 1.0
    override var intensity: Double
        get() = _intensity
        set(value) {
            _intensity = value
            shaderSurfaceView.intensity = value
        }

    private var _scale = 1.0
    override var scale: Double
        get() = _scale
        set(value) {
            _scale = value
            shaderSurfaceView.scale = value
        }

    private var _warp = 1.0
    override var warp: Double
        get() = _warp
        set(value) {
            _warp = value
            shaderSurfaceView.warp = value
        }

    private var _grain = 0.0
    override var grain: Double
        get() = _grain
        set(value) {
            _grain = value
            shaderSurfaceView.grain = value
        }

    private var _distortion = 1.2
    override var distortion: Double
        get() = _distortion
        set(value) {
            _distortion = value
            shaderSurfaceView.distortion = value
        }

    private var _shape = "circle"
    override var shape: String
        get() = _shape
        set(value) {
            _shape = value
            shaderSurfaceView.shape = value
        }

    private var _colorBack = "#00000000"
    override var colorBack: String
        get() = _colorBack
        set(value) {
            _colorBack = value
            shaderSurfaceView.colorBack = value
        }

    private var _colorTint = "#00000000"
    override var colorTint: String
        get() = _colorTint
        set(value) {
            _colorTint = value
            shaderSurfaceView.colorTint = value
        }

    private var _repetition = 4.0
    override var repetition: Double
        get() = _repetition
        set(value) {
            _repetition = value
            shaderSurfaceView.repetition = value
        }

    private var _softness = 0.5
    override var softness: Double
        get() = _softness
        set(value) {
            _softness = value
            shaderSurfaceView.softness = value
        }

    private var _shiftRed = 0.3
    override var shiftRed: Double
        get() = _shiftRed
        set(value) {
            _shiftRed = value
            shaderSurfaceView.shiftRed = value
        }

    private var _shiftBlue = 0.3
    override var shiftBlue: Double
        get() = _shiftBlue
        set(value) {
            _shiftBlue = value
            shaderSurfaceView.shiftBlue = value
        }

    private var _contour = 0.5
    override var contour: Double
        get() = _contour
        set(value) {
            _contour = value
            shaderSurfaceView.contour = value
        }

    private var _angle = 0.0
    override var angle: Double
        get() = _angle
        set(value) {
            _angle = value
            shaderSurfaceView.angle = value
        }

    private var _orbMaterial = 0.0
    override var orbMaterial: Double
        get() = _orbMaterial
        set(value) {
            _orbMaterial = value
            shaderSurfaceView.orbMaterial = value
        }

    private var _wobble = 1.0
    override var wobble: Double
        get() = _wobble
        set(value) {
            _wobble = value
            shaderSurfaceView.wobble = value
        }

    private var _detail = 1.0
    override var detail: Double
        get() = _detail
        set(value) {
            _detail = value
            shaderSurfaceView.detail = value
        }

    private var _materialColor = 0.5
    override var materialColor: Double
        get() = _materialColor
        set(value) {
            _materialColor = value
            shaderSurfaceView.materialColor = value
        }
}

private class ShaderSurfaceView(context: Context): View(context), Choreographer.FrameCallback {
    var color: String = "#000000"
        set(value) {
            field = value
            parsedColor = parseColor(value)
            invalidate()
        }

    var animated: Boolean = false
        set(value) {
            field = value
            updateFrameCallback()
        }

    var paused: Boolean = false
        set(value) {
            field = value
            updateFrameCallback()
        }

    var debugTime: Double = -1.0
        set(value) {
            field = value
            updateFrameCallback()
            invalidate()
        }

    var shader: String = "solid"
        set(value) {
            field = value
            invalidate()
        }

    var colors: Array<String> = arrayOf()
        set(value) {
            field = value
            parsedColors = parseColors(value)
            invalidate()
        }

    var speed: Double = 1.0
        set(value) {
            field = value
            invalidate()
        }

    var intensity: Double = 1.0
        set(value) {
            field = value
            invalidate()
        }

    var scale: Double = 1.0
        set(value) {
            field = value
            invalidate()
        }

    var warp: Double = 1.0
        set(value) {
            field = value
            invalidate()
        }

    var grain: Double = 0.0
        set(value) {
            field = value
            invalidate()
        }

    var distortion: Double = 1.2
        set(value) {
            field = value
            invalidate()
        }

    var shape: String = "circle"
        set(value) {
            field = value
            invalidate()
        }

    var colorBack: String = "#00000000"
        set(value) {
            field = value
            invalidate()
        }

    var colorTint: String = "#00000000"
        set(value) {
            field = value
            invalidate()
        }

    var repetition: Double = 4.0
        set(value) {
            field = value
            invalidate()
        }

    var softness: Double = 0.5
        set(value) {
            field = value
            invalidate()
        }

    var shiftRed: Double = 0.3
        set(value) {
            field = value
            invalidate()
        }

    var shiftBlue: Double = 0.3
        set(value) {
            field = value
            invalidate()
        }

    var contour: Double = 0.5
        set(value) {
            field = value
            invalidate()
        }

    var angle: Double = 0.0
        set(value) {
            field = value
            invalidate()
        }

    var orbMaterial: Double = 0.0
        set(value) {
            field = value
            invalidate()
        }

    var wobble: Double = 1.0
        set(value) {
            field = value
            invalidate()
        }

    var detail: Double = 1.0
        set(value) {
            field = value
            invalidate()
        }

    var materialColor: Double = 0.5
        set(value) {
            field = value
            invalidate()
        }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val fluidPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val fallbackPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val appContext = context.applicationContext
    private var parsedColor = Color.BLACK
    private var parsedColors: FloatArray = FloatArray(0)
    private var frameCallbackPosted = false
    private val startTimeNanos = System.nanoTime()

    private var fluidShaderInit = false
    private var fluidRuntimeShader: RuntimeShader? = null

    private var solidShaderInit = false
    private var solidRuntimeShader: RuntimeShader? = null

    private val liquidMetalPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var liquidMetalShaderInit = false
    private var liquidMetalRuntimeShader: RuntimeShader? = null

    private val materialOrbPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var materialOrbShaderInit = false
    private var materialOrbRuntimeShader: RuntimeShader? = null

    private fun materialOrbShader(): RuntimeShader? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return null
        }
        if (!materialOrbShaderInit) {
            materialOrbShaderInit = true
            val source = appContext.assets.open("shaders/material-orb.agsl")
                .bufferedReader().use { it.readText() }
            materialOrbRuntimeShader = RuntimeShader(source).also { materialOrbPaint.shader = it }
        }
        return materialOrbRuntimeShader
    }

    private fun liquidMetalShader(): RuntimeShader? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return null
        }
        if (!liquidMetalShaderInit) {
            liquidMetalShaderInit = true
            val source = appContext.assets.open("shaders/liquid-metal.agsl")
                .bufferedReader().use { it.readText() }
            liquidMetalRuntimeShader = RuntimeShader(source).also { liquidMetalPaint.shader = it }
        }
        return liquidMetalRuntimeShader
    }

    private fun fluidShader(): RuntimeShader? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return null
        }
        if (!fluidShaderInit) {
            fluidShaderInit = true
            val source = appContext.assets.open("shaders/fluid-gradient.agsl")
                .bufferedReader().use { it.readText() }
            fluidRuntimeShader = RuntimeShader(source).also { fluidPaint.shader = it }
        }
        return fluidRuntimeShader
    }

    private fun solidShader(): RuntimeShader? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return null
        }
        if (!solidShaderInit) {
            solidShaderInit = true
            val source = appContext.assets.open("shaders/shared-surface.agsl")
                .bufferedReader().use { it.readText() }
            solidRuntimeShader = RuntimeShader(source).also { paint.shader = it }
        }
        return solidRuntimeShader
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        updateFrameCallback()
    }

    override fun onDetachedFromWindow() {
        stopFrameCallback()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (shader == "materialOrb") {
                val materialOrb = materialOrbShader()
                if (materialOrb != null) {
                    setMaterialOrbUniforms(materialOrb, w, h)
                    canvas.drawRect(0f, 0f, w, h, materialOrbPaint)
                    return
                }
            } else if (shader == "liquidMetal") {
                val liquidMetal = liquidMetalShader()
                if (liquidMetal != null) {
                    setLiquidMetalUniforms(liquidMetal, w, h)
                    // drawPaint fills the whole device clip when the parent disables
                    // child clipping (RN default) — draw an explicit rect instead.
                    canvas.drawRect(0f, 0f, w, h, liquidMetalPaint)
                    return
                }
            } else if (shader == "fluidGradient") {
                val fluid = fluidShader()
                if (fluid != null) {
                    setFluidUniforms(fluid, w, h)
                    // drawPaint fills the whole device clip when the parent disables
                    // child clipping (RN default) — draw an explicit rect instead.
                    canvas.drawRect(0f, 0f, w, h, fluidPaint)
                    return
                }
            } else {
                val shader = solidShader()
                if (shader != null) {
                    shader.setFloatUniform("u_color", red(), green(), blue(), alpha())
                    shader.setFloatUniform("u_time", currentTimeSeconds())
                    // drawPaint fills the whole device clip when the parent disables
                    // child clipping (RN default) — draw an explicit rect instead.
                    canvas.drawRect(0f, 0f, w, h, paint)
                    return
                }
            }
        }

        if (shader == "materialOrb") {
            fallbackPaint.color = Color.TRANSPARENT
            canvas.drawRect(0f, 0f, w, h, fallbackPaint)
            return
        }

        if (shader == "liquidMetal") {
            // API < 33 fallback: flat fill with the background color.
            fallbackPaint.color = parseColor(colorBack)
            canvas.drawRect(0f, 0f, w, h, fallbackPaint)
            return
        }

        if (shader == "fluidGradient") {
            // API < 33 fallback: static vertical LinearGradient from the palette.
            fallbackPaint.shader = fluidFallbackGradient(w, h)
            canvas.drawRect(0f, 0f, w, h, fallbackPaint)
            fallbackPaint.shader = null
            return
        }

        fallbackPaint.color = parsedColor
        canvas.drawRect(0f, 0f, w, h, fallbackPaint)
    }

    private fun setMaterialOrbUniforms(materialOrb: RuntimeShader, w: Float, h: Float) {
        materialOrb.setFloatUniform("u_time", currentTimeSeconds())
        materialOrb.setFloatUniform("u_speed", speed.toFloat())
        materialOrb.setFloatUniform("u_resolution", w, h)
        materialOrb.setFloatUniform("u_orbMaterial", orbMaterial.toFloat())
        materialOrb.setFloatUniform("u_wobble", wobble.toFloat())
        materialOrb.setFloatUniform("u_distortion", distortion.toFloat())
        materialOrb.setFloatUniform("u_detail", detail.toFloat())
        materialOrb.setFloatUniform("u_materialColor", materialColor.toFloat())
    }

    private fun setLiquidMetalUniforms(liquidMetal: RuntimeShader, w: Float, h: Float) {
        liquidMetal.setFloatUniform("u_time", currentTimeSeconds())
        liquidMetal.setFloatUniform("u_speed", speed.toFloat())
        liquidMetal.setFloatUniform("u_resolution", w, h)

        val back = parseColor(colorBack)
        liquidMetal.setFloatUniform(
            "u_colorBack",
            Color.red(back) / 255f,
            Color.green(back) / 255f,
            Color.blue(back) / 255f,
            Color.alpha(back) / 255f
        )
        val tint = parseColor(colorTint)
        liquidMetal.setFloatUniform(
            "u_colorTint",
            Color.red(tint) / 255f,
            Color.green(tint) / 255f,
            Color.blue(tint) / 255f,
            Color.alpha(tint) / 255f
        )

        liquidMetal.setFloatUniform("u_repetition", repetition.toFloat())
        liquidMetal.setFloatUniform("u_softness", softness.toFloat())
        liquidMetal.setFloatUniform("u_shiftRed", shiftRed.toFloat())
        liquidMetal.setFloatUniform("u_shiftBlue", shiftBlue.toFloat())
        liquidMetal.setFloatUniform("u_distortion", distortion.toFloat())
        liquidMetal.setFloatUniform("u_contour", contour.toFloat())
        liquidMetal.setFloatUniform("u_angle", angle.toFloat())
        liquidMetal.setFloatUniform("u_shape", shapeToFloat(shape))
        liquidMetal.setFloatUniform("u_grain", grain.toFloat())
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

    private fun setFluidUniforms(fluid: RuntimeShader, w: Float, h: Float) {
        fluid.setFloatUniform("u_time", currentTimeSeconds())
        fluid.setFloatUniform("u_speed", speed.toFloat())
        fluid.setFloatUniform("u_scale", scale.toFloat())
        fluid.setFloatUniform("u_warp", warp.toFloat())
        fluid.setFloatUniform("u_intensity", intensity.toFloat())
        fluid.setFloatUniform("u_grain", grain.toFloat())
        fluid.setFloatUniform("u_resolution", w, h)

        val count = parsedColors.size / 3
        fluid.setFloatUniform("u_colorCount", count.coerceAtLeast(1).toFloat())
        // u_colors is a fixed vec3[6]; fill unused slots with black.
        val flat = FloatArray(18)
        System.arraycopy(parsedColors, 0, flat, 0, parsedColors.size.coerceAtMost(18))
        fluid.setFloatUniform("u_colors", flat)
    }

    private fun fluidFallbackGradient(w: Float, h: Float): LinearGradient {
        val count = parsedColors.size / 3
        val ints = if (count == 0) {
            intArrayOf(Color.BLACK, Color.BLACK)
        } else if (count == 1) {
            val c = colorFromParsed(0)
            intArrayOf(c, c)
        } else {
            IntArray(count) { colorFromParsed(it) }
        }
        return LinearGradient(0f, 0f, 0f, h, ints, null, Shader.TileMode.CLAMP)
    }

    private fun colorFromParsed(i: Int): Int {
        val o = i * 3
        return Color.rgb(
            (parsedColors[o] * 255f).toInt(),
            (parsedColors[o + 1] * 255f).toInt(),
            (parsedColors[o + 2] * 255f).toInt()
        )
    }

    override fun doFrame(frameTimeNanos: Long) {
        frameCallbackPosted = false
        invalidate()
        updateFrameCallback()
    }

    private fun updateFrameCallback() {
        if (animated && !paused && debugTime < 0 && isAttachedToWindow) {
            if (!frameCallbackPosted) {
                frameCallbackPosted = true
                Choreographer.getInstance().postFrameCallback(this)
            }
        } else {
            stopFrameCallback()
        }
    }

    private fun stopFrameCallback() {
        if (frameCallbackPosted) {
            Choreographer.getInstance().removeFrameCallback(this)
            frameCallbackPosted = false
        }
    }

    private fun currentTimeSeconds(): Float {
        if (debugTime >= 0) {
            return debugTime.toFloat()
        }

        return ((System.nanoTime() - startTimeNanos).toDouble() / 1_000_000_000.0).toFloat()
    }

    private fun parseColor(value: String): Int {
        return try {
            Color.parseColor(value)
        } catch (_: IllegalArgumentException) {
            Color.BLACK
        }
    }

    // Parse up to 6 hex colors into a flat [r,g,b, ...] float array (0..1). Extras ignored.
    private fun parseColors(values: Array<String>): FloatArray {
        val count = values.size.coerceAtMost(6)
        val out = FloatArray(count * 3)
        for (i in 0 until count) {
            val c = parseColor(values[i])
            out[i * 3] = Color.red(c) / 255f
            out[i * 3 + 1] = Color.green(c) / 255f
            out[i * 3 + 2] = Color.blue(c) / 255f
        }
        return out
    }

    private fun red(): Float = Color.red(parsedColor) / 255f
    private fun green(): Float = Color.green(parsedColor) / 255f
    private fun blue(): Float = Color.blue(parsedColor) / 255f
    private fun alpha(): Float = Color.alpha(parsedColor) / 255f

}
