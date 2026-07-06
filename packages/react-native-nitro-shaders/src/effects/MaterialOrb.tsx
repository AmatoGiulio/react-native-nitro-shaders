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
      repetition={environment !== undefined ? environment + 100 : undefined}
      // Pseudo-HDR toggle rides the legacy `grain` slot (same declared debt).
      grain={hdr ? 1 : 0}
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
