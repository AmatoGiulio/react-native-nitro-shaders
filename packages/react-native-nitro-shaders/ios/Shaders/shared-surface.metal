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
