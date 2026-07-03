export type MaterialName =
  | 'fluidGradient'
  | 'liquidMetal'
  | 'metal'
  | 'water'
  | 'iridescent'

export const MATERIAL_NAMES: readonly MaterialName[] = [
  'fluidGradient',
  'liquidMetal',
  'metal',
  'water',
  'iridescent',
] as const
