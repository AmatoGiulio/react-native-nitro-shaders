import React from 'react'
import { ShaderSurface, type ShaderSurfaceProps } from '../core/ShaderSurface'
import { LIQUID_METAL_DEFAULTS, type LiquidMetalShape } from '../materials/liquid-metal'

const LIQUID_METAL_BASE_DEFAULTS = {
  ...LIQUID_METAL_DEFAULTS,
  colorBack: '#00000000',
  colorTint: '#00000000',
  grain: 0,
}

export type LiquidMetalProps = {
  shape?: LiquidMetalShape
  colorBack?: string
  colorTint?: string
  repetition?: number
  softness?: number
  shiftRed?: number
  shiftBlue?: number
  distortion?: number
  contour?: number
  angle?: number
  speed?: number
  animated?: boolean
  paused?: boolean
  debugTime?: number
  grain?: number
  style?: ShaderSurfaceProps['style']
}

export function LiquidMetal(props: LiquidMetalProps) {
  const {
    shape = 'circle',
    colorBack = LIQUID_METAL_BASE_DEFAULTS.colorBack,
    colorTint = LIQUID_METAL_BASE_DEFAULTS.colorTint,
    repetition = LIQUID_METAL_BASE_DEFAULTS.repetition,
    softness = LIQUID_METAL_BASE_DEFAULTS.softness,
    shiftRed = LIQUID_METAL_BASE_DEFAULTS.shiftRed,
    shiftBlue = LIQUID_METAL_BASE_DEFAULTS.shiftBlue,
    distortion = LIQUID_METAL_BASE_DEFAULTS.distortion,
    contour = LIQUID_METAL_BASE_DEFAULTS.contour,
    angle = LIQUID_METAL_BASE_DEFAULTS.angle,
    speed = LIQUID_METAL_BASE_DEFAULTS.speed,
    animated = true,
    paused = false,
    debugTime = -1,
    grain = LIQUID_METAL_BASE_DEFAULTS.grain,
    style,
  } = props

  return (
    <ShaderSurface
      shader="liquidMetal"
      color="#000000"
      colors={[]}
      shape={shape}
      colorBack={colorBack}
      colorTint={colorTint}
      repetition={repetition}
      softness={softness}
      shiftRed={shiftRed}
      shiftBlue={shiftBlue}
      distortion={distortion}
      contour={contour}
      angle={angle}
      speed={speed}
      animated={animated}
      paused={paused}
      debugTime={debugTime}
      grain={grain}
      style={style}
    />
  )
}
