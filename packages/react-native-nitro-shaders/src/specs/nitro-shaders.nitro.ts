import type {
  HybridView,
  HybridViewProps,
  HybridViewMethods,
} from 'react-native-nitro-modules'

export interface NitroShadersProps extends HybridViewProps {
  color: string
  animated: boolean
  paused: boolean
  debugTime: number
}

export interface NitroShadersMethods extends HybridViewMethods {}

export type NitroShaders = HybridView<NitroShadersProps, NitroShadersMethods, { ios: 'swift', android: 'kotlin' }>
