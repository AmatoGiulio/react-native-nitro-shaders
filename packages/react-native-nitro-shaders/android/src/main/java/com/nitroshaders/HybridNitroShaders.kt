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

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val fluidPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val fallbackPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val appContext = context.applicationContext
    private var runtimeShader: RuntimeShader? = null
    private var parsedColor = Color.BLACK
    private var parsedColors: FloatArray = FloatArray(0)
    private var frameCallbackPosted = false
    private val startTimeNanos = System.nanoTime()

    private var fluidShaderInit = false
    private var fluidRuntimeShader: RuntimeShader? = null

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            runtimeShader = RuntimeShader(SHADER_SOURCE)
            paint.shader = runtimeShader
        }
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
            if (shader == "fluidGradient") {
                val fluid = fluidShader()
                if (fluid != null) {
                    setFluidUniforms(fluid, w, h)
                    // drawPaint fills the whole device clip when the parent disables
                    // child clipping (RN default) — draw an explicit rect instead.
                    canvas.drawRect(0f, 0f, w, h, fluidPaint)
                    return
                }
            } else {
                val shader = runtimeShader
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

    companion object {
        private const val SHADER_SOURCE = """
            uniform vec4 u_color;
            uniform float u_time;

            half4 main(float2 coord) {
              return half4(u_color);
            }
        """
    }
}
