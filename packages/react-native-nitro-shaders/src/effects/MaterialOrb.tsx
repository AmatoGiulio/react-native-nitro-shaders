import React from 'react'
import { ShaderSurface, type ShaderSurfaceProps } from '../core/ShaderSurface'
import {
  MATERIAL_ORB_DEFAULTS,
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

function materialToFloat(material: MaterialOrbMaterial): number {
  if (material === 'liquidChrome') {
    return 0
  }

  return MATERIAL_ORB_DEFAULTS.orbMaterial
}

export function MaterialOrb(props: MaterialOrbProps) {
  const {
    material = 'liquidChrome',
    speed = MATERIAL_ORB_DEFAULTS.speed,
    wobble = MATERIAL_ORB_DEFAULTS.wobble,
    distortion = MATERIAL_ORB_DEFAULTS.distortion,
    detail = MATERIAL_ORB_DEFAULTS.detail,
    materialColor = MATERIAL_ORB_DEFAULTS.materialColor,
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
      orbMaterial={materialToFloat(material)}
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
