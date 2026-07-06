import React from 'react'
import { ShaderSurface, type ShaderSurfaceProps } from '../core/ShaderSurface'
import {
  ORB_MATERIALS,
  type MaterialOrbMaterial,
} from '../materials/material-orb'
import { resolveMotion, type Motion } from '../motions'

export type MaterialOrbProps = {
  material?: MaterialOrbMaterial
  motion?: Motion
  speed?: number
  wobble?: number
  distortion?: number
  detail?: number
  materialColor?: number
  /** water/gel body density (Volume Absorption strength). Rides the `intensity` slot. */
  density?: number
  /** water/gel smooth-patch amount (0 = mostly rippled, 1 = mostly smooth glass). Rides `softness`. */
  smooth?: number
  /** horizontal rotation of the environment in radians (move the HDRI). Rides `angle`. */
  envRot?: number
  /** Runtime environment override: index of assets env/lab-N.png. Omit for the material default. */
  environment?: number
  /** Pseudo-HDR highlight expansion on the reflected env (default true). */
  hdr?: boolean
  animated?: boolean
  paused?: boolean
  debugTime?: number
  style?: ShaderSurfaceProps['style']
}

export function MaterialOrb(props: MaterialOrbProps) {
  const material = props.material ?? 'metal'
  const definition = ORB_MATERIALS[material]
  const { preset } = definition
  const baseMotion = resolveMotion(props.motion, material)
  const resolvedMotion =
    props.motion === undefined
      ? {
          ...baseMotion,
          motionSpeed: preset.speed,
          motionAmp: preset.wobble,
          motionWarp: preset.distortion,
          motionDetail: preset.detail,
        }
      : baseMotion
  const {
    speed = resolvedMotion.motionSpeed,
    wobble = resolvedMotion.motionAmp,
    distortion = resolvedMotion.motionWarp,
    detail = resolvedMotion.motionDetail,
    materialColor = preset.materialColor,
    density = 1.0,
    smooth = 0.0,
    envRot = 0,
    environment,
    hdr = true,
    animated = true,
    paused = false,
    debugTime = -1,
    style,
  } = props

  return (
    <ShaderSurface
      shader="materialOrb"
      color="#000000"
      colors={[]}
      orbMaterial={definition.orbMaterial}
      speed={speed}
      wobble={wobble}
      distortion={distortion}
      detail={detail}
      materialColor={materialColor}
      // Lab env switch rides the legacy `repetition` slot (offset +100) until the
      // material API stabilizes — see HANDOFF declared debt.
      // 0 (not undefined) means "material default env": native reads labIndex =
      // repetition-100 < 0 → falls back to the per-material default. Passing undefined
      // sends null to the non-null Nitro prop and crashes.
      repetition={environment !== undefined ? environment + 100 : 0}
      // Pseudo-HDR toggle rides the legacy `grain` slot (same declared debt).
      grain={hdr ? 1 : 0}
      // water live-tuning on legacy liquidMetal slots: density→intensity, smooth→softness,
      // envRot→angle (env rotation).
      intensity={density}
      softness={smooth}
      angle={envRot}
      motionType={resolvedMotion.motionType}
      motionSpeed={speed}
      motionAmp={wobble}
      motionWarp={distortion}
      motionDetail={detail}
      motionSeed={resolvedMotion.motionSeed}
      motionPeriod={resolvedMotion.motionPeriod}
      animated={animated}
      paused={paused}
      debugTime={debugTime}
      style={style}
    />
  )
}
