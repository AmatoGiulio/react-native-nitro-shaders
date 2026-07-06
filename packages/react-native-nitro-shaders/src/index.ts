import type { HybridRef } from 'react-native-nitro-modules'
import type {
  NitroShadersProps,
  NitroShadersMethods,
} from './specs/nitro-shaders.nitro'

export type NitroShadersRef = HybridRef<NitroShadersProps, NitroShadersMethods>
export { NitroShaders, ShaderSurface, type ShaderSurfaceProps } from './core/ShaderSurface'
export { MaterialOrb, type MaterialOrbProps } from './effects/MaterialOrb'
export type { MaterialOrbMaterial } from './materials/material-orb'
export { ORB_MATERIALS, MATERIAL_ORB_PRESETS } from './materials/material-orb'

// Material × Motion × Skin architecture (see docs/architecture/material-motion-skin.md).
export type { MaterialName } from './materials/catalog'
export type { Motion, MotionType } from './motions'
