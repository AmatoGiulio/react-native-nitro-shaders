# HANDOFF

Snapshot dello stato corrente. SOVRASCRITTO ad ogni fine sessione (lo storico è in
`CHANGELOG.md`; il percorso degli orb in `ORB_MATERIALS_JOURNEY.md`; il piano in
`OPERATIONAL-PLAN.md`).

## Regole vincolanti
1. Gli agenti NON usano Argent/emulatori/simulatori/Metro (CLAUDE.md). Implementano,
   fanno `bun run typecheck`, `bun test`, `./gradlew :app:assembleDebug`
   (serve `export ANDROID_HOME=$HOME/Library/Android/sdk`). La validazione visiva su
   device la fa SOLO Giulio.
2. Architettura pubblica: `docs/architecture/material-motion-skin.md` (Material ×
   Motion × Skin). Reference tecnico orb: `docs/engineering/orb-materials.md`.
   Condivisione cross-platform: `docs/engineering/cross-platform-shaders.md`.
   Piano corrente: `docs/process/OPERATIONAL-PLAN.md`. Mappa doc: `docs/README.md`.

## Stato attuale (2026-07-04)
Engine orb = **mini-PBR + IBL** (riflette l'HDRI `assets/env/studio.png`) su sfera
3D viva (noise 3D + rotazione). Material attuali: **`metal`, `water`, `iridescent`,
`aura`** (4). Componente demo `MaterialOrb` (legacy) via registry.

- **Fase 0 COMPLETATA + validata**: refactor SOLID. Kotlin `ShaderMaterial.kt`
  (Strategy + registry al posto degli `if shader==`); TS `ORB_MATERIALS`
  (Open/Closed). Output invariato (parità confermata da Giulio). Commit `ecc18cc`.
- **Fase 1 COMPLETATA + validata**: rimossi `liquidMetal` (Paper) e `fluidGradient`
  (componenti/impl/asset/reference); aggiunto material **`aura`** (mode 3 emissivo
  neon). metal/water/iridescent invariati. Commit `251e757`.
- **Aura BlendKit target + motion video IMPLEMENTATA, da validare da Giulio**:
  target materiale/composizione = immagine BlendKit `thumbnail_6a19d5c6...webp`;
  video `gradient-1080p-6s-1783191770745.mp4` serve solo per il comportamento
  motion. Video analizzato localmente in `/tmp`: motion = drift continuo di
  masse grandi, non vortice/rumore fine. Implementazione nel ramo `mode 3` di
  `material-orb.agsl`: nucleo indaco (`blueCore`/`upperBlue`), lobi rosa
  (`leftPetal`/`rightPetal`/`lowerPetal`), velo magenta, rim ciano/verde
  basso+alto e magenta laterale; rimossa palette arancio/oro derivata dal video.
  Dopo feedback "sembra una biglia di vetro", il solo ramo `aura` non usa piu'
  modello glass/PBR: niente `refract`, HDRI/reflection o `depthShade` sul corpo.
  Il corpo e' colore organico matte/emissivo clipped dalla skin dell'orb; solo
  edge glow/membrana opaca usano `baseN` per mantenere la silhouette. Preset
  `aura`: `speed 0.4`, `wobble 0.22`, `distortion 0.18`, `detail 0.6`.
  Verifica meccanica verde: `bun run typecheck`, `bun test`,
  `./gradlew :app:assembleDebug`.

## Material definitivi (target 5)
`metal`, `water`, `iridescent`, `aura` (fatti), **`glass`** (Fase 2, ref
`references/materials/glass.webp` = chrome liquido scuro; la gemma rossa è artefatto).

## Prossimi passi (in ordine)
1. **Giulio valida `aura` su device**. Se non passa, iterare ancora solo sul ramo
   `mode 3` di `material-orb.agsl`; se passa, procedere a Fase 2.
2. **Fase 2** (`OPERATIONAL-PLAN.md`):
   a. **metal**: target confermato da Giulio = `references/materials/metal.png`
      + video J `references/materials/liquid-orb-metal-swiftui-reference.mp4`.
      `glass.webp` non si usa più come forma del metal. Rifinire forma/normal relief,
      roughness/IBL response e motion verso argento cromato liquido con grigi medi,
      riflessi più morbidi e rim iridescente sottile.
   b. **glass** (nuovo material, mode 4): chrome liquido scuro traslucido come
      `glass.webp` (riflessi viola/blu, accenti rossi, highlight bianchi).
   c. Poi rifinire `water`, `iridescent`, `aura`.

## Debito tecnico dichiarato
1. **Spec Nitro**: le prop di liquidMetal/fluid (shape, colorBack, repetition, scale,
   warp, grain, ...) restano nel `nitro-shaders.nitro.ts` INUTILIZZATE. Pulizia
   dedicata (rimuovere prop + rigenerare codegen) quando l'API material è stabile.
2. **iOS**: material orb assenti in Metal; Swift non implementa le prop orb né le
   `u_motion*`. Porting IBL + shared `.core` (vedi cross-platform-shaders.md).
3. **Rotazione orb per-asse** hardcoded nello shader (`t*0.30`/`t*0.42`) → parametro.
4. **HDRI** 382KB PNG → WebP/512×256 (~50-100KB) prima della pubblicazione.
5. `MaterialOrb` è legacy → `MaterialView` (skin) in un R2 successivo; le tre Skin
   (View/Text/Svg) non esistono ancora.

## Blocchi aperti
- BLOCKED (Giulio): validazione visiva su device (DoD di ogni step visuale).
- BLOCKED (Giulio/Mac): tutto iOS.
