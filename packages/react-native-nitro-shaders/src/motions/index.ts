import type { MaterialName } from '../materials/catalog'

export type MotionType = 'none' | 'flow' | 'wobble' | 'loop'

export type Motion =
  | MotionType
  | { type: 'none' }
  | { type: 'flow'; speed?: number; amplitude?: number; warp?: number; detail?: number; seed?: number }
  | { type: 'wobble'; speed?: number; amplitude?: number; warp?: number; detail?: number; seed?: number }
  | { type: 'loop'; speed?: number; amplitude?: number; period?: number; seed?: number }

// Values that are passed as uniforms to the native runtime. Do NOT rename the keys.
export type ResolvedMotion = {
  motionType: number // 0 none, 1 flow, 2 wobble, 3 loop
  motionSpeed: number
  motionAmp: number
  motionWarp: number
  motionDetail: number
  motionSeed: number
  motionPeriod: number
}

// Numeric mapping of motion types (mirrors the native uniform contract).
export const MOTION_TYPE_VALUES: Record<MotionType, number> = {
  none: 0,
  flow: 1,
  wobble: 2,
  loop: 3,
}

// Natural motion of each material.
export const MOTION_DEFAULTS: Record<MaterialName, ResolvedMotion> = {
  metal: {
    motionType: 2,
    motionSpeed: 1.0,
    motionAmp: 1.0,
    motionWarp: 0.77,
    motionDetail: 1.0,
    motionSeed: 0,
    motionPeriod: 0,
  },
  water: {
    motionType: 2,
    motionSpeed: 1.26,
    motionAmp: 0.6,
    motionWarp: 0.55,
    motionDetail: 1.1,
    motionSeed: 0,
    motionPeriod: 0,
  },
  iridescent: {
    motionType: 2,
    motionSpeed: 0.9,
    motionAmp: 0.4,
    motionWarp: 0.4,
    motionDetail: 1.0,
    motionSeed: 0,
    motionPeriod: 0,
  },
  aura: {
    motionType: 1,
    motionSpeed: 1.0,
    motionAmp: 0.6,
    motionWarp: 0.5,
    motionDetail: 1.2,
    motionSeed: 0,
    motionPeriod: 0,
  },
  mercury: {
    motionType: 2,
    motionSpeed: 0.4,
    motionAmp: 0.4,
    motionWarp: 0.5,
    motionDetail: 0.28,
    motionSeed: 0,
    motionPeriod: 0,
  },
  glass: {
    motionType: 2,
    motionSpeed: 0.4,
    motionAmp: 0.4,
    motionWarp: 0.5,
    motionDetail: 0.28,
    motionSeed: 0,
    motionPeriod: 0,
  },
}

export function resolveMotion(motion: Motion | undefined, material: MaterialName): ResolvedMotion {
  const base: ResolvedMotion = { ...MOTION_DEFAULTS[material] }

  if (motion === undefined) {
    return base
  }

  if (typeof motion === 'string') {
    base.motionType = MOTION_TYPE_VALUES[motion]
    return base
  }

  base.motionType = MOTION_TYPE_VALUES[motion.type]

  if (motion.type === 'none') {
    return base
  }

  if ('speed' in motion && motion.speed !== undefined) {
    base.motionSpeed = motion.speed
  }
  if ('amplitude' in motion && motion.amplitude !== undefined) {
    base.motionAmp = motion.amplitude
  }
  if ('warp' in motion && motion.warp !== undefined) {
    base.motionWarp = motion.warp
  }
  if ('detail' in motion && motion.detail !== undefined) {
    base.motionDetail = motion.detail
  }
  if ('seed' in motion && motion.seed !== undefined) {
    base.motionSeed = motion.seed
  }
  if ('period' in motion && motion.period !== undefined) {
    base.motionPeriod = motion.period
  }

  return base
}
