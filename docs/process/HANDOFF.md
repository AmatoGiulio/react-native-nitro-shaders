# HANDOFF

Snapshot dello stato corrente. SOVRASCRITTO ad ogni fine sessione (lo storico è in
`CHANGELOG.md`; il percorso degli orb in `ORB_MATERIALS_JOURNEY.md`; il piano in
`OPERATIONAL-PLAN.md`).

## Regole vincolanti
1. Gli agenti NON usano Argent/emulatori/simulatori/Metro (CLAUDE.md). Implementano,
   fanno `bun run typecheck`, `bun test`, `./gradlew :app:assembleDebug`
   (serve `export ANDROID_HOME=$HOME/Library/Android/sdk`). Per iOS:
   `cd apps/example/ios && LANG=en_US.UTF-8 pod install` + `xcodebuild` (vedi sotto).
   La validazione visiva su device la fa SOLO Giulio.
2. Architettura pubblica: `docs/architecture/material-motion-skin.md`. Reference
   tecnico orb: `docs/engineering/orb-materials.md`. Piano: `docs/process/OPERATIONAL-PLAN.md`.
3. Lezione di pipeline: il 90% del look di un material riflettente È l'environment.
   Prima la ground truth Blender (`tools/blender/ground_truth.py`), poi il match.

## Stato attuale (2026-07-06 — GRANDE REFACTOR)
Decisione di Giulio: **4 material** — metal, water, iridescent, glass.
**aura e mercury RIMOSSI** ovunque (storia in git: ultima versione completa in dea7fe3).

### Architettura post-refactor
- **Spec Nitro semantico** (`src/specs/nitro-shaders.nitro.ts`): morti gli slot legacy
  (repetition/grain/intensity/softness/angle/orbMaterial/wobble/detail/materialColor…).
  Prop nuove: `material`, `speed`, `morph` (quanto la forma si trasforma su se stessa),
  `orbit` (rotazione 3D residua), `pattern` ('auto'|'folds'|'bands'|'ripples' —
  texture INTERCAMBIABILE a runtime, è solo un uniform), `patternScale`,
  `patternDistortion`, `tint`, `opacity`, `lightAzimuth`/`lightElevation` (posizione
  della key light), `environment` (indice lab, -1 = default del material),
  `envRotation`, `hdr` (bool), `density`/`smoothness` (water).
- **Registry parametri TS** (`src/materials/params.ts`): `OrbParams` semantici,
  `MATERIAL_PRESETS` (congelati: water e glass validati device 2026-07-06),
  `resolveOrbParams` = UNICO punto di mapping. `MaterialOrb` ora prende
  `material` + `params` + `motion`.
- **Shader split** (Android `android/src/main/assets/shaders/`):
  `orb-core.agsl` (uniform + noise lib + env + pattern library + silhouette funcs)
  + `material-{metal,water,iridescent,glass}.agsl` (solo `surfaceColor()`)
  + `orb-main.agsl` (entry). Il Kotlin concatena e cache-a un RuntimeShader per
  material. Pattern switch = cambio uniform, zero ricompilazioni.
- **Kotlin** (`ShaderMaterial.kt`): silhouette family (smooth = water/glass,
  pebble = metal/iridescent), density/shadow/env default per NOME material
  (water=sunset-sea `water-env.png`, glass=teal `glass-env.png`,
  metal/iridescent=`studio.png`). Uniform risolti nativamente (motion override).
  `opacity` agisce anche sull'ombra di contatto.
- **iOS Metal (Fase D, primo porting)**: struttura SPECULARE — `ios/Shaders/*.msl`
  (orb-core + 4 material + orb-main, port 1:1 della matematica AGSL), compilati a
  runtime (una pipeline per material, cache), env PNG nel resource bundle
  `NitroShaders` (podspec `resource_bundles`). Silhouette tagliata IN-SHADER
  (alpha), stesse formule del Path Android. MSL verificati con `xcrun metal`
  (4/4 compilano). **Manca l'ombra di contatto su iOS (debito dichiarato).**
  Swift riscritto sulle prop nuove; blending premultiplied, view trasparente.
- **Lab example**: slider semantici (Speed/Morph/Orbit/Scale/Distortion/Tint/
  Opacity/Light Az/Light El/Env Rot/Density/Smooth) + selettore pattern + env row
  + HDR + reset.
- ATTENZIONE codegen: NON eseguire `node post-script.js` dopo nitrogen — in questo
  repo la classe Kotlin sta in `com.margelo.nitro.nitroshaders` e il post-script
  (che riscrive gli import in `com.nitroshaders`) ROMPE la build. Solo `bunx nitrogen`.

### Preset congelati (validati da Giulio su device 2026-07-06)
- water: speed 2.0, morph 0.38, scale 0.19, distortion 0, tint 0.2, density 1.11,
  smoothness 0.6, hdr OFF, env sunset-sea.
- glass: speed 2.0, morph 0.4, scale 0.69, distortion 0.07, tint 0.5, envRot 2.85,
  hdr ON, env teal (ex lab-2).
- metal: look chrome validato (hdr OFF = bit-identico). iridescent: milestone IBL.

## Verifiche fatte (2026-07-06 sera)
- `bun run typecheck` OK, `bun test` 6/6 OK, `assembleDebug` Android OK.
- MSL: 4/4 compilano con `xcrun -sdk iphoneos metal`.
- `pod install` example OK (con `LANG=en_US.UTF-8`). xcodebuild example in corso a
  fine sessione — se fallisce, il probabile colpevole è HybridNitroShaders.swift.

## Prossimi passi (in ordine)
1. **Giulio**: rebuild Android → verificare che i 4 material siano identici a prima
   del refactor (water/glass congelati, metal/iridescent invariati) e provare i knob
   nuovi (Opacity, Orbit, Light, Pattern switch tra material).
2. **Giulio (Mac/Xcode)**: run dell'example su iPhone/simulatore — primo test
   assoluto dei material su iOS. Attesi: orb funzionanti senza ombra di contatto.
3. Ombra di contatto iOS (port della piramide di blur o approccio in-shader).
4. Se tutto regge: pulizia finale doc architettura (material-motion-skin.md va
   aggiornato allo spec semantico) + valutare rename `MaterialOrb` → skin `MaterialView`.

## Debito tecnico dichiarato
1. iOS: manca l'ombra di contatto; `ios/Shaders/shared-surface.metal` legacy inutilizzato.
2. Licenze env: lab-0 (GSG) e lab-1..6 (BlendKit) SOLO SVILUPPO; `glass-env.png`
   (=lab-2, 1.1MB) DA SOSTITUIRE/ottimizzare prima della pubblicazione;
   lab-12/water-env free (confermato Giulio); lab-13/14 da confermare.
   TODO: link BlenderKit in ASSET-LICENSES (li fornisce Giulio).
3. Env PNG duplicati in android/assets e ios/Assets (≈7MB per piattaforma) —
   ottimizzare/ridurre prima della pubblicazione.
4. `docs/architecture/material-motion-skin.md` e `docs/engineering/orb-materials.md`
   NON ancora aggiornati al refactor (parlano ancora dei vecchi prop/6 material).

## Blocchi aperti
- BLOCKED (Giulio): validazione visiva post-refactor su Android (parità con dea7fe3).
- BLOCKED (Giulio/Mac): primo run iOS su device/simulatore.
- (Se ripreso in futuro) animazione symbiote: video del playback Blender.
