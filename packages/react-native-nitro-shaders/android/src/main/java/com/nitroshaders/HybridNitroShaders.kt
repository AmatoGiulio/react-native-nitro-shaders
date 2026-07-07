package com.margelo.nitro.nitroshaders

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
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

    // ---- Surface selection + lifecycle ----

    private var _shader = "solid"
    override var shader: String
        get() = _shader
        set(value) {
            _shader = value
            shaderSurfaceView.shader = value
        }

    private var _color = "#000000"
    override var color: String
        get() = _color
        set(value) {
            _color = value
            shaderSurfaceView.color = value
        }

    private var _colors: Array<String> = arrayOf()
    override var colors: Array<String>
        get() = _colors
        set(value) {
            _colors = value
            shaderSurfaceView.colors = value
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

    // ---- Material orb: semantic parameter set ----

    private var _material = "metal"
    override var material: String
        get() = _material
        set(value) {
            _material = value
            shaderSurfaceView.material = value
        }

    private var _speed = 1.0
    override var speed: Double
        get() = _speed
        set(value) {
            _speed = value
            shaderSurfaceView.speed = value
        }

    private var _morph = 0.5
    override var morph: Double
        get() = _morph
        set(value) {
            _morph = value
            shaderSurfaceView.morph = value
        }

    private var _orbit = 1.0
    override var orbit: Double
        get() = _orbit
        set(value) {
            _orbit = value
            shaderSurfaceView.orbit = value
        }

    private var _pattern = "auto"
    override var pattern: String
        get() = _pattern
        set(value) {
            _pattern = value
            shaderSurfaceView.pattern = value
        }

    private var _patternScale = 1.0
    override var patternScale: Double
        get() = _patternScale
        set(value) {
            _patternScale = value
            shaderSurfaceView.patternScale = value
        }

    private var _patternDistortion = 0.5
    override var patternDistortion: Double
        get() = _patternDistortion
        set(value) {
            _patternDistortion = value
            shaderSurfaceView.patternDistortion = value
        }

    private var _tint = 0.5
    override var tint: Double
        get() = _tint
        set(value) {
            _tint = value
            shaderSurfaceView.tint = value
        }

    private var _opacity = 1.0
    override var opacity: Double
        get() = _opacity
        set(value) {
            _opacity = value
            shaderSurfaceView.opacity = value
        }

    private var _lightAzimuth = 0.0
    override var lightAzimuth: Double
        get() = _lightAzimuth
        set(value) {
            _lightAzimuth = value
            shaderSurfaceView.lightAzimuth = value
        }

    private var _lightElevation = Math.PI / 2
    override var lightElevation: Double
        get() = _lightElevation
        set(value) {
            _lightElevation = value
            shaderSurfaceView.lightElevation = value
        }

    private var _environment = -1.0
    override var environment: Double
        get() = _environment
        set(value) {
            _environment = value
            shaderSurfaceView.environment = value
        }

    private var _envRotation = 0.0
    override var envRotation: Double
        get() = _envRotation
        set(value) {
            _envRotation = value
            shaderSurfaceView.envRotation = value
        }

    private var _hdr = false
    override var hdr: Boolean
        get() = _hdr
        set(value) {
            _hdr = value
            shaderSurfaceView.hdr = value
        }

    private var _density = 1.0
    override var density: Double
        get() = _density
        set(value) {
            _density = value
            shaderSurfaceView.density = value
        }

    private var _smoothness = 0.0
    override var smoothness: Double
        get() = _smoothness
        set(value) {
            _smoothness = value
            shaderSurfaceView.smoothness = value
        }

    // ---- Motion uniform contract ----

    private var _motionType = 0.0
    override var motionType: Double
        get() = _motionType
        set(value) {
            _motionType = value
            shaderSurfaceView.motionType = value
        }

    private var _motionSpeed = 0.0
    override var motionSpeed: Double
        get() = _motionSpeed
        set(value) {
            _motionSpeed = value
            shaderSurfaceView.motionSpeed = value
        }

    private var _motionAmp = 0.0
    override var motionAmp: Double
        get() = _motionAmp
        set(value) {
            _motionAmp = value
            shaderSurfaceView.motionAmp = value
        }

    private var _motionWarp = 0.0
    override var motionWarp: Double
        get() = _motionWarp
        set(value) {
            _motionWarp = value
            shaderSurfaceView.motionWarp = value
        }

    private var _motionDetail = 0.0
    override var motionDetail: Double
        get() = _motionDetail
        set(value) {
            _motionDetail = value
            shaderSurfaceView.motionDetail = value
        }

    private var _motionSeed = 0.0
    override var motionSeed: Double
        get() = _motionSeed
        set(value) {
            _motionSeed = value
            shaderSurfaceView.motionSeed = value
        }

    private var _motionPeriod = 0.0
    override var motionPeriod: Double
        get() = _motionPeriod
        set(value) {
            _motionPeriod = value
            shaderSurfaceView.motionPeriod = value
        }
}

internal class ShaderSurfaceView(context: Context): View(context), Choreographer.FrameCallback {
    // ---- Surface ----
    var shader: String = "solid"
        set(value) {
            field = value
            invalidate()
        }

    var color: String = "#000000"
        set(value) {
            field = value
            parsedColor = parseColor(value)
            invalidate()
        }

    var colors: Array<String> = arrayOf()
        set(value) {
            field = value
            parsedColors = parseColors(value)
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

    // ---- Material orb semantic params ----
    var material: String = "metal"
        set(value) {
            field = value
            invalidate()
        }

    var speed: Double = 1.0
        set(value) {
            field = value
            invalidate()
        }

    var morph: Double = 0.5
        set(value) {
            field = value
            invalidate()
        }

    var orbit: Double = 1.0
        set(value) {
            field = value
            invalidate()
        }

    var pattern: String = "auto"
        set(value) {
            field = value
            invalidate()
        }

    var patternScale: Double = 1.0
        set(value) {
            field = value
            invalidate()
        }

    var patternDistortion: Double = 0.5
        set(value) {
            field = value
            invalidate()
        }

    var tint: Double = 0.5
        set(value) {
            field = value
            invalidate()
        }

    var opacity: Double = 1.0
        set(value) {
            field = value
            invalidate()
        }

    var lightAzimuth: Double = 0.0
        set(value) {
            field = value
            invalidate()
        }

    var lightElevation: Double = Math.PI / 2
        set(value) {
            field = value
            invalidate()
        }

    var environment: Double = -1.0
        set(value) {
            field = value
            invalidate()
        }

    var envRotation: Double = 0.0
        set(value) {
            field = value
            invalidate()
        }

    var hdr: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    var density: Double = 1.0
        set(value) {
            field = value
            invalidate()
        }

    var smoothness: Double = 0.0
        set(value) {
            field = value
            invalidate()
        }

    // ---- Motion ----
    var motionType: Double = 0.0
        set(value) {
            field = value
            invalidate()
        }

    var motionSpeed: Double = 0.0
        set(value) {
            field = value
            invalidate()
        }

    var motionAmp: Double = 0.0
        set(value) {
            field = value
            invalidate()
        }

    var motionWarp: Double = 0.0
        set(value) {
            field = value
            invalidate()
        }

    var motionDetail: Double = 0.0
        set(value) {
            field = value
            invalidate()
        }

    var motionSeed: Double = 0.0
        set(value) {
            field = value
            invalidate()
        }

    var motionPeriod: Double = 0.0
        set(value) {
            field = value
            invalidate()
        }

    internal val appContext = context.applicationContext
    internal var parsedColor = Color.BLACK
        private set
    internal var parsedColors: FloatArray = FloatArray(0)
        private set
    private var frameCallbackPosted = false
    private val startTimeNanos = System.nanoTime()

    // Registry of shader materials (Strategy pattern). onDraw looks up the active
    // material by [shader] name and delegates rendering to it; unknown names fall
    // back to the default "solid" material.
    private val solidMaterial: ShaderMaterial = SolidMaterial()
    private val materials: Map<String, ShaderMaterial> = listOf(
        MaterialOrbMaterial(),
        solidMaterial
    ).associateBy { it.name }

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

        val activeMaterial = materials[shader] ?: solidMaterial
        if (activeMaterial.isAvailable()) {
            activeMaterial.draw(canvas, this, w, h)
        } else {
            activeMaterial.drawFallback(canvas, this, w, h)
        }
    }

    // Motion params fall back to the semantic params when unset (0) — the motion
    // system can override any of them at runtime without touching the others.
    internal fun resolvedMotionSpeed(): Float {
        return if (motionSpeed > 0.0) motionSpeed.toFloat() else speed.toFloat()
    }

    internal fun resolvedMotionAmp(): Float {
        return if (motionAmp > 0.0) motionAmp.toFloat() else morph.toFloat()
    }

    internal fun resolvedMotionWarp(): Float {
        return if (motionWarp > 0.0) motionWarp.toFloat() else patternDistortion.toFloat()
    }

    internal fun resolvedMotionDetail(): Float {
        return if (motionDetail > 0.0) motionDetail.toFloat() else patternScale.toFloat()
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

    internal fun currentTimeSeconds(): Float {
        if (debugTime >= 0) {
            return debugTime.toFloat()
        }

        return ((System.nanoTime() - startTimeNanos).toDouble() / 1_000_000_000.0).toFloat()
    }

    internal fun parseColor(value: String): Int {
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

    internal fun red(): Float = Color.red(parsedColor) / 255f
    internal fun green(): Float = Color.green(parsedColor) / 255f
    internal fun blue(): Float = Color.blue(parsedColor) / 255f
    internal fun alpha(): Float = Color.alpha(parsedColor) / 255f

}
