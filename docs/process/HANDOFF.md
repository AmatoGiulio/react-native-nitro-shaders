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

## Stato attuale (2026-07-06)
Material: **metal, water, iridescent, aura, mercury, glass** (6). Novità sessione 2026-07-06:
- **glass** (mode 5): dispersion glass da BlenderKit "Dispersion glass shader" (EEVEE) —
  rifrazione env + dispersione RGB per-canale, superficie a bande (`glassBands`), silhouette
  liscia. Env default `glass-env.png` (wooden_studio_08, CC0). Ground truth:
  `docs/references/blender-dispersion-glass.md`.
- **mercury**: ora condivide forma+motion di glass (silhouette liscia, relief `glassBands`);
  resa specchio liquido invariata.
- **water → gel** (finalizzato): pattern **marmo morbido** fedele alla Noise Texture reale
  (fBM Scale 2, Detail 2 = 2 ottave, Distortion 5, gradient noise), superficie quasi liscia
  + gradiente verticale (pancia liscia). Base Color Mix(ciano/blu), Mix Shader Glass1.8/
  Principled1.473 via Fresnel, Volume Absorption 0.4. **Movimento water invariato**.
  Env default `water-env.png` (qwantani, CC0). Slider live: Density/Smooth/Env Rot.
  METODO: per il pattern, isolare la Noise pura in Blender (plug → Surface) e guardarla —
  NON indovinare a occhio dal render finale.
- **Lab UI**: slider nativi (`@react-native-community/slider`). 15 env (lab-0..14).
  Knob live via prop legacy riusati: Density=`intensity`, Smooth=`softness`, Env Rot=`angle`.
- **Fix**: crash env-"auto" (`repetition` null→0); build Android example (namespace).
- **TUTTI DA VALIDARE da Giulio su device** (glass/mercury/water); consigliato HDR boost OFF.
- Nota resa: forma/env liscia per water/mercury/glass NON usa più il rim-fold coupling
  (gated su `smoothSilhouette`); metal/iridescent/aura invariati.

### Snapshot precedente (2026-07-05)
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
- Tabs per switchare i 6 material live (slider sincronizzati ai preset).
- Riga anteprime env: 12 environment switchabili (`assets/env/lab-0..11.png`); lab-7..11 sono
  env HDRI Poly Haven CC0 (studio, mercury, glass=wooden_studio_08, hangar_interior,
  qwantani_dawn_puresky). "auto" ripristina l'env di default del material.
- Checkbox HDR boost (pseudo-HDR inverse tonemap in `sampleEnv`).
- Plumbing temporaneo su slot legacy: `repetition` = envIndex+100, `grain` = hdr flag
  (documentato come debito, da migrare a prop vere quando l'API e' stabile).

### Ground truth Blender
- `tools/blender/ground_truth.py` (headless): sfera chrome Metallic 1/Roughness 0.05
  + env equirect → PNG. README in `tools/blender/`.
- Ground truth dei 7 lab env renderizzate: candidati migliori **lab-1** (pannelli
  luce su nero) e **lab-5** (tunnel neon); lab-2/3/4/6 scartati.

## Prossimi passi (in ordine)
1. **Giulio (device)**: validare glass / mercury / water-gel col laboratorio (material × env ×
   HDR × slider), HDR boost OFF. Annotare divergenze residue per asse (colore / scala onde /
   contrasto / trasparenza) così l'orchestratore corregge il singolo parametro.
2. Congelare i preset vincenti (env default già impostati: water=qwantani, glass=wooden_studio,
   mercury=mercury-env) e l'hdr default per-material.
3. (era) esplorare altri env in Blender → export EXR/HDR → l'orchestratore converte con ffmpeg
   (tonemap Hable) e aggiunge come lab-N.
4. **Env come sfondo dietro la sfera** ("sphere in scene", tipo viewport Blender): rimandato.
   Va reso l'env full-frame NEL draw nativo (dietro il Path dell'orb), NON come Image RN
   (la view nativa non è trasparente → copre lo sfondo).

## Idee / feature future (progetti a parte)
- **Camera-reflection**: usare il feed della fotocamera come sorgente di `u_env` al posto
  dell'HDRI (toggle camera↔HDRI). Il materiale (colori/rifrazione) è indipendente dalla
  sorgente env: si cambia solo cosa riflette. Killer demo: **metal + front camera = specchio
  chrome che riflette il selfie con distorsioni** (metal è specchio puro, più facile di water).
  Costi: CameraX (Android)/AVCapture→Metal (iOS) nel modulo Nitro, permesso CAMERA, frame a
  bassa risoluzione per perf, mappatura frontale (non equirect). Feature strutturale → roadmap.
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
- BLOCKED (Giulio): validazione visiva glass / mercury / water-gel su device + eventuali
  correzioni fini per asse.
- BLOCKED (Giulio/Mac): tutto iOS.
