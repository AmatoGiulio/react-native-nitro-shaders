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
  private var pipelineState: MTLRenderPipelineState?
  private var startTime = CACurrentMediaTime()

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
    pipelineState = makePipelineState()
  }

  func mtkView(_ view: MTKView, drawableSizeWillChange size: CGSize) {
    drawIfNeeded()
  }

  func draw(in view: MTKView) {
    guard
      let commandQueue,
      let pipelineState,
      let drawable = view.currentDrawable,
      let descriptor = view.currentRenderPassDescriptor
    else {
      return
    }

    var uniforms = Uniforms(
      color: parseColor(color),
      time: Float(debugTime >= 0 ? debugTime : CACurrentMediaTime() - startTime)
    )

    let commandBuffer = commandQueue.makeCommandBuffer()
    let encoder = commandBuffer?.makeRenderCommandEncoder(descriptor: descriptor)
    encoder?.setRenderPipelineState(pipelineState)
    encoder?.setFragmentBytes(&uniforms, length: MemoryLayout<Uniforms>.stride, index: 0)
    encoder?.drawPrimitives(type: .triangle, vertexStart: 0, vertexCount: 3)
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

  private func makePipelineState() -> MTLRenderPipelineState? {
    guard let device else {
      return nil
    }

    do {
      let library = try device.makeLibrary(source: Self.shaderSource, options: nil)
      let descriptor = MTLRenderPipelineDescriptor()
      descriptor.vertexFunction = library.makeFunction(name: "vertexMain")
      descriptor.fragmentFunction = library.makeFunction(name: "fragmentMain")
      descriptor.colorAttachments[0].pixelFormat = metalView.colorPixelFormat
      return try device.makeRenderPipelineState(descriptor: descriptor)
    } catch {
      return nil
    }
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
