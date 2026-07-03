//
//  HybridNitroShaders.swift
//  Pods
//
//  Created by Giulio Amato on 03/07/2026.
//

import Foundation
import Metal
import MetalKit
import UIKit

class HybridNitroShaders : HybridNitroShadersSpec, MTKViewDelegate {
  private struct Uniforms {
    var color: SIMD4<Float>
    var time: Float
  }

  // Layout MUST match FluidUniforms in fluid-gradient.metal (float4 per color).
  private struct FluidUniforms {
    var time: Float = 0
    var speed: Float = 1
    var scale: Float = 1
    var warp: Float = 1
    var intensity: Float = 1
    var grain: Float = 0
    var resolution: SIMD2<Float> = SIMD2<Float>(1, 1)
    var colorCount: Float = 1
    var colors: (SIMD4<Float>, SIMD4<Float>, SIMD4<Float>,
                 SIMD4<Float>, SIMD4<Float>, SIMD4<Float>) =
      (SIMD4<Float>(0, 0, 0, 1), SIMD4<Float>(0, 0, 0, 1), SIMD4<Float>(0, 0, 0, 1),
       SIMD4<Float>(0, 0, 0, 1), SIMD4<Float>(0, 0, 0, 1), SIMD4<Float>(0, 0, 0, 1))
  }

  // Layout MUST match LiquidChromeUniforms in liquid-chrome.metal (float4 per color).
  private struct LiquidChromeUniforms {
    var resolution: SIMD2<Float> = SIMD2<Float>(1, 1)
    var time: Float = 0
    var speed: Float = 1
    var scale: Float = 1
    var flow: Float = 0.35
    var distortion: Float = 1.2
    var contrast: Float = 1.4
    var highlightPower: Float = 6.15
    var highlightIntensity: Float = 1.1
    var grain: Float = 0
    var iridescence: Float = 0
    var baseDark: SIMD4<Float> = SIMD4<Float>(0, 0, 0, 1)
    var baseLight: SIMD4<Float> = SIMD4<Float>(1, 1, 1, 1)
    var highlightColor: SIMD4<Float> = SIMD4<Float>(1, 1, 1, 1)
    var edgeTint: SIMD4<Float> = SIMD4<Float>(1, 1, 1, 1)
  }

  // CPU-side resolution of a liquidChrome variant. Table MUST stay identical to
  // the Android/AGSL counterpart for visual parity.
  private struct LiquidChromeVariant {
    var baseDark: SIMD4<Float>
    var baseLight: SIMD4<Float>
    var highlight: SIMD4<Float>
    var edgeTint: SIMD4<Float>
    var iridescence: Float
    var highlightPowerFactor: Float
  }

  private static func liquidChromeVariant(_ name: String) -> LiquidChromeVariant {
    switch name {
    case "mercury":
      return LiquidChromeVariant(
        baseDark: SIMD4<Float>(0.02, 0.03, 0.05, 1),
        baseLight: SIMD4<Float>(0.85, 0.90, 0.98, 1),
        highlight: SIMD4<Float>(0.98, 0.99, 1.0, 1),
        edgeTint: SIMD4<Float>(0.65, 0.75, 0.95, 1),
        iridescence: 0.0, highlightPowerFactor: 1.0)
    case "blackChrome":
      return LiquidChromeVariant(
        baseDark: SIMD4<Float>(0.01, 0.01, 0.012, 1),
        baseLight: SIMD4<Float>(0.35, 0.36, 0.38, 1),
        highlight: SIMD4<Float>(0.85, 0.87, 0.90, 1),
        edgeTint: SIMD4<Float>(0.25, 0.26, 0.30, 1),
        iridescence: 0.0, highlightPowerFactor: 2.0)
    case "rainbowOil":
      return LiquidChromeVariant(
        baseDark: SIMD4<Float>(0.05, 0.05, 0.06, 1),
        baseLight: SIMD4<Float>(0.90, 0.90, 0.94, 1),
        highlight: SIMD4<Float>(1.0, 1.0, 1.0, 1),
        edgeTint: SIMD4<Float>(0.80, 0.84, 0.90, 1),
        iridescence: 0.12, highlightPowerFactor: 1.0)
    default: // silver (also unknown fallback)
      return LiquidChromeVariant(
        baseDark: SIMD4<Float>(0.05, 0.05, 0.06, 1),
        baseLight: SIMD4<Float>(0.95, 0.95, 0.97, 1),
        highlight: SIMD4<Float>(1.0, 1.0, 1.0, 1),
        edgeTint: SIMD4<Float>(0.85, 0.88, 0.92, 1),
        iridescence: 0.0, highlightPowerFactor: 1.0)
    }
  }

  private let metalView: MTKView
  private let device: MTLDevice?
  private let commandQueue: MTLCommandQueue?
  private var library: MTLLibrary?
  private var pipelineCache: [String: MTLRenderPipelineState] = [:]
  private var startTime = CACurrentMediaTime()

  // Parsed palette cache — reparsed only when `colors` changes.
  private var paletteColors: [SIMD4<Float>] = []
  private var paletteCount: Float = 1

  var view: UIView {
    metalView
  }

  var color: String = "#000000" {
    didSet {
      drawIfNeeded()
    }
  }

  var animated: Bool = false {
    didSet {
      metalView.enableSetNeedsDisplay = !shouldAnimate
      metalView.isPaused = !shouldAnimate
      drawIfNeeded()
    }
  }

  var paused: Bool = false {
    didSet {
      metalView.enableSetNeedsDisplay = !shouldAnimate
      metalView.isPaused = !shouldAnimate
      drawIfNeeded()
    }
  }

  var debugTime: Double = -1 {
    didSet {
      drawIfNeeded()
    }
  }

  var shader: String = "solid" {
    didSet {
      drawIfNeeded()
    }
  }

  var colors: [String] = [] {
    didSet {
      updatePalette()
      drawIfNeeded()
    }
  }

  var speed: Double = 1 {
    didSet {
      drawIfNeeded()
    }
  }

  var intensity: Double = 1 {
    didSet {
      drawIfNeeded()
    }
  }

  var scale: Double = 1 {
    didSet {
      drawIfNeeded()
    }
  }

  var warp: Double = 1 {
    didSet {
      drawIfNeeded()
    }
  }

  var grain: Double = 0 {
    didSet {
      drawIfNeeded()
    }
  }

  var variant: String = "silver" {
    didSet {
      drawIfNeeded()
    }
  }

  var flow: Double = 0.35 {
    didSet {
      drawIfNeeded()
    }
  }

  var distortion: Double = 1.2 {
    didSet {
      drawIfNeeded()
    }
  }

  var contrast: Double = 1.4 {
    didSet {
      drawIfNeeded()
    }
  }

  var highlightWidth: Double = 0.65 {
    didSet {
      drawIfNeeded()
    }
  }

  var highlightIntensity: Double = 1.1 {
    didSet {
      drawIfNeeded()
    }
  }

  override init() {
    let metalDevice = MTLCreateSystemDefaultDevice()
    device = metalDevice
    commandQueue = metalDevice?.makeCommandQueue()
    metalView = MTKView(frame: .zero, device: metalDevice)
    super.init()

    metalView.clearColor = MTLClearColor(red: 0, green: 0, blue: 0, alpha: 1)
    metalView.framebufferOnly = true
    metalView.isPaused = true
    metalView.enableSetNeedsDisplay = true
    metalView.delegate = self
    library = try? metalDevice?.makeLibrary(source: Self.shaderSource, options: nil)
    updatePalette()
  }

  func mtkView(_ view: MTKView, drawableSizeWillChange size: CGSize) {
    drawIfNeeded()
  }

  func draw(in view: MTKView) {
    guard
      let commandQueue,
      let drawable = view.currentDrawable,
      let descriptor = view.currentRenderPassDescriptor
    else {
      return
    }

    let time = Float(debugTime >= 0 ? debugTime : CACurrentMediaTime() - startTime)

    let commandBuffer = commandQueue.makeCommandBuffer()
    let encoder = commandBuffer?.makeRenderCommandEncoder(descriptor: descriptor)

    if shader == "fluidGradient", let pipelineState = pipelineState(for: "fluidGradient") {
      var uniforms = FluidUniforms()
      uniforms.time = time
      uniforms.speed = Float(speed)
      uniforms.scale = Float(scale)
      uniforms.warp = Float(warp)
      uniforms.intensity = Float(intensity)
      uniforms.grain = Float(grain)
      uniforms.resolution = SIMD2<Float>(
        Float(max(view.drawableSize.width, 1)),
        Float(max(view.drawableSize.height, 1))
      )
      uniforms.colorCount = paletteCount
      uniforms.colors = (
        paletteColor(0), paletteColor(1), paletteColor(2),
        paletteColor(3), paletteColor(4), paletteColor(5)
      )
      encoder?.setRenderPipelineState(pipelineState)
      encoder?.setFragmentBytes(&uniforms, length: MemoryLayout<FluidUniforms>.stride, index: 0)
      encoder?.drawPrimitives(type: .triangle, vertexStart: 0, vertexCount: 3)
    } else if shader == "liquidChrome", let pipelineState = pipelineState(for: "liquidChrome") {
      let v = Self.liquidChromeVariant(variant)
      var uniforms = LiquidChromeUniforms()
      uniforms.resolution = SIMD2<Float>(
        Float(max(view.drawableSize.width, 1)),
        Float(max(view.drawableSize.height, 1))
      )
      uniforms.time = time
      uniforms.speed = Float(speed)
      uniforms.scale = Float(scale)
      uniforms.flow = Float(flow)
      uniforms.distortion = Float(distortion)
      uniforms.contrast = Float(contrast)
      uniforms.highlightPower = (4.0 / max(Float(highlightWidth), 0.05)) * v.highlightPowerFactor
      uniforms.highlightIntensity = Float(highlightIntensity)
      uniforms.grain = Float(grain)
      uniforms.iridescence = v.iridescence
      uniforms.baseDark = v.baseDark
      uniforms.baseLight = v.baseLight
      uniforms.highlightColor = v.highlight
      uniforms.edgeTint = v.edgeTint
      encoder?.setRenderPipelineState(pipelineState)
      encoder?.setFragmentBytes(&uniforms, length: MemoryLayout<LiquidChromeUniforms>.stride, index: 0)
      encoder?.drawPrimitives(type: .triangle, vertexStart: 0, vertexCount: 3)
    } else if let pipelineState = pipelineState(for: "solid") {
      var uniforms = Uniforms(color: parseColor(color), time: time)
      encoder?.setRenderPipelineState(pipelineState)
      encoder?.setFragmentBytes(&uniforms, length: MemoryLayout<Uniforms>.stride, index: 0)
      encoder?.drawPrimitives(type: .triangle, vertexStart: 0, vertexCount: 3)
    }

    encoder?.endEncoding()
    commandBuffer?.present(drawable)
    commandBuffer?.commit()
  }

  private var shouldAnimate: Bool {
    animated && !paused && debugTime < 0
  }

  private func drawIfNeeded() {
    if !shouldAnimate {
      metalView.setNeedsDisplay()
    }
  }

  // Cached pipeline lookup by material name — never recreated per frame.
  private func pipelineState(for material: String) -> MTLRenderPipelineState? {
    if let cached = pipelineCache[material] {
      return cached
    }
    guard let device, let library else {
      return nil
    }

    let vertexName: String
    let fragmentName: String
    switch material {
    case "fluidGradient":
      vertexName = "fluidVertex"
      fragmentName = "fluidFragment"
    case "liquidChrome":
      vertexName = "liquidChromeVertex"
      fragmentName = "liquidChromeFragment"
    default:
      vertexName = "vertexMain"
      fragmentName = "fragmentMain"
    }

    do {
      let descriptor = MTLRenderPipelineDescriptor()
      descriptor.vertexFunction = library.makeFunction(name: vertexName)
      descriptor.fragmentFunction = library.makeFunction(name: fragmentName)
      descriptor.colorAttachments[0].pixelFormat = metalView.colorPixelFormat
      let state = try device.makeRenderPipelineState(descriptor: descriptor)
      pipelineCache[material] = state
      return state
    } catch {
      return nil
    }
  }

  // Reparse hex palette (max 6, extras ignored, empty -> single black).
  private func updatePalette() {
    let parsed = colors.prefix(6).map { parseColor($0) }
    if parsed.isEmpty {
      paletteColors = [SIMD4<Float>(0, 0, 0, 1)]
      paletteCount = 1
    } else {
      paletteColors = Array(parsed)
      paletteCount = Float(paletteColors.count)
    }
  }

  private func paletteColor(_ index: Int) -> SIMD4<Float> {
    index < paletteColors.count ? paletteColors[index] : SIMD4<Float>(0, 0, 0, 1)
  }

  private func parseColor(_ value: String) -> SIMD4<Float> {
    let hex = value.trimmingCharacters(in: CharacterSet(charactersIn: "#"))
    guard hex.count == 6 || hex.count == 8, let raw = UInt32(hex, radix: 16) else {
      return SIMD4<Float>(0, 0, 0, 1)
    }

    if hex.count == 8 {
      return SIMD4<Float>(
        Float((raw >> 24) & 0xff) / 255,
        Float((raw >> 16) & 0xff) / 255,
        Float((raw >> 8) & 0xff) / 255,
        Float(raw & 0xff) / 255
      )
    }

    return SIMD4<Float>(
      Float((raw >> 16) & 0xff) / 255,
      Float((raw >> 8) & 0xff) / 255,
      Float(raw & 0xff) / 255,
      1
    )
  }

  private static let shaderSource = """
  #include <metal_stdlib>
  using namespace metal;

  struct VertexOut {
    float4 position [[position]];
  };

  struct Uniforms {
    float4 color;
    float time;
  };

  vertex VertexOut vertexMain(uint vertexID [[vertex_id]]) {
    float2 positions[3] = {
      float2(-1.0, -1.0),
      float2(3.0, -1.0),
      float2(-1.0, 3.0)
    };

    VertexOut out;
    out.position = float4(positions[vertexID], 0.0, 1.0);
    return out;
  }

  fragment float4 fragmentMain(VertexOut in [[stage_in]],
                               constant Uniforms& uniforms [[buffer(0)]]) {
    return uniforms.color;
  }

  // ---- fluidGradient material ----
  // Source of truth: ios/Shaders/fluid-gradient.metal (kept in sync).
  struct FluidUniforms {
    float time;
    float speed;
    float scale;
    float warp;
    float intensity;
    float grain;
    float2 resolution;
    float colorCount;
    float4 colors[6];
  };

  vertex VertexOut fluidVertex(uint vertexID [[vertex_id]]) {
    float2 positions[3] = {
      float2(-1.0, -1.0),
      float2(3.0, -1.0),
      float2(-1.0, 3.0)
    };
    VertexOut out;
    out.position = float4(positions[vertexID], 0.0, 1.0);
    return out;
  }

  static float fluidHash21(float2 p) {
    p = fract(p * float2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
  }

  static float fluidValueNoise(float2 p) {
    float2 i = floor(p);
    float2 f = fract(p);
    float a = fluidHash21(i);
    float b = fluidHash21(i + float2(1.0, 0.0));
    float c = fluidHash21(i + float2(0.0, 1.0));
    float d = fluidHash21(i + float2(1.0, 1.0));
    float2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
  }

  static float fluidFbm(float2 p) {
    float value = 0.0;
    float amplitude = 0.5;
    for (int i = 0; i < 4; i++) {
      value += amplitude * fluidValueNoise(p);
      p *= 2.0;
      amplitude *= 0.5;
    }
    return value;
  }

  static float3 fluidPalette(float t, constant FluidUniforms& u) {
    int count = int(clamp(u.colorCount, 1.0, 6.0));
    if (count <= 1) {
      return u.colors[0].xyz;
    }
    float scaled = clamp(t, 0.0, 1.0) * float(count - 1);
    int idx = int(floor(scaled));
    idx = min(idx, count - 2);
    float f = scaled - float(idx);
    return mix(u.colors[idx].xyz, u.colors[idx + 1].xyz, f);
  }

  fragment float4 fluidFragment(VertexOut in [[stage_in]],
                                constant FluidUniforms& u [[buffer(0)]]) {
    float2 fragCoord = in.position.xy;
    float2 uv = fragCoord / u.resolution;

    float t = u.time * u.speed;

    float2 velA = float2(0.10, 0.08);
    float2 velB = float2(-0.06, 0.12);
    float2 velC = float2(0.05, -0.07);
    float2 offset = float2(5.2, 1.3);

    float2 p = uv * u.scale;
    float2 q = float2(fluidFbm(p + t * velA), fluidFbm(p + offset + t * velB));
    float2 r = p + u.warp * q;
    float v = fluidFbm(r + t * velC);

    v = clamp(0.5 + (v - 0.5) * u.intensity, 0.0, 1.0);

    float3 rgb = fluidPalette(v, u);
    rgb += (fluidHash21(fragCoord) - 0.5) * u.grain * 0.08;

    return float4(rgb, 1.0);
  }

  // ---- liquidChrome material ----
  // Source of truth: ios/Shaders/liquid-chrome.metal (kept in sync).
  // Finite differences (NOT dFdx/dFdy) for parity with the AGSL counterpart.
  struct LiquidChromeUniforms {
    float2 resolution;
    float time;
    float speed;
    float scale;
    float flow;
    float distortion;
    float contrast;
    float highlightPower;
    float highlightIntensity;
    float grain;
    float iridescence;
    float4 baseDark;
    float4 baseLight;
    float4 highlightColor;
    float4 edgeTint;
  };

  vertex VertexOut liquidChromeVertex(uint vertexID [[vertex_id]]) {
    float2 positions[3] = {
      float2(-1.0, -1.0),
      float2(3.0, -1.0),
      float2(-1.0, 3.0)
    };
    VertexOut out;
    out.position = float4(positions[vertexID], 0.0, 1.0);
    return out;
  }

  static float chromeHash21(float2 p) {
    p = fract(p * float2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
  }

  static float chromeValueNoise(float2 p) {
    float2 i = floor(p);
    float2 f = fract(p);
    float a = chromeHash21(i);
    float b = chromeHash21(i + float2(1.0, 0.0));
    float c = chromeHash21(i + float2(0.0, 1.0));
    float d = chromeHash21(i + float2(1.0, 1.0));
    float2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
  }

  static float chromeFbm(float2 p) {
    float value = 0.0;
    float amplitude = 0.5;
    for (int i = 0; i < 4; i++) {
      value += amplitude * chromeValueNoise(p);
      p *= 2.0;
      amplitude *= 0.5;
    }
    return value;
  }

  static float heightAt(float2 p, float t, float flow) {
    float h1 = chromeFbm(p + t * float2(0.30, 0.24) * flow);
    float h2 = chromeFbm(p * 1.7 + float2(h1, h1) + t * float2(-0.20, 0.34) * flow);
    return h1 * 0.55 + h2 * 0.45;
  }

  fragment float4 liquidChromeFragment(VertexOut in [[stage_in]],
                                       constant LiquidChromeUniforms& u [[buffer(0)]]) {
    float2 fragCoord = in.position.xy;
    float2 uv = fragCoord / u.resolution;
    float t = u.time * u.speed;
    float2 p = uv * u.scale;

    float eps = 4.0 / min(u.resolution.x, u.resolution.y) * u.scale;
    float hx = heightAt(p + float2(eps, 0.0), t, u.flow) - heightAt(p - float2(eps, 0.0), t, u.flow);
    float hy = heightAt(p + float2(0.0, eps), t, u.flow) - heightAt(p - float2(0.0, eps), t, u.flow);
    float3 normal = normalize(float3(-hx * u.distortion, -hy * u.distortion, 0.6));

    float2 reflection = normal.xy * 0.5 + 0.5;
    float bands = 0.45 * sin(reflection.y * 6.0 + normal.x * 3.0)
                + 0.35 * sin(reflection.x * 4.0 + t * 0.35)
                + 0.20 * sin((reflection.x + reflection.y) * 8.0);
    float chrome = smoothstep(0.15, 0.90, bands);
    float fresnel = pow(1.0 - max(dot(normal, float3(0.0, 0.0, 1.0)), 0.0), 3.0);

    float3 rgb = mix(u.baseDark.xyz, u.baseLight.xyz, chrome);
    rgb += u.highlightColor.xyz * pow(chrome, u.highlightPower) * u.highlightIntensity;
    rgb += u.edgeTint.xyz * fresnel;

    float ph = (reflection.x + reflection.y) * 6.2831853;
    rgb += float3(sin(ph), sin(ph + 2.0944), sin(ph + 4.1888)) * u.iridescence;

    rgb = clamp((rgb - 0.5) * u.contrast + 0.5, 0.0, 1.0);
    rgb += (chromeHash21(fragCoord) - 0.5) * u.grain * 0.08;

    return float4(rgb, 1.0);
  }
  """
}
