# HANDOFF

Snapshot dello stato corrente. SOVRASCRITTO ad ogni fine sessione (lo storico è
in `CHANGELOG.md`; il percorso degli orb è in `ORB_MATERIALS_JOURNEY.md`).

## Regole vincolanti
1. Gli agenti NON usano Argent / emulatori / simulatori / Metro (vedi CLAUDE.md).
   Implementano, fanno `bun run typecheck`, `bun test`, `./gradlew assembleDebug`
   (serve `ANDROID_HOME`). La validazione visiva su device la fa solo Giulio.
2. Architettura pubblica: `docs/architecture/material-motion-skin.md`
   (Material × Motion × Skin). Reference tecnico orb: `docs/engineering/orb-materials.md`.
   Mappa doc: `docs/README.md`.

## Stato attuale — TRAGUARDO ORB RAGGIUNTO (Android)
I tre material orb (`metal`, `water`, `iridescent`) sono resi con un **mini-PBR +
IBL**: riflettono/rifrangono un HDRI di studio reale (`assets/env/studio.png`,
CC0 Poly Haven) e vivono in 3D (noise 3D sulla superficie sferica, rotazione su
assi Y+X, bulge radiale). Validato visivamente da Giulio (2026-07-04). Dettaglio
tecnico e trappole in `docs/engineering/orb-materials.md`; percorso in
`docs/process/ORB_MATERIALS_JOURNEY.md`.

Componente demo attuale: `MaterialOrb` (legacy) con material `metal`/`water`/
`iridescent`. Silhouette organica + ombra sono native (Kotlin). Solo Android; iOS
orb non implementato.

## Architettura decisa (Giulio)
- **Material** (5): `fluidGradient`, `liquidMetal`, `metal`, `water`, `iridescent`.
- **Motion** ibrido: `none`/`flow`/`wobble`/`loop`, default per-material, opzionale.
- **Skin** (target R2+): `MaterialView` / `MaterialText` / `MaterialSvg`. Oggi esiste
  solo `MaterialOrb` (demo legacy) da migrare a `MaterialView`.
- Struttura src target: `core/`, `materials/`, `motions/`, `skins/`.

## Contratto (R1, completata)
`src/materials/catalog.ts` (`MaterialName`), `src/motions/` (`Motion`,
`resolveMotion`, `MOTION_DEFAULTS`), spec Nitro con 7 uniform `u_motion*`, override
Kotlin storage-only, unit test `bun test` 6/6.

## Prossimi passi (in ordine)
1. **Parametrizzazione** — esporre i parametri configurabili come nel video ref:
   rotazione per-asse (orizzontale/verticale, ora hardcoded `t*0.30`/`t*0.42`),
   `roughness`, `transmission`, `ior`, intensità iridescenza. Un material = un set
   di parametri Principled → base per "importare" material (glTF/BlenderKit).
2. **Cleanup / R2** — migrare `MaterialOrb` → `MaterialView` (skin), foldare
   `FluidGradient`/`LiquidMetal` sotto la stessa API e rimuoverli, consumare
   `u_motion*` come unica fonte (oggi via `resolved(motion, legacy)`).
3. **iOS** — portare l'engine IBL in Metal (env come `MTLTexture`, refract/reflect,
   silhouette via stencil/Path). Oggi con `materialOrb` iOS non rende l'orb.
4. **Ottimizzazione asset** — HDRI 382KB PNG → WebP / 512×256 (~50-100KB).
5. **Rifiniture** — orientamento studio riflesso, contact shadow, bloom softbox.

## Debito tecnico
1. iOS: material orb assenti in Metal; `HybridNitroShaders.swift` non implementa
   nemmeno le prop orb esistenti né le 7 `u_motion*` (storage rimandato a iOS).
   Debito default library Metal (fragment inline vs file `.metal`).
2. Rename `packages/react-native-nitro-shaders` → `packages/nitro-shaders` (mai fatto).
3. `MaterialOrb` è legacy: da sostituire con `MaterialView` in R2.
4. Rotazione orb per-asse hardcoded nello shader (→ parametro).
5. Peso HDRI da ottimizzare.

## Blocchi aperti
- BLOCKED (Giulio): validazione visiva su device (DoD di ogni step visuale).
- BLOCKED (Giulio/Mac): tutto iOS (orb, LiquidMetal, Fase 1-2).
