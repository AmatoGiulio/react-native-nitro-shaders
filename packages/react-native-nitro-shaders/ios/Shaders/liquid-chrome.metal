#include <metal_stdlib>
using namespace metal;

// Liquid chrome material — Metal counterpart of the AGSL liquidChrome shader.
// Math and constants MUST stay identical to the Android/AGSL version for visual parity.
// Uses finite differences (NOT dFdx/dFdy) to match AGSL exactly.
//
// Uniform layout note:
//   Colors are stored as float4 (rgb in .xyz, .w unused) so every field lands on a
//   natural 16-byte boundary with no hidden padding, keeping the Swift <-> Metal
//   layout unambiguous.
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
  float4 baseDark;        // rgb in xyz, w unused
  float4 baseLight;       // rgb in xyz, w unused
  float4 highlightColor;  // rgb in xyz, w unused
  float4 edgeTint;        // rgb in xyz, w unused
};

struct VertexOut {
  float4 position [[position]];
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

// 2D hash noise — identical to fluidGradient's hash21.
static float chromeHash21(float2 p) {
  p = fract(p * float2(123.34, 456.21));
  p += dot(p, p + 45.32);
  return fract(p.x * p.y);
}

// Value noise with smoothstep interpolation.
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

// fBm: 4 octaves, lacunarity 2.0, gain 0.5.
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
