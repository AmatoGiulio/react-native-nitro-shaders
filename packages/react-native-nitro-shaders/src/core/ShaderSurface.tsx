import React from 'react'
import { getHostComponent } from 'react-native-nitro-modules'
import NitroShadersConfig from '../../nitrogen/generated/shared/json/NitroShadersConfig.json'
import type {
  NitroShadersMethods,
  NitroShadersProps,
} from '../specs/nitro-shaders.nitro'

export type ShaderSurfaceProps = NitroShadersProps

export const NitroShaders = getHostComponent<NitroShadersProps, NitroShadersMethods>(
  'NitroShaders',
  () => NitroShadersConfig
)

export function ShaderSurface(props: ShaderSurfaceProps) {
  return <NitroShaders {...props} />
}
