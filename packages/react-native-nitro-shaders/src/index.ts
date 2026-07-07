import type { HybridRef } from 'react-native-nitro-modules'
import type {
  NitroShadersProps,
  NitroShadersMethods,
} from './specs/nitro-shaders.nitro'

export type NitroShadersRef = HybridRef<NitroShadersProps, NitroShadersMethods>
export { NitroShaders, ShaderSurface, type ShaderSurfaceProps } from './core/ShaderSurface'
export { MaterialOrb, type MaterialOrbProps } from './effects/MaterialOrb'

// Material × Motion × Skin architecture (see docs/architecture/material-motion-skin.md).
export type { MaterialName } from './materials/catalog'
export { MATERIAL_NAMES } from './materials/catalog'
export {
  MATERIAL_PRESETS,
  resolveOrbParams,
  type OrbParams,
  type OrbPattern,
  type OrbLight,
  type ResolvedOrbParams,
} from './materials/params'
export type { Motion, MotionType } from './motions'
