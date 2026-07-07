import type {
  HybridView,
  HybridViewProps,
  HybridViewMethods,
} from 'react-native-nitro-modules'

export interface NitroShadersProps extends HybridViewProps {
  // ---- Surface selection + lifecycle ----
  shader: string // 'materialOrb' | 'solid'
  color: string // solid fill
  colors: string[] // reserved palette (max 6)
  animated: boolean
  paused: boolean
  debugTime: number

  // ---- Material orb: semantic parameter set ----
  // One meaning per knob across ALL materials (see src/materials/params.ts).
  material: string // 'metal' | 'water' | 'iridescent' | 'glass'
  speed: number // global time scale
  morph: number // shape self-transformation (silhouette swells + relief)
  orbit: number // residual 3D rotation of the pattern in space
  pattern: string // 'auto' | 'folds' | 'bands' | 'ripples'
  patternScale: number // texture frequency
  patternDistortion: number // texture domain warp
  tint: number // material color axis
  opacity: number // overall orb opacity (1 = opaque)
  lightAzimuth: number // key light horizontal angle (radians)
  lightElevation: number // key light vertical angle (radians)
  environment: number // env/lab-N.png index; -1 = material default
  envRotation: number // horizontal env rotation (radians)
  hdr: boolean // pseudo-HDR expansion of env lights
  density: number // water: volume absorption strength
  smoothness: number // water: smooth glassy patch amount

  // ---- Motion uniform contract (0 none, 1 flow, 2 wobble, 3 loop) ----
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
