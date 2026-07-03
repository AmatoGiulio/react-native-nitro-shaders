import React from 'react'
import { ShaderSurface, type ShaderSurfaceProps } from '../core/ShaderSurface'
import {
  MATERIAL_ORB_PRESETS,
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
  animated?: boolean
  paused?: boolean
  debugTime?: number
  style?: ShaderSurfaceProps['style']
}

export function MaterialOrb(props: MaterialOrbProps) {
  const material = props.material ?? 'liquidChrome'
  const preset = MATERIAL_ORB_PRESETS[material]
  const baseMotion = resolveMotion(props.motion, preset.materialName)
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
      orbMaterial={preset.orbMaterial}
      speed={speed}
      wobble={wobble}
      distortion={distortion}
      detail={detail}
      materialColor={materialColor}
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
