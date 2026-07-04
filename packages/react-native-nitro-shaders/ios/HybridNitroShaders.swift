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

    if let pipelineState = pipelineState(for: "solid") {
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

    let vertexName = "vertexMain"
    let fragmentName = "fragmentMain"

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

  """
}
