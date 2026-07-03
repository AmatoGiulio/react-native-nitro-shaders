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
  // motion (shared across materials): 0 none, 1 flow, 2 wobble, 3 loop
  motionType: number
  motionSpeed: number
  motionAmp: number
  motionWarp: number
  motionDetail: number
  motionSeed: number
  motionPeriod: number
}

export interface NitroShadersMethods extends HybridViewMethods {}

export type NitroShaders = HybridView<NitroShadersProps, NitroShadersMethods, { ios: 'swift', android: 'kotlin' }>
