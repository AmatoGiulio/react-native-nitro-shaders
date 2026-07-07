import type { MaterialName } from './catalog'

// Surface texture families, decoupled from the material BRDF. Any material can
// render any pattern at runtime: the switch is a plain uniform change on the
// native side, no shader rebuild. Omit (or 'auto') for the material default.
export type OrbPattern = 'folds' | 'bands' | 'ripples'

export type OrbLight = {
  /** horizontal angle of the key light, radians (0 = front) */
  azimuth?: number
  /** vertical angle of the key light, radians (π/2 = straight above) */
  elevation?: number
}

// Semantic, material-agnostic parameter set. Every knob has ONE meaning across
// all materials; material-specific defaults live in MATERIAL_PRESETS.
export type OrbParams = {
  /** global time scale — everything (pattern, silhouette, breath) follows it */
  speed?: number
  /** how much the shape transforms on itself (silhouette swells + surface relief) */
  morph?: number
  /** residual 3D orbit of the sphere — how fast the pattern rotates in space */
  orbit?: number
  /** surface texture family; omit for the material default */
  pattern?: OrbPattern
  /** texture frequency */
  patternScale?: number
  /** texture domain warp / turbulence */
  patternDistortion?: number
  /** material color axis (metal = warmth, water = frost, glass = dispersion/frost) */
  tint?: number
  /** overall opacity of the orb (1 = opaque, 0 = invisible) */
  opacity?: number
  /** key light direction — affects the lit materials (water top light) */
  light?: OrbLight
  /** runtime environment override: index of assets env/lab-N.png */
  environment?: number
  /** horizontal rotation of the environment in radians (move the HDRI) */
  envRotation?: number
  /** pseudo-HDR highlight expansion on the reflected environment */
  hdr?: boolean
  /** water: body density (volume absorption strength) */
  density?: number
  /** water: smooth glassy patch amount (0 = fully rippled, 1 = mostly smooth) */
  smoothness?: number
}

// Fully-resolved flat params — what the native layer actually receives.
export type ResolvedOrbParams = {
  speed: number
  morph: number
  orbit: number
  pattern: OrbPattern
  patternScale: number
  patternDistortion: number
  tint: number
  opacity: number
  lightAzimuth: number
  lightElevation: number
  environment: number // -1 = material default env
  envRotation: number
  hdr: boolean
  density: number
  smoothness: number
}

const LIGHT_ABOVE = Math.PI / 2 // key light straight above (validated water look)

// Per-material presets — frozen from Giulio's on-device validations (2026-07-06
// for water and glass; metal/iridescent from the IBL milestone look).
export const MATERIAL_PRESETS: Record<MaterialName, ResolvedOrbParams> = {
  metal: {
    speed: 0.9,
    morph: 0.72,
    orbit: 1,
    pattern: 'folds',
    patternScale: 1,
    patternDistortion: 0.56,
    tint: 0.42,
    opacity: 1,
    lightAzimuth: 0,
    lightElevation: LIGHT_ABOVE,
    environment: -1,
    envRotation: 0,
    hdr: false, // hdr=false is bit-identical to the validated chrome look
    density: 1,
    smoothness: 0,
  },
  water: {
    speed: 2.0,
    morph: 0.38,
    orbit: 1,
    pattern: 'ripples',
    patternScale: 0.19,
    patternDistortion: 0,
    tint: 0.2,
    opacity: 1,
    lightAzimuth: 0,
    lightElevation: LIGHT_ABOVE,
    environment: -1,
    envRotation: 0,
    hdr: false,
    density: 1.11,
    smoothness: 0.6,
  },
  iridescent: {
    speed: 0.9,
    morph: 0.34,
    orbit: 1,
    pattern: 'folds',
    patternScale: 1,
    patternDistortion: 0.28,
    tint: 1,
    opacity: 1,
    lightAzimuth: 0,
    lightElevation: LIGHT_ABOVE,
    environment: -1,
    envRotation: 0,
    hdr: false,
    density: 1,
    smoothness: 0,
  },
  glass: {
    speed: 2.0,
    morph: 0.4,
    orbit: 1,
    pattern: 'bands',
    patternScale: 0.69,
    patternDistortion: 0.07,
    tint: 0.5,
    opacity: 1,
    lightAzimuth: 0,
    lightElevation: LIGHT_ABOVE,
    environment: -1,
    envRotation: 2.85,
    hdr: true,
    density: 1,
    smoothness: 0,
  },
}

// Merge user params over the material preset — the ONLY place where semantic
// params are flattened for the native layer.
export function resolveOrbParams(
  material: MaterialName,
  params?: OrbParams
): ResolvedOrbParams {
  const base = MATERIAL_PRESETS[material]
  if (params === undefined) {
    return base
  }
  return {
    speed: params.speed ?? base.speed,
    morph: params.morph ?? base.morph,
    orbit: params.orbit ?? base.orbit,
    pattern: params.pattern ?? base.pattern,
    patternScale: params.patternScale ?? base.patternScale,
    patternDistortion: params.patternDistortion ?? base.patternDistortion,
    tint: params.tint ?? base.tint,
    opacity: params.opacity ?? base.opacity,
    lightAzimuth: params.light?.azimuth ?? base.lightAzimuth,
    lightElevation: params.light?.elevation ?? base.lightElevation,
    environment: params.environment ?? base.environment,
    envRotation: params.envRotation ?? base.envRotation,
    hdr: params.hdr ?? base.hdr,
    density: params.density ?? base.density,
    smoothness: params.smoothness ?? base.smoothness,
  }
}
