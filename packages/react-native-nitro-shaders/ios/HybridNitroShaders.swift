//
//  HybridNitroShaders.swift
//  Pods
//
//  Created by Giulio Amato on 03/07/2026.
//
//  iOS runtime of the material orb. Mirrors the Android runtime structure:
//  the orb fragment program is assembled per material from bundled MSL sources
//  (orb-core.msl + material-<name>.msl + orb-main.msl) and compiled at runtime,
//  one cached pipeline per material. Environments are equirectangular PNGs in
//  the NitroShaders resource bundle (same assets as Android).
//
//  First porting pass: the silhouette is cut in-shader; the contact shadow of
//  the Android Skin is NOT rendered yet (declared debt).
//

import Foundation
import Metal
import MetalKit
import UIKit

// MTKViewDelegate requires NSObjectProtocol while the Nitro base class is not an
// NSObject — a small proxy forwards the delegate callbacks to the hybrid object.
private final class OrbViewDelegate: NSObject, MTKViewDelegate {
  weak var owner: HybridNitroShaders?

  func mtkView(_ view: MTKView, drawableSizeWillChange size: CGSize) {
    owner?.drawableSizeWillChange()
  }

  func draw(in view: MTKView) {
    owner?.render(in: view)
  }
}

class HybridNitroShaders : HybridNitroShadersSpec {
  // Legacy "solid" pipeline uniforms.
  private struct SolidUniforms {
    var color: SIMD4<Float>
    var time: Float
  }

  // MUST mirror OrbUniforms in ios/Shaders/orb-core.msl (same field order —
  // float4 first, then float2 pair, then scalar floats).
  private struct OrbUniforms {
    var lightDir: SIMD4<Float>
    var resolution: SIMD2<Float>
    var envSize: SIMD2<Float>
    var time: Float
    var speed: Float
    var morph: Float
    var orbit: Float
    var pattern: Float
    var patternScale: Float
    var patternDistortion: Float
    var tint: Float
    var opacity: Float
    var envRot: Float
    var hdrBoost: Float
    var density: Float
    var smoothness: Float
    var silDensity: Float
    var smoothSil: Float
    var motionSeed: Float
  }

  private let metalView: MTKView
  private let viewDelegate = OrbViewDelegate()
  private let device: MTLDevice?
  private let commandQueue: MTLCommandQueue?
  private var solidLibrary: MTLLibrary?
  private var solidPipeline: MTLRenderPipelineState?
  // One assembled+compiled pipeline per orb material, built lazily.
  private var orbPipelines: [String: MTLRenderPipelineState] = [:]
  // Environment textures by asset name ("studio", "lab-3", ...), lazy.
  private var envTextures: [String: MTLTexture] = [:]
  private var startTime = CACurrentMediaTime()

  var view: UIView {
    metalView
  }

  // ---- Surface selection + lifecycle ----

  var shader: String = "solid" {
    didSet { drawIfNeeded() }
  }

  var color: String = "#000000" {
    didSet { drawIfNeeded() }
  }

  var colors: [String] = [] {
    didSet { drawIfNeeded() }
  }

  var animated: Bool = false {
    didSet { applyAnimationState() }
  }

  var paused: Bool = false {
    didSet { applyAnimationState() }
  }

  var debugTime: Double = -1 {
    didSet {
      applyAnimationState()
      drawIfNeeded()
    }
  }

  // ---- Material orb: semantic parameter set ----

  var material: String = "metal" {
    didSet { drawIfNeeded() }
  }

  var speed: Double = 1 {
    didSet { drawIfNeeded() }
  }

  var morph: Double = 0.5 {
    didSet { drawIfNeeded() }
  }

  var orbit: Double = 1 {
    didSet { drawIfNeeded() }
  }

  var pattern: String = "auto" {
    didSet { drawIfNeeded() }
  }

  var patternScale: Double = 1 {
    didSet { drawIfNeeded() }
  }

  var patternDistortion: Double = 0.5 {
    didSet { drawIfNeeded() }
  }

  var tint: Double = 0.5 {
    didSet { drawIfNeeded() }
  }

  var opacity: Double = 1 {
    didSet { drawIfNeeded() }
  }

  var lightAzimuth: Double = 0 {
    didSet { drawIfNeeded() }
  }

  var lightElevation: Double = Double.pi / 2 {
    didSet { drawIfNeeded() }
  }

  var environment: Double = -1 {
    didSet { drawIfNeeded() }
  }

  var envRotation: Double = 0 {
    didSet { drawIfNeeded() }
  }

  var hdr: Bool = false {
    didSet { drawIfNeeded() }
  }

  var density: Double = 1 {
    didSet { drawIfNeeded() }
  }

  var smoothness: Double = 0 {
    didSet { drawIfNeeded() }
  }

  // ---- Motion uniform contract ----

  var motionType: Double = 0 {
    didSet { drawIfNeeded() }
  }

  var motionSpeed: Double = 0 {
    didSet { drawIfNeeded() }
  }

  var motionAmp: Double = 0 {
    didSet { drawIfNeeded() }
  }

  var motionWarp: Double = 0 {
    didSet { drawIfNeeded() }
  }

  var motionDetail: Double = 0 {
    didSet { drawIfNeeded() }
  }

  var motionSeed: Double = 0 {
    didSet { drawIfNeeded() }
  }

  var motionPeriod: Double = 0 {
    didSet { drawIfNeeded() }
  }

  override init() {
    let metalDevice = MTLCreateSystemDefaultDevice()
    device = metalDevice
    commandQueue = metalDevice?.makeCommandQueue()
    metalView = MTKView(frame: .zero, device: metalDevice)
    super.init()

    // Transparent surface: the orb floats over whatever is behind the view.
    metalView.clearColor = MTLClearColor(red: 0, green: 0, blue: 0, alpha: 0)
    metalView.isOpaque = false
    metalView.backgroundColor = .clear
    metalView.framebufferOnly = true
    metalView.isPaused = true
    metalView.enableSetNeedsDisplay = true
    viewDelegate.owner = self
    metalView.delegate = viewDelegate
    solidLibrary = try? metalDevice?.makeLibrary(source: Self.solidShaderSource, options: nil)
  }

  fileprivate func drawableSizeWillChange() {
    drawIfNeeded()
  }

  fileprivate func render(in view: MTKView) {
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

    if shader == "materialOrb" {
      drawOrb(encoder: encoder, time: time)
    } else {
      drawSolid(encoder: encoder, time: time)
    }

    encoder?.endEncoding()
    commandBuffer?.present(drawable)
    commandBuffer?.commit()
  }

  // ---- Material orb drawing ----

  private static let orbMaterials: Set<String> = ["metal", "water", "iridescent", "glass"]

  private var resolvedMaterial: String {
    Self.orbMaterials.contains(material) ? material : "metal"
  }

  // Motion params fall back to the semantic params when unset (0) — mirrors
  // ShaderSurfaceView.resolvedMotion* on Android.
  private var resolvedSpeed: Float { motionSpeed > 0 ? Float(motionSpeed) : Float(speed) }
  private var resolvedMorph: Float { motionAmp > 0 ? Float(motionAmp) : Float(morph) }
  private var resolvedWarp: Float { motionWarp > 0 ? Float(motionWarp) : Float(patternDistortion) }
  private var resolvedScale: Float { motionDetail > 0 ? Float(motionDetail) : Float(patternScale) }

  private func defaultPatternIndex(for material: String) -> Float {
    switch material {
    case "water": return 2   // ripples
    case "glass": return 1   // bands
    default: return 0        // folds (metal/iridescent)
    }
  }

  private func patternIndex(for material: String) -> Float {
    switch pattern {
    case "folds": return 0
    case "bands": return 1
    case "ripples": return 2
    default: return defaultPatternIndex(for: material)
    }
  }

  // Silhouette family + density + default env, mirroring ShaderMaterial.kt.
  private func isSmoothSilhouette(_ material: String) -> Bool {
    material == "water" || material == "glass"
  }

  private func silhouetteDensity(_ material: String) -> Float {
    switch material {
    case "metal": return 0.92
    case "water", "glass": return 0.45
    default: return 0.62
    }
  }

  private func defaultEnvName(_ material: String) -> String {
    switch material {
    case "water": return "water-env"
    case "glass": return "glass-env"
    default: return "studio"
    }
  }

  private func drawOrb(encoder: MTLRenderCommandEncoder?, time: Float) {
    let material = resolvedMaterial
    guard
      let pipeline = orbPipeline(for: material),
      let envTexture = envTexture(for: material)
    else {
      return
    }

    let size = metalView.drawableSize
    let az = Float(lightAzimuth)
    let el = Float(lightElevation)
    var uniforms = OrbUniforms(
      lightDir: SIMD4<Float>(sin(az) * cos(el), sin(el), cos(az) * cos(el), 0),
      resolution: SIMD2<Float>(Float(size.width), Float(size.height)),
      envSize: SIMD2<Float>(Float(envTexture.width), Float(envTexture.height)),
      time: time,
      speed: resolvedSpeed,
      morph: resolvedMorph,
      orbit: Float(orbit),
      pattern: patternIndex(for: material),
      patternScale: resolvedScale,
      patternDistortion: resolvedWarp,
      tint: Float(tint),
      opacity: Float(opacity),
      envRot: Float(envRotation),
      hdrBoost: hdr ? 1 : 0,
      density: Float(density),
      smoothness: Float(smoothness),
      silDensity: silhouetteDensity(material),
      smoothSil: isSmoothSilhouette(material) ? 1 : 0,
      motionSeed: Float(motionSeed)
    )

    encoder?.setRenderPipelineState(pipeline)
    encoder?.setFragmentBytes(&uniforms, length: MemoryLayout<OrbUniforms>.stride, index: 0)
    encoder?.setFragmentTexture(envTexture, index: 0)
    encoder?.drawPrimitives(type: .triangle, vertexStart: 0, vertexCount: 3)
  }

  private func drawSolid(encoder: MTLRenderCommandEncoder?, time: Float) {
    guard let pipeline = solidPipelineState() else {
      return
    }
    var uniforms = SolidUniforms(color: Self.parseColor(color), time: time)
    encoder?.setRenderPipelineState(pipeline)
    encoder?.setFragmentBytes(&uniforms, length: MemoryLayout<SolidUniforms>.stride, index: 0)
    encoder?.drawPrimitives(type: .triangle, vertexStart: 0, vertexCount: 3)
  }

  // ---- Pipelines + resources ----

  private static let resourceBundle: Bundle? = {
    let frameworkBundle = Bundle(for: HybridNitroShaders.self)
    for candidate in [frameworkBundle, Bundle.main] {
      if let url = candidate.url(forResource: "NitroShaders", withExtension: "bundle"),
         let bundle = Bundle(url: url) {
        return bundle
      }
    }
    return nil
  }()

  private func loadShaderSource(_ name: String) -> String? {
    guard let url = Self.resourceBundle?.url(forResource: name, withExtension: "msl") else {
      return nil
    }
    return try? String(contentsOf: url, encoding: .utf8)
  }

  private func orbPipeline(for material: String) -> MTLRenderPipelineState? {
    if let cached = orbPipelines[material] {
      return cached
    }
    guard
      let device,
      let core = loadShaderSource("orb-core"),
      let mat = loadShaderSource("material-\(material)"),
      let main = loadShaderSource("orb-main")
    else {
      return nil
    }
    do {
      let source = core + "\n" + mat + "\n" + main
      let library = try device.makeLibrary(source: source, options: nil)
      let descriptor = MTLRenderPipelineDescriptor()
      descriptor.vertexFunction = library.makeFunction(name: "orbVertex")
      descriptor.fragmentFunction = library.makeFunction(name: "orbFragment")
      let attachment = descriptor.colorAttachments[0]
      attachment?.pixelFormat = metalView.colorPixelFormat
      // The fragment returns premultiplied alpha.
      attachment?.isBlendingEnabled = true
      attachment?.sourceRGBBlendFactor = .one
      attachment?.destinationRGBBlendFactor = .oneMinusSourceAlpha
      attachment?.sourceAlphaBlendFactor = .one
      attachment?.destinationAlphaBlendFactor = .oneMinusSourceAlpha
      let state = try device.makeRenderPipelineState(descriptor: descriptor)
      orbPipelines[material] = state
      return state
    } catch {
      return nil
    }
  }

  private func envTexture(for material: String) -> MTLTexture? {
    let labIndex = Int(environment)
    let name = labIndex >= 0 ? "lab-\(labIndex)" : defaultEnvName(material)
    if let cached = envTextures[name] {
      return cached
    }
    guard
      let device,
      let url = Self.resourceBundle?.url(forResource: name, withExtension: "png")
    else {
      return nil
    }
    let loader = MTKTextureLoader(device: device)
    let options: [MTKTextureLoader.Option: Any] = [
      .SRGB: false,
      .textureUsage: MTLTextureUsage.shaderRead.rawValue,
      .textureStorageMode: MTLStorageMode.private.rawValue,
    ]
    guard let texture = try? loader.newTexture(URL: url, options: options) else {
      return nil
    }
    envTextures[name] = texture
    return texture
  }

  private func solidPipelineState() -> MTLRenderPipelineState? {
    if let cached = solidPipeline {
      return cached
    }
    guard let device, let library = solidLibrary else {
      return nil
    }
    do {
      let descriptor = MTLRenderPipelineDescriptor()
      descriptor.vertexFunction = library.makeFunction(name: "vertexMain")
      descriptor.fragmentFunction = library.makeFunction(name: "fragmentMain")
      descriptor.colorAttachments[0].pixelFormat = metalView.colorPixelFormat
      let state = try device.makeRenderPipelineState(descriptor: descriptor)
      solidPipeline = state
      return state
    } catch {
      return nil
    }
  }

  // ---- Animation / invalidation ----

  private var shouldAnimate: Bool {
    animated && !paused && debugTime < 0
  }

  private func applyAnimationState() {
    metalView.enableSetNeedsDisplay = !shouldAnimate
    metalView.isPaused = !shouldAnimate
    drawIfNeeded()
  }

  private func drawIfNeeded() {
    if !shouldAnimate {
      metalView.setNeedsDisplay()
    }
  }

  // ---- Helpers ----

  private static func parseColor(_ value: String) -> SIMD4<Float> {
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

  private static let solidShaderSource = """
  #include <metal_stdlib>
  using namespace metal;

  struct SolidVertexOut {
    float4 position [[position]];
  };

  struct SolidUniforms {
    float4 color;
    float time;
  };

  vertex SolidVertexOut vertexMain(uint vertexID [[vertex_id]]) {
    float2 positions[3] = {
      float2(-1.0, -1.0),
      float2(3.0, -1.0),
      float2(-1.0, 3.0)
    };

    SolidVertexOut out;
    out.position = float4(positions[vertexID], 0.0, 1.0);
    return out;
  }

  fragment float4 fragmentMain(SolidVertexOut in [[stage_in]],
                               constant SolidUniforms& uniforms [[buffer(0)]]) {
    return uniforms.color;
  }

  """
}
