import { getHostComponent, type HybridRef } from 'react-native-nitro-modules'
import NitroShadersConfig from '../nitrogen/generated/shared/json/NitroShadersConfig.json'
import type {
  NitroShadersProps,
  NitroShadersMethods,
} from './specs/nitro-shaders.nitro'


export const NitroShaders = getHostComponent<NitroShadersProps, NitroShadersMethods>(
  'NitroShaders',
  () => NitroShadersConfig
)

export type NitroShadersRef = HybridRef<NitroShadersProps, NitroShadersMethods>
