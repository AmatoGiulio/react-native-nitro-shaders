import type { HybridRef } from 'react-native-nitro-modules'
import type {
  NitroShadersProps,
  NitroShadersMethods,
} from './specs/nitro-shaders.nitro'

export type NitroShadersRef = HybridRef<NitroShadersProps, NitroShadersMethods>
export { NitroShaders, ShaderSurface, type ShaderSurfaceProps } from './core/ShaderSurface'
export { FluidGradient, type FluidGradientProps } from './effects/FluidGradient'
