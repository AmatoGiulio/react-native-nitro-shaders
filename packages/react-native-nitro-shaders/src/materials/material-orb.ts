export const MATERIAL_ORB_DEFAULTS = {
  orbMaterial: 0,
  materialName: 'metal',
  speed: 1,
  wobble: 1,
  distortion: 0.77,
  detail: 1,
  materialColor: 0.5,
}

export type MaterialOrbMaterial =
  | 'liquidChrome'
  | 'liquidGlass'
  | 'iridescentGlass'

// Default per material osservati dalle reference (docs/references/materials/).
export const MATERIAL_ORB_PRESETS: Record<
  MaterialOrbMaterial,
  {
    orbMaterial: number
    materialName: 'metal' | 'water' | 'iridescent'
    speed: number
    wobble: number
    distortion: number
    detail: number
    materialColor: number
  }
> = {
  liquidChrome: {
    orbMaterial: 0,
    materialName: 'metal',
    speed: 0.9,
    wobble: 0.72,
    distortion: 0.56,
    detail: 1,
    materialColor: 0.42,
  },
  liquidGlass: {
    orbMaterial: 1,
    materialName: 'water',
    speed: 1.39,
    wobble: 0.6,
    distortion: 0.55,
    detail: 1.43,
    materialColor: 0.55,
  },
  iridescentGlass: {
    orbMaterial: 2,
    materialName: 'iridescent',
    speed: 0.9,
    wobble: 0.34,
    distortion: 0.28,
    detail: 1,
    materialColor: 1,
  },
}
