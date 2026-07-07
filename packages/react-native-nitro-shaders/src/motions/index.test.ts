import { test, expect } from 'bun:test'
import { MOTION_DEFAULTS, MOTION_TYPE_VALUES, resolveMotion } from './index'

test('undefined motion falls back to the material natural motion', () => {
  expect(resolveMotion(undefined, 'metal')).toEqual(MOTION_DEFAULTS.metal)
  expect(resolveMotion(undefined, 'water')).toEqual(MOTION_DEFAULTS.water)
})

test('string motion only overrides the type, keeps material params', () => {
  const r = resolveMotion('flow', 'metal')
  expect(r.motionType).toBe(MOTION_TYPE_VALUES.flow)
  // params stay the metal defaults
  expect(r.motionWarp).toBe(MOTION_DEFAULTS.metal.motionWarp)
  expect(r.motionSpeed).toBe(MOTION_DEFAULTS.metal.motionSpeed)
})

test('object motion merges provided params over the material defaults', () => {
  const r = resolveMotion(
    { type: 'flow', speed: 2, warp: 0.1 },
    'iridescent',
  )
  expect(r.motionType).toBe(MOTION_TYPE_VALUES.flow)
  expect(r.motionSpeed).toBe(2)
  expect(r.motionWarp).toBe(0.1)
  // untouched fields remain the iridescent defaults
  expect(r.motionAmp).toBe(MOTION_DEFAULTS.iridescent.motionAmp)
  expect(r.motionDetail).toBe(MOTION_DEFAULTS.iridescent.motionDetail)
})

test('loop motion applies period', () => {
  const r = resolveMotion({ type: 'loop', period: 5, seed: 9 }, 'glass')
  expect(r.motionType).toBe(MOTION_TYPE_VALUES.loop)
  expect(r.motionPeriod).toBe(5)
  expect(r.motionSeed).toBe(9)
})

test('none motion sets type 0 and keeps base params', () => {
  const r = resolveMotion('none', 'water')
  expect(r.motionType).toBe(0)
  expect(r.motionSpeed).toBe(MOTION_DEFAULTS.water.motionSpeed)
})

test('every material has a default motion', () => {
  for (const name of ['metal', 'water', 'iridescent', 'glass'] as const) {
    expect(MOTION_DEFAULTS[name]).toBeDefined()
  }
})
