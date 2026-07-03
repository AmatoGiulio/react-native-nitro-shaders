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

export type ShaderSurfaceProps = NitroShadersProps &
  Pick<React.ComponentProps<typeof NitroShaders>, 'style'>

export function ShaderSurface(props: ShaderSurfaceProps) {
  return <NitroShaders {...props} />
}
