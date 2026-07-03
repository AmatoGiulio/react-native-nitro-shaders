export const MATERIAL_ORB_DEFAULTS = {
  orbMaterial: 0,
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
    speed: number
    wobble: number
    distortion: number
    detail: number
    materialColor: number
  }
> = {
  liquidChrome: {
    orbMaterial: 0,
    speed: 1,
    wobble: 1,
    distortion: 0.77,
    detail: 1,
    materialColor: 0.5,
  },
  liquidGlass: {
    orbMaterial: 1,
    speed: 1.26,
    wobble: 0.6,
    distortion: 0.55,
    detail: 1.1,
    materialColor: 0.55,
  },
  iridescentGlass: {
    orbMaterial: 2,
    speed: 0.9,
    wobble: 0.4,
    distortion: 0.4,
    detail: 1,
    materialColor: 0.65,
  },
}
