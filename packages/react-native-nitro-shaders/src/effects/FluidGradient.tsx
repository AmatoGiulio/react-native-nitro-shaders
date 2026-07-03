import React from 'react'
import { ShaderSurface, type ShaderSurfaceProps } from '../core/ShaderSurface'
import { FLUID_DEFAULTS } from '../materials/fluid'

export type FluidGradientProps = {
  colors: string[]
  scale?: number
  warp?: number
  grain?: number
  speed?: number
  intensity?: number
  animated?: boolean
  paused?: boolean
  debugTime?: number
  style?: ShaderSurfaceProps['style']
}

export function FluidGradient(props: FluidGradientProps) {
  const {
    colors,
    scale = FLUID_DEFAULTS.scale,
    warp = FLUID_DEFAULTS.warp,
    grain = FLUID_DEFAULTS.grain,
    speed = FLUID_DEFAULTS.speed,
    intensity = FLUID_DEFAULTS.intensity,
    animated = true,
    paused = false,
    debugTime = -1,
    style,
  } = props

  return (
    <ShaderSurface
      shader="fluidGradient"
      color="#000000"
      colors={colors}
      scale={scale}
      warp={warp}
      grain={grain}
      speed={speed}
      intensity={intensity}
      animated={animated}
      paused={paused}
      debugTime={debugTime}
      style={style}
    />
  )
}
