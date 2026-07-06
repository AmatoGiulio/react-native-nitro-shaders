# HANDOFF

Snapshot dello stato corrente. SOVRASCRITTO ad ogni fine sessione (lo storico è in
`CHANGELOG.md`; il percorso degli orb in `ORB_MATERIALS_JOURNEY.md`; il piano in
`OPERATIONAL-PLAN.md`).

## Regole vincolanti
1. Gli agenti NON usano Argent/emulatori/simulatori/Metro (CLAUDE.md). Implementano,
   fanno `bun run typecheck`, `bun test`, `./gradlew :app:assembleDebug`
   (serve `export ANDROID_HOME=$HOME/Library/Android/sdk`). La validazione visiva su
   device la fa SOLO Giulio.
2. Architettura pubblica: `docs/architecture/material-motion-skin.md`. Reference
   tecnico orb: `docs/engineering/orb-materials.md`. Piano: `docs/process/OPERATIONAL-PLAN.md`.
3. Lezione di pipeline (2 giorni di iterazioni): il 90% del look di un material
   riflettente E' l'environment. Niente piu' tuning alla cieca: prima la ground truth
   Blender (`tools/blender/ground_truth.py`), poi il match dello shader.

## Stato attuale (2026-07-05)
Material: **metal, water, iridescent, aura, mercury** (5). Engine = mini-PBR + IBL
su sfera 3D viva; silhouette nativa (lobi 5/8/11 + dents + battito + sag), ombra
reale (silhouette proiettata, blur piramidale), pieghe che morphano in place,
rim-fold coupling bordo→superficie.

- **metal**: consolidato e committato (`d160578`). Con `hdr=false` e' bit-identico
  al look validato.
- **mercury**: nuovo (mode 4), mercurio liquido denso — specchio piombo, oscillazione
  l=2, env dedicato. DA VALIDARE da Giulio; env definitivo da scegliere.
- **aura**: implementata (sessioni precedenti), mai validata del tutto.
- **water/iridescent**: invariati dal traguardo IBL; risentono del pseudo-HDR se attivo.

### Laboratorio di tuning runtime (demo apps/example)
- Tabs per switchare i 5 material live (slider sincronizzati ai preset).
- Riga anteprime env: 7 environment switchabili a runtime (`assets/env/lab-0..6.png`).
- Checkbox HDR boost (pseudo-HDR inverse tonemap in `sampleEnv`).
- Plumbing temporaneo su slot legacy: `repetition` = envIndex+100, `grain` = hdr flag
  (documentato come debito, da migrare a prop vere quando l'API e' stabile).

### Ground truth Blender
- `tools/blender/ground_truth.py` (headless): sfera chrome Metallic 1/Roughness 0.05
  + env equirect → PNG. README in `tools/blender/`.
- Ground truth dei 7 lab env renderizzate: candidati migliori **lab-1** (pannelli
  luce su nero) e **lab-5** (tunnel neon); lab-2/3/4/6 scartati.

## Prossimi passi (in ordine)
1. **Giulio (PC Windows + Blender)**: esplorare environment per mercury/metal in GUI
   (procedura in `tools/blender/README.md`); ogni env promettente → PNG equirect
   1024×512, salvato come nuovo `lab-N.png` + preview in `apps/example/assets/envs/`
   + entry in `ENVS` (App.tsx). Commit e push.
2. Validazione mercury su device col laboratorio (material × env × HDR × slider);
   annotare le combinazioni vincenti.
3. Congelare i preset vincenti (env default per-material, hdr default) e replicare
   in parametrico gli env con licenza da sostituire (lab-0 GSG, lab-1..6 BlendKit).
4. Confronto A/B shader vs ground truth con `debugTime` (frame congelato) e correzione
   delle divergenze residue del BRDF.
5. Poi: pulizia debito (prop vere `environment`/`hdr` nello spec Nitro), R2 skin,
   iOS Metal (`cross-platform-shaders.md`).

## Debito tecnico dichiarato
1. Slot legacy riusati: `repetition` (envIndex+100), `grain` (hdr). Da migrare a prop
   dedicate nello spec Nitro con rigenerazione codegen.
2. Licenze env: lab-0 (preview GSG) e lab-1..6 (thumbnail BlendKit) SOLO SVILUPPO —
   da sostituire con env propri prima della pubblicazione. Vedi ASSET-LICENSES.md.
3. Spec Nitro: prop liquidMetal/fluid inutilizzate (pulizia rinviata).
4. iOS: material orb assenti in Metal; porting IBL + `.core` condiviso.
5. `MaterialOrb` e' legacy → `MaterialView` (skin) in un R2 successivo.
6. HDRI `studio.png` 382KB → ottimizzare prima della pubblicazione.

## Blocchi aperti
- BLOCKED (Giulio): validazione visiva mercury + scelta env definitivo.
- BLOCKED (Giulio/Mac): tutto iOS.
