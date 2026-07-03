import React from 'react'
import { ShaderSurface, type ShaderSurfaceProps } from '../core/ShaderSurface'
import {
  MATERIAL_ORB_PRESETS,
  type MaterialOrbMaterial,
} from '../materials/material-orb'

export type MaterialOrbProps = {
  material?: MaterialOrbMaterial
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
  const {
    speed = preset.speed,
    wobble = preset.wobble,
    distortion = preset.distortion,
    detail = preset.detail,
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
      animated={animated}
      paused={paused}
      debugTime={debugTime}
      style={style}
    />
  )
}
