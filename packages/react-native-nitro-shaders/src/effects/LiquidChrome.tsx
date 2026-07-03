import React from 'react'
import { ShaderSurface, type ShaderSurfaceProps } from '../core/ShaderSurface'
import { CHROME_DEFAULTS, type ChromeVariant } from '../materials/chrome'

export type LiquidChromeProps = {
  variant?: ChromeVariant
  scale?: number
  flow?: number
  distortion?: number
  contrast?: number
  highlightWidth?: number
  highlightIntensity?: number
  grain?: number
  speed?: number
  intensity?: number
  animated?: boolean
  paused?: boolean
  debugTime?: number
  style?: ShaderSurfaceProps['style']
}

export function LiquidChrome(props: LiquidChromeProps) {
  const {
    variant = 'silver',
    scale = CHROME_DEFAULTS.scale,
    flow = CHROME_DEFAULTS.flow,
    distortion = CHROME_DEFAULTS.distortion,
    contrast = CHROME_DEFAULTS.contrast,
    highlightWidth = CHROME_DEFAULTS.highlightWidth,
    highlightIntensity = CHROME_DEFAULTS.highlightIntensity,
    grain = CHROME_DEFAULTS.grain,
    speed = CHROME_DEFAULTS.speed,
    intensity = CHROME_DEFAULTS.intensity,
    animated = true,
    paused = false,
    debugTime = -1,
    style,
  } = props

  return (
    <ShaderSurface
      shader="liquidChrome"
      color="#000000"
      colors={[]}
      variant={variant}
      scale={scale}
      flow={flow}
      distortion={distortion}
      contrast={contrast}
      highlightWidth={highlightWidth}
      highlightIntensity={highlightIntensity}
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
