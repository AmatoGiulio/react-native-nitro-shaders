// Public orb material names. These match the target architecture (MaterialName);
// mode index maps to u_orbMaterial in material-orb.agsl.
export type MaterialOrbMaterial = 'metal' | 'water' | 'iridescent' | 'aura' | 'mercury'

// Per-material parameter preset, observed from the references
// (docs/references/materials/ + the @jc_builds reference video).
type MaterialOrbPreset = {
  speed: number
  wobble: number
  distortion: number
  detail: number
  materialColor: number
}

// A material orb registry entry: the shader mode index plus its parameter preset.
export type OrbMaterialDefinition = {
  name: MaterialOrbMaterial
  orbMaterial: number
  preset: MaterialOrbPreset
}

// Material orb registry — Open/Closed by design: adding a new orb material
// (e.g. Phase 1 `aura` or Phase 2 `glass`) means adding ONE entry here, with
// no changes required to MaterialOrb.tsx or any other consumer:
//
//   glass: {
//     name: 'glass',
//     orbMaterial: 3,
//     preset: { speed: ..., wobble: ..., distortion: ..., detail: ..., materialColor: ... },
//   },
//
// and extending the `MaterialOrbMaterial` union above with the new name.
export const ORB_MATERIALS: Record<MaterialOrbMaterial, OrbMaterialDefinition> = {
  metal: {
    name: 'metal',
    orbMaterial: 0,
    preset: {
      speed: 0.9,
      wobble: 0.72,
      distortion: 0.56,
      detail: 1,
      materialColor: 0.42,
    },
  },
  water: {
    name: 'water',
    orbMaterial: 1,
    preset: {
      speed: 1.39,
      wobble: 0.6,
      distortion: 0.55,
      detail: 1.43,
      materialColor: 0.55,
    },
  },
  iridescent: {
    name: 'iridescent',
    orbMaterial: 2,
    preset: {
      speed: 0.9,
      wobble: 0.34,
      distortion: 0.28,
      detail: 1,
      materialColor: 1,
    },
  },
  aura: {
    name: 'aura',
    orbMaterial: 3,
    preset: {
      speed: 0.4,
      wobble: 0.22,
      distortion: 0.18,
      detail: 0.6,
      materialColor: 0.5,
    },
  },
  mercury: {
    name: 'mercury',
    orbMaterial: 4,
    preset: {
      speed: 0.5,
      wobble: 0.5,
      distortion: 0.42,
      detail: 0.5,
      materialColor: 0.5,
    },
  },
}

// Retro-compatible flattened preset map, derived from the registry, kept for
// any existing importer of the old shape (`{ orbMaterial, speed, wobble, ... }`).
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
> = Object.fromEntries(
  Object.entries(ORB_MATERIALS).map(([key, def]) => [
    key,
    { orbMaterial: def.orbMaterial, ...def.preset },
  ]),
) as Record<
  MaterialOrbMaterial,
  {
    orbMaterial: number
    speed: number
    wobble: number
    distortion: number
    detail: number
    materialColor: number
  }
>
