#include <metal_stdlib>
using namespace metal;

// Fluid gradient material — Metal counterpart of the AGSL fluidGradient shader.
// Math and constants MUST stay identical to the Android/AGSL version for visual parity.
//
// Uniform layout note:
//   Metal does not allow packed_float3 arrays inside a `constant` struct cleanly,
//   and a float3 is 16-byte aligned anyway. To keep the Swift <-> Metal layout
//   unambiguous we store the 6 palette colors as float4 (rgb in .xyz, .w unused),
//   so every field lands on a natural 16-byte boundary with no hidden padding.
struct FluidUniforms {
  float time;
  float speed;
  float scale;
  float warp;
  float intensity;
  float grain;
  float2 resolution;
  float colorCount;   // 1..6
  float4 colors[6];   // rgb in xyz, w unused
};

struct VertexOut {
  float4 position [[position]];
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

// 2D hash noise.
static float hash21(float2 p) {
  p = fract(p * float2(123.34, 456.21));
  p += dot(p, p + 45.32);
  return fract(p.x * p.y);
}

// Value noise with smoothstep interpolation.
static float valueNoise(float2 p) {
  float2 i = floor(p);
  float2 f = fract(p);
  float a = hash21(i);
  float b = hash21(i + float2(1.0, 0.0));
  float c = hash21(i + float2(0.0, 1.0));
  float d = hash21(i + float2(1.0, 1.0));
  float2 u = f * f * (3.0 - 2.0 * f); // smoothstep
  return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}

// fBm: 4 octaves, lacunarity 2.0, gain 0.5.
static float fbm(float2 p) {
  float value = 0.0;
  float amplitude = 0.5;
  for (int i = 0; i < 4; i++) {
    value += amplitude * valueNoise(p);
    p *= 2.0;         // lacunarity
    amplitude *= 0.5; // gain
  }
  return value;
}

// Piecewise-linear palette over colorCount (1..6) segments.
static float3 palette(float t, constant FluidUniforms& u) {
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
  float2 q = float2(fbm(p + t * velA), fbm(p + offset + t * velB));
  float2 r = p + u.warp * q;
  float v = fbm(r + t * velC);

  v = clamp(0.5 + (v - 0.5) * u.intensity, 0.0, 1.0);

  float3 rgb = palette(v, u);
  rgb += (hash21(fragCoord) - 0.5) * u.grain * 0.08;

  return float4(rgb, 1.0);
}
