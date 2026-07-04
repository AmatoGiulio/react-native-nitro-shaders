// Public orb material names. These match the target architecture (MaterialName);
// mode index maps to u_orbMaterial in material-orb.agsl.
export type MaterialOrbMaterial = 'metal' | 'water' | 'iridescent'

// Per-material parameter presets, observed from the references
// (docs/references/materials/ + the @jc_builds reference video).
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
  metal: {
    orbMaterial: 0,
    speed: 0.9,
    wobble: 0.72,
    distortion: 0.56,
    detail: 1,
    materialColor: 0.42,
  },
  water: {
    orbMaterial: 1,
    speed: 1.39,
    wobble: 0.6,
    distortion: 0.55,
    detail: 1.43,
    materialColor: 0.55,
  },
  iridescent: {
    orbMaterial: 2,
    speed: 0.9,
    wobble: 0.34,
    distortion: 0.28,
    detail: 1,
    materialColor: 1,
  },
}
