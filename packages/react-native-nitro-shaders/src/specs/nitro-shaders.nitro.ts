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
  shader: string
  colors: string[]
  speed: number
  intensity: number
  scale: number
  warp: number
  grain: number
  // liquidMetal props:
  shape: string
  colorBack: string
  colorTint: string
  repetition: number
  softness: number
  shiftRed: number
  shiftBlue: number
  distortion: number
  contour: number
  angle: number
  orbMaterial: number
  wobble: number
  detail: number
  materialColor: number
}

export interface NitroShadersMethods extends HybridViewMethods {}

export type NitroShaders = HybridView<NitroShadersProps, NitroShadersMethods, { ios: 'swift', android: 'kotlin' }>
