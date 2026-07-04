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
  - `aura`: prima versione OK (viola/magenta emissivo, flussi, core blu). Da
    rifinire: il **rim deve andare verde (basso) ↔ magenta (alto/lati)**, ora è
    cyan uniforme; manca il glow verde della ref `aura.webp`. Ritocco piccolo,
    localizzato nel ramo `mode 3` di `material-orb.agsl`.

## Material definitivi (target 5)
`metal`, `water`, `iridescent`, `aura` (fatti), **`glass`** (Fase 2, ref
`references/materials/glass.webp` = chrome liquido scuro; la gemma rossa è artefatto).

## Prossimi passi (in ordine)
1. **Rifinire `aura`**: rim verde↔magenta bipolare + un filo di bloom sul glow →
   verso `aura.webp`. Solo il ramo mode 3 di `material-orb.agsl`.
2. **Fase 2** (`OPERATIONAL-PLAN.md`):
   a. **Forma condivisa**: adottare la forma/moto organico a onde larghe di
      `glass.webp` come geometria/silhouette degli orb (Kotlin `buildMaterialOrbPath`
      + relief). Oggi la forma è un cerchio con wobble leggero.
   b. **metal**: prende la forma condivisa (di glass) con le proprietà material verso
      `references/materials/metal.png` (argento cromato, rim iridescente).
   c. **glass** (nuovo material, mode 4): chrome liquido scuro traslucido come
      `glass.webp` (riflessi viola/blu, accenti rossi, highlight bianchi).
   d. Poi rifinire `water`, `iridescent`, `aura`.

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
