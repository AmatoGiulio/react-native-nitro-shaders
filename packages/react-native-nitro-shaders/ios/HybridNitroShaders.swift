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

  // Layout MUST match LiquidMetalUniforms in liquid-metal.metal (float4 per color).
  private struct LiquidMetalUniforms {
    var resolution: SIMD2<Float> = SIMD2<Float>(1, 1)
    var time: Float = 0
    var speed: Float = 1
    var repetition: Float = 4
    var softness: Float = 0.5
    var shiftRed: Float = 0.3
    var shiftBlue: Float = 0.3
    var distortion: Float = 0
    var contour: Float = 0.5
    var angle: Float = 0
    var shape: Float = 1
    var colorBack: SIMD4<Float> = SIMD4<Float>(0, 0, 0, 0)
    var colorTint: SIMD4<Float> = SIMD4<Float>(0, 0, 0, 0)
  }

  private static func liquidMetalShape(_ name: String) -> Float {
    switch name {
    case "none": return 0
    case "circle": return 1
    case "daisy": return 2
    case "diamond": return 3
    case "metaballs": return 4
    default: return 1
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

  // liquidMetal props
  var distortion: Double = 0 {
    didSet {
      drawIfNeeded()
    }
  }

  var shape: String = "circle" {
    didSet {
      drawIfNeeded()
    }
  }

  var colorBack: String = "#00000000" {
    didSet {
      drawIfNeeded()
    }
  }

  var colorTint: String = "#00000000" {
    didSet {
      drawIfNeeded()
    }
  }

  var repetition: Double = 4.0 {
    didSet {
      drawIfNeeded()
    }
  }

  var softness: Double = 0.5 {
    didSet {
      drawIfNeeded()
    }
  }

  var shiftRed: Double = 0.3 {
    didSet {
      drawIfNeeded()
    }
  }

  var shiftBlue: Double = 0.3 {
    didSet {
      drawIfNeeded()
    }
  }

  var contour: Double = 0.5 {
    didSet {
      drawIfNeeded()
    }
  }

  var angle: Double = 0.0 {
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
    } else if shader == "liquidMetal", let pipelineState = pipelineState(for: "liquidMetal") {
      var uniforms = LiquidMetalUniforms()
      uniforms.resolution = SIMD2<Float>(
        Float(max(view.drawableSize.width, 1)),
        Float(max(view.drawableSize.height, 1))
      )
      uniforms.time = time
      uniforms.speed = Float(speed)
      uniforms.repetition = Float(repetition)
      uniforms.softness = Float(softness)
      uniforms.shiftRed = Float(shiftRed)
      uniforms.shiftBlue = Float(shiftBlue)
      uniforms.distortion = Float(distortion)
      uniforms.contour = Float(contour)
      uniforms.angle = Float(angle)
      uniforms.shape = Self.liquidMetalShape(shape)
      uniforms.colorBack = parseColor(colorBack)
      uniforms.colorTint = parseColor(colorTint)
      encoder?.setRenderPipelineState(pipelineState)
      encoder?.setFragmentBytes(&uniforms, length: MemoryLayout<LiquidMetalUniforms>.stride, index: 0)
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
    case "liquidMetal":
      vertexName = "liquidMetalVertex"
      fragmentName = "liquidMetalFragment"
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

  // ---- liquidMetal material ----
  // Source of truth: ios/Shaders/liquid-metal.metal (kept in sync).
  // Ported from paper-design/shaders (liquid-metal), Apache License 2.0.
  // NO fwidth/dfdx (parity with AGSL): fixed analytic edge/stripe AA.
  struct LiquidMetalUniforms {
    float2 resolution;
    float time;
    float speed;
    float repetition;
    float softness;
    float shiftRed;
    float shiftBlue;
    float distortion;
    float contour;
    float angle;
    float shape;
    float4 colorBack;
    float4 colorTint;
  };

  constant float LM_PI = 3.14159265358979323846;

  vertex VertexOut liquidMetalVertex(uint vertexID [[vertex_id]]) {
    float2 positions[3] = {
      float2(-1.0, -1.0),
      float2(3.0, -1.0),
      float2(-1.0, 3.0)
    };
    VertexOut out;
    out.position = float4(positions[vertexID], 0.0, 1.0);
    return out;
  }

  static float2 lmRotate(float2 p, float a) {
    float c = cos(a);
    float s = sin(a);
    return float2(p.x * c - p.y * s, p.x * s + p.y * c);
  }

  static float3 lmMod289v3(float3 x) {
    return x - floor(x * (1.0 / 289.0)) * 289.0;
  }

  static float2 lmMod289v2(float2 x) {
    return x - floor(x * (1.0 / 289.0)) * 289.0;
  }

  static float3 lmPermute(float3 x) {
    return lmMod289v3(((x * 34.0) + 1.0) * x);
  }

  static float lmSnoise(float2 v) {
    const float4 C = float4(0.211324865405187,
                            0.366025403784439,
                            -0.577350269189626,
                            0.024390243902439);
    float2 i = floor(v + dot(v, C.yy));
    float2 x0 = v - i + dot(i, C.xx);
    float2 i1 = (x0.x > x0.y) ? float2(1.0, 0.0) : float2(0.0, 1.0);
    float4 x12 = x0.xyxy + C.xxzz;
    x12.xy -= i1;
    i = lmMod289v2(i);
    float3 p = lmPermute(lmPermute(i.y + float3(0.0, i1.y, 1.0)) + i.x + float3(0.0, i1.x, 1.0));
    float3 m = max(0.5 - float3(dot(x0, x0), dot(x12.xy, x12.xy), dot(x12.zw, x12.zw)), 0.0);
    m = m * m;
    m = m * m;
    float3 x = 2.0 * fract(p * C.www) - 1.0;
    float3 h = abs(x) - 0.5;
    float3 ox = floor(x + 0.5);
    float3 a0 = x - ox;
    m *= 1.79284291400159 - 0.85373472095314 * (a0 * a0 + h * h);
    float3 g;
    g.x = a0.x * x0.x + h.x * x0.y;
    g.yz = a0.yz * x12.xz + h.yz * x12.yw;
    return 130.0 * dot(m, g);
  }

  static float lmGetColorChanges(float c1, float c2, float stripe_p, float3 w,
                                 float blur, float bump, float tint, float tintAlpha) {
    float ch = mix(c2, c1, smoothstep(0.0, 2.0 * blur, stripe_p));
    float border = w[0];
    ch = mix(ch, c2, smoothstep(border, border + 2.0 * blur, stripe_p));
    border = w[0] + 0.4 * (1.0 - bump) * w[1];
    ch = mix(ch, c1, smoothstep(border, border + 2.0 * blur, stripe_p));
    border = w[0] + 0.5 * (1.0 - bump) * w[1];
    ch = mix(ch, c2, smoothstep(border, border + 2.0 * blur, stripe_p));
    border = w[0] + w[1];
    ch = mix(ch, c1, smoothstep(border, border + 2.0 * blur, stripe_p));
    float gradient_t = (stripe_p - w[0] - w[1]) / w[2];
    float gradient = mix(c1, c2, smoothstep(0.0, 1.0, gradient_t));
    ch = mix(ch, gradient, smoothstep(border, border + 0.5 * blur, stripe_p));
    ch = mix(ch, 1.0 - min(1.0, (1.0 - ch) / max(tint, 0.0001)), tintAlpha);
    return ch;
  }

  fragment float4 liquidMetalFragment(VertexOut in [[stage_in]],
                                      constant LiquidMetalUniforms& u [[buffer(0)]]) {
    float2 fragCoord = in.position.xy;

    const float edgeAA = 0.02;
    float stripeAA = 1.5 * u.repetition / min(u.resolution.x, u.resolution.y);

    const float firstFrameOffset = 2.8;
    float t = 0.3 * (u.time * u.speed + firstFrameOffset);
    // Loop the shared material pass while shape-local motion keeps its timing.
    float loopTravel = 3.0;
    float phase = fract(t / loopTravel);
    float loopT = phase * loopTravel;

    float2 uv = fragCoord / u.resolution;

    float cycleWidth = u.repetition;
    float edge = 0.0;
    float contOffset = 1.0;

    float ratio = u.resolution.x / u.resolution.y;

    float2 rotatedUV = uv - float2(0.5);
    float angle = (-u.angle + 70.0) * LM_PI / 180.0;
    float cosA = cos(angle);
    float sinA = sin(angle);
    rotatedUV = float2(
      rotatedUV.x * cosA - rotatedUV.y * sinA,
      rotatedUV.x * sinA + rotatedUV.y * cosA
    ) + float2(0.5);

    if (u.shape < 1.0) {
      float2 borderUV = uv;
      float2 mask = min(borderUV, 1.0 - borderUV);
      float2 pixel_thickness = min(250.0 / u.resolution, float2(0.5));
      float maskX = smoothstep(0.0, pixel_thickness.x, mask.x);
      float maskY = smoothstep(0.0, pixel_thickness.y, mask.y);
      maskX = pow(maskX, 0.25);
      maskY = pow(maskY, 0.25);
      edge = clamp(1.0 - maskX * maskY, 0.0, 1.0);
      if (ratio > 1.0) {
        uv.y /= ratio;
      } else {
        uv.x *= ratio;
      }
      cycleWidth *= 2.0;
      contOffset = 1.5;
    } else if (u.shape < 2.0) {
      float2 shapeUV = uv - 0.5;
      shapeUV *= 0.67;
      edge = pow(clamp(3.0 * length(shapeUV), 0.0, 1.0), 18.0);
    } else if (u.shape < 3.0) {
      float2 shapeUV = uv - 0.5;
      shapeUV *= 1.68;
      float r = length(shapeUV) * 2.0;
      float a = atan2(shapeUV.y, shapeUV.x) + 0.2;
      r *= (1.0 + 0.05 * sin(3.0 * a + 2.0 * t));
      float f = abs(cos(a * 3.0));
      edge = smoothstep(f, f + 0.7, r);
      edge *= edge;
      uv *= 0.8;
      cycleWidth *= 1.6;
    } else if (u.shape < 4.0) {
      float2 shapeUV = uv - 0.5;
      shapeUV = lmRotate(shapeUV, 0.25 * LM_PI);
      shapeUV *= 1.42;
      shapeUV += 0.5;
      float2 mask = min(shapeUV, 1.0 - shapeUV);
      float2 pixel_thickness = float2(0.15);
      float maskX = smoothstep(0.0, pixel_thickness.x, mask.x);
      float maskY = smoothstep(0.0, pixel_thickness.y, mask.y);
      maskX = pow(maskX, 0.25);
      maskY = pow(maskY, 0.25);
      edge = clamp(1.0 - maskX * maskY, 0.0, 1.0);
    } else if (u.shape < 5.0) {
      float2 shapeUV = uv - 0.5;
      shapeUV *= 1.3;
      edge = 0.0;
      for (int i = 0; i < 5; i++) {
        float fi = float(i);
        float speed = 1.5 + 2.0 / 3.0 * sin(fi * 12.345);
        float mangle = -fi * 1.5;
        float2 dir1 = float2(cos(mangle), sin(mangle));
        float2 dir2 = float2(cos(mangle + 1.57), sin(mangle + 1.0));
        float2 traj = 0.4 * (dir1 * sin(t * speed + fi * 1.23) + dir2 * cos(t * (speed * 0.7) + fi * 2.17));
        float d = length(shapeUV + traj);
        edge += pow(1.0 - clamp(d, 0.0, 1.0), 4.0);
      }
      edge = 1.0 - smoothstep(0.65, 0.9, edge);
      edge = pow(edge, 4.0);
    }

    edge = mix(smoothstep(0.9 - edgeAA, 0.9, edge), edge, smoothstep(0.0, 0.4, u.contour));

    float opacity = 1.0 - smoothstep(0.9 - edgeAA, 0.9, edge);
    if (u.shape < 2.0) {
      edge = 1.2 * edge;
    } else if (u.shape < 5.0) {
      edge = 1.8 * pow(edge, 1.5);
    }

    float diagBLtoTR = rotatedUV.x - rotatedUV.y;
    float diagTLtoBR = rotatedUV.x + rotatedUV.y;

    float3 color = float3(0.0);
    float3 color1 = float3(0.98, 0.98, 1.0);
    float3 color2 = float3(0.1, 0.1, 0.1 + 0.1 * smoothstep(0.7, 1.3, diagTLtoBR));

    float2 grad_uv = uv - 0.5;

    float dist = length(grad_uv + float2(0.0, 0.2 * diagBLtoTR));
    grad_uv = lmRotate(grad_uv, (0.25 - 0.2 * diagBLtoTR) * LM_PI);
    float direction = grad_uv.x;

    float bump = pow(1.8 * dist, 1.2);
    bump = 1.0 - bump;
    bump *= pow(uv.y, 0.3);

    float thin_strip_1_ratio = 0.12 / cycleWidth * (1.0 - 0.4 * bump);
    float thin_strip_2_ratio = 0.07 / cycleWidth * (1.0 + 0.4 * bump);
    float wide_strip_ratio = (1.0 - thin_strip_1_ratio - thin_strip_2_ratio);

    float thin_strip_1_width = cycleWidth * thin_strip_1_ratio;
    float thin_strip_2_width = cycleWidth * thin_strip_2_ratio;

    float blend = smoothstep(0.85, 1.0, phase);
    float noise = mix(lmSnoise(uv - loopT), lmSnoise(uv - loopT + loopTravel), blend);

    edge += (1.0 - edge) * u.distortion * noise;

    direction += diagBLtoTR;
    float contour = 0.0;
    direction -= 2.0 * noise * diagBLtoTR * (smoothstep(0.0, 1.0, edge) * (1.0 - smoothstep(0.0, 1.0, edge)));
    direction *= mix(1.0, 1.0 - edge, smoothstep(0.5, 1.0, u.contour));
    direction -= 1.7 * edge * smoothstep(0.5, 1.0, u.contour);
    direction += 0.2 * pow(u.contour, 4.0) * (1.0 - smoothstep(0.0, 1.0, edge));

    bump *= clamp(pow(uv.y, 0.1), 0.3, 1.0);
    direction *= (0.1 + (1.1 - edge) * bump);

    direction *= (0.4 + 0.6 * (1.0 - smoothstep(0.5, 1.0, edge)));
    direction += 0.18 * (smoothstep(0.1, 0.2, uv.y) * (1.0 - smoothstep(0.2, 0.4, uv.y)));
    direction += 0.03 * (smoothstep(0.1, 0.2, 1.0 - uv.y) * (1.0 - smoothstep(0.2, 0.4, 1.0 - uv.y)));

    direction *= (0.5 + 0.5 * pow(uv.y, 2.0));
    direction *= cycleWidth;
    direction -= loopT;

    float colorDispersion = (1.0 - bump);
    colorDispersion = clamp(colorDispersion, 0.0, 1.0);
    float dispersionRed = colorDispersion;
    dispersionRed += 0.03 * bump * noise;
    dispersionRed += 5.0 * (smoothstep(-0.1, 0.2, uv.y) * (1.0 - smoothstep(0.1, 0.5, uv.y))) * (smoothstep(0.4, 0.6, bump) * (1.0 - smoothstep(0.4, 1.0, bump)));
    dispersionRed -= diagBLtoTR;

    float dispersionBlue = colorDispersion;
    dispersionBlue *= 1.3;
    dispersionBlue += (smoothstep(0.0, 0.4, uv.y) * (1.0 - smoothstep(0.1, 0.8, uv.y))) * (smoothstep(0.4, 0.6, bump) * (1.0 - smoothstep(0.4, 0.8, bump)));
    dispersionBlue -= 0.2 * edge;

    dispersionRed *= (u.shiftRed / 20.0);
    dispersionBlue *= (u.shiftBlue / 20.0);

    float blur = u.softness / 15.0 + 0.3 * contour;

    float3 w = float3(thin_strip_1_width, thin_strip_2_width, wide_strip_ratio);
    w[1] -= 0.02 * smoothstep(0.0, 1.0, edge + bump);
    float stripe_r = fract(direction + dispersionRed);
    float r = lmGetColorChanges(color1.r, color2.r, stripe_r, w, blur + stripeAA, bump, u.colorTint.r, u.colorTint.a);
    float stripe_g = fract(direction);
    float g = lmGetColorChanges(color1.g, color2.g, stripe_g, w, blur + stripeAA, bump, u.colorTint.g, u.colorTint.a);
    float stripe_b = fract(direction - dispersionBlue);
    float b = lmGetColorChanges(color1.b, color2.b, stripe_b, w, blur + stripeAA, bump, u.colorTint.b, u.colorTint.a);

    color = float3(r, g, b);
    color *= opacity;

    float3 bgColor = u.colorBack.rgb * u.colorBack.a;
    color = color + bgColor * (1.0 - opacity);
    opacity = opacity + u.colorBack.a * (1.0 - opacity);

    color += (fract(sin(dot(fragCoord, float2(12.9898, 78.233))) * 43758.5453123) - 0.5) / 255.0;

    return float4(color, opacity);
  }
  """
}
