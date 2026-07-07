import React from 'react'
import { ShaderSurface, type ShaderSurfaceProps } from '../core/ShaderSurface'
import type { MaterialName } from '../materials/catalog'
import { resolveOrbParams, type OrbParams } from '../materials/params'
import { resolveMotion, type Motion } from '../motions'

export type MaterialOrbProps = {
  material?: MaterialName
  /** semantic knobs, merged over the material preset (see OrbParams) */
  params?: OrbParams
  motion?: Motion
  animated?: boolean
  paused?: boolean
  debugTime?: number
  style?: ShaderSurfaceProps['style']
}

export function MaterialOrb(props: MaterialOrbProps) {
  const material = props.material ?? 'metal'
  const p = resolveOrbParams(material, props.params)
  const baseMotion = resolveMotion(props.motion, material)
  // With no explicit motion the orb follows the resolved semantic params
  // (single source of truth); an explicit motion overrides them.
  const motion =
    props.motion === undefined
      ? {
          ...baseMotion,
          motionSpeed: p.speed,
          motionAmp: p.morph,
          motionWarp: p.patternDistortion,
          motionDetail: p.patternScale,
        }
      : baseMotion

  return (
    <ShaderSurface
      shader="materialOrb"
      material={material}
      speed={p.speed}
      morph={p.morph}
      orbit={p.orbit}
      pattern={p.pattern}
      patternScale={p.patternScale}
      patternDistortion={p.patternDistortion}
      tint={p.tint}
      opacity={p.opacity}
      lightAzimuth={p.lightAzimuth}
      lightElevation={p.lightElevation}
      environment={p.environment}
      envRotation={p.envRotation}
      hdr={p.hdr}
      density={p.density}
      smoothness={p.smoothness}
      motionType={motion.motionType}
      motionSpeed={motion.motionSpeed}
      motionAmp={motion.motionAmp}
      motionWarp={motion.motionWarp}
      motionDetail={motion.motionDetail}
      motionSeed={motion.motionSeed}
      motionPeriod={motion.motionPeriod}
      animated={props.animated ?? true}
      paused={props.paused ?? false}
      debugTime={props.debugTime ?? -1}
      style={props.style}
    />
  )
}
