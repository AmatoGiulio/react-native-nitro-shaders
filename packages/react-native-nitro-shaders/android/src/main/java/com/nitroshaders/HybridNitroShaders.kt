package com.margelo.nitro.nitroshaders

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RuntimeShader
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

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val fallbackPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var runtimeShader: RuntimeShader? = null
    private var parsedColor = Color.BLACK
    private var frameCallbackPosted = false
    private val startTimeNanos = System.nanoTime()

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            runtimeShader = RuntimeShader(SHADER_SOURCE)
            paint.shader = runtimeShader
        }
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val shader = runtimeShader
            if (shader != null) {
                shader.setFloatUniform("u_color", red(), green(), blue(), alpha())
                shader.setFloatUniform("u_time", currentTimeSeconds())
                // drawPaint fills the whole device clip when the parent disables
                // child clipping (RN default) — draw an explicit rect instead.
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
                return
            }
        }

        fallbackPaint.color = parsedColor
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), fallbackPaint)
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
