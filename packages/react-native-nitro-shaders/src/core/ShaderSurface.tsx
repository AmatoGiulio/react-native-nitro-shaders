import React from 'react'
import { getHostComponent } from 'react-native-nitro-modules'
import NitroShadersConfig from '../../nitrogen/generated/shared/json/NitroShadersConfig.json'
import type {
  NitroShadersMethods,
  NitroShadersProps,
} from '../specs/nitro-shaders.nitro'

export const NitroShaders = getHostComponent<NitroShadersProps, NitroShadersMethods>(
  'NitroShaders',
  () => NitroShadersConfig
)

// Material defaults live on the native side; each effect passes only its own
// uniforms. `shader` stays required because it selects the material.
export type ShaderSurfaceProps = Partial<NitroShadersProps> &
  Pick<NitroShadersProps, 'shader'> &
  Pick<React.ComponentProps<typeof NitroShaders>, 'style'>

export function ShaderSurface(props: ShaderSurfaceProps) {
  // Omitted props fall back to the native-side defaults.
  return <NitroShaders {...(props as React.ComponentProps<typeof NitroShaders>)} />
}
