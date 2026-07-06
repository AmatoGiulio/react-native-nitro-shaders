// Public orb material names. These match the target architecture (MaterialName);
// mode index maps to u_orbMaterial in material-orb.agsl.
export type MaterialOrbMaterial = 'metal' | 'water' | 'iridescent' | 'aura' | 'mercury' | 'glass'

// Per-material parameter preset, observed from the references
// (docs/references/materials/ + the @jc_builds reference video).
export type MaterialOrbPreset = {
  speed: number
  wobble: number
  distortion: number
  detail: number
  materialColor: number
  /** water/gel body density (Volume Absorption). Omit for the engine default (1.0). */
  density?: number
  /** water/gel smooth-patch amount. Omit for the engine default (0.0). */
  smooth?: number
  /** horizontal env rotation in radians. Omit for the engine default (0). */
  envRot?: number
  /** pseudo-HDR boost default for this material. Omit for the engine default (true). */
  hdr?: boolean
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
    // Live-tunable in the lab: wobble=wave height, distortion=flow, detail=frequency/
    // definition, materialColor=frost (0 clear glass → 1 frosted).
    // Values frozen from Giulio's on-device validation (2026-07-06, env sunset sea,
    // HDR boost off).
    preset: {
      speed: 2.0,
      wobble: 0.38,
      distortion: 0.0,
      detail: 0.19,
      materialColor: 0.2,
      density: 1.11,
      smooth: 0.6,
      hdr: false,
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
  // aura: symbiote look (BlenderKit "Symbiote With Aura Power"): emissive 3-layer —
  // lavender Smoke fog + purple Symbiote body (blue wells, pink flows) + green rim
  // glow. Live knobs: distortion→noise distortion, detail→noise scale, materialColor
  // →pink-flow coverage. Env-independent (fully emissive).
  aura: {
    name: 'aura',
    orbMaterial: 3,
    preset: {
      speed: 0.4,
      wobble: 0.4,
      distortion: 0.3,
      detail: 0.5,
      materialColor: 0.5,
    },
  },
  mercury: {
    name: 'mercury',
    orbMaterial: 4,
    // Shares glass's shape + motion (wave-band surface, smooth wave silhouette).
    preset: {
      speed: 0.4,
      wobble: 0.4,
      distortion: 0.5,
      detail: 0.28,
      materialColor: 0.5,
    },
  },
  // glass: BlenderKit "Dispersion glass shader" (EEVEE). Colorless glass refracting
  // the studio HDRI with chromatic dispersion; swirl from the perturbed normal.
  // See docs/references/blender-dispersion-glass.md. Preset maps: distortion→swirl
  // (Pattern distortion 20), detail→broad folds (Displacement, not fine), materialColor
  // →dispersion roughness (0.3).
  glass: {
    name: 'glass',
    orbMaterial: 5,
    // Values frozen from Giulio's on-device validation (2026-07-06, env lab-2 dark
    // teal, HDR boost ON). density/smooth are water-only knobs → not part of glass.
    preset: {
      speed: 2.0,
      wobble: 0.4,
      distortion: 0.07,
      detail: 0.69,
      materialColor: 0.5,
      envRot: 2.85,
      hdr: true,
    },
  },
}

// Retro-compatible flattened preset map, derived from the registry, kept for
// any existing importer of the old shape (`{ orbMaterial, speed, wobble, ... }`).
export const MATERIAL_ORB_PRESETS: Record<
  MaterialOrbMaterial,
  { orbMaterial: number } & MaterialOrbPreset
> = Object.fromEntries(
  Object.entries(ORB_MATERIALS).map(([key, def]) => [
    key,
    { orbMaterial: def.orbMaterial, ...def.preset },
  ]),
) as Record<MaterialOrbMaterial, { orbMaterial: number } & MaterialOrbPreset>
