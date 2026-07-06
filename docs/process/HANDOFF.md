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
- **water VALIDATO e CONGELATO da Giulio su device (2026-07-06 sera)**: preset default =
  speed 2.0, wobble 0.38, distortion 0.0, detail 0.19, color 0.2, density 1.11, smooth 0.6,
  HDR boost OFF, env default = sunset sea (ex lab-12, ora `water-env.png`). I preset ora
  supportano `density`/`smooth`/`hdr` opzionali (MaterialOrb li usa come default; il lab
  li legge e sincronizza anche il checkbox HDR al cambio material / reset).
  Licenza lab-12/water-env: free (confermata da Giulio 2026-07-06), nessun debito.
- **glass VALIDATO e CONGELATO da Giulio su device (2026-07-06 sera)**: preset default =
  speed 2.0, wobble 0.4, distortion 0.07, detail 0.69, color 0.5, envRot 2.85, HDR boost ON,
  env default = teal scuro (ex lab-2, ora `glass-env.png`). Aggiunto `envRot` opzionale ai
  preset (density/smooth sono knob solo-water, non congelati per glass).
  ⚠️ lab-2 è thumbnail BlendKit SOLO SVILUPPO (e pesa 1.1MB) → glass-env da sostituire/
  ottimizzare prima della pubblicazione.
- **mercury ancora DA VALIDARE da Giulio su device.**
- **DECISIONE (Giulio, 2026-07-06): aura sostituita** — nuovo target = materiale
  BlenderKit **"Symbiote With Aura Power"** (sfera viola traslucida, flussi rosa interni,
  rim glow verde). Il nome pubblico resta `aura` (API invariata), cambia il look.
  **IMPLEMENTATA** (stessa sessione, ground truth = render dei layer isolati forniti da
  Giulio): mode 3 riscritto in material-orb.agsl come 3 layer emissivi (Smoke fog
  lavanda gfbm 1.8/2.6 → corpo Symbiote con pozzi blu face-on + flussi rosa con
  frangia aqua, noise 1.7/1 ottava/distortion 0.2 → rim glow verde/ciano a chiazze
  animate). Key light top-left finta (shading volume + speculare waxy). Silhouette di
  aura ora liscia (smoothSilhouette esteso a mode 3 in Kotlin + skip rim-fold in AGSL).
  Rimosso il vecchio blocco plasma/capsule (e capsuleMask, ora inutilizzata).
  Build example OK, typecheck OK.
  **v2 dopo feedback device di Giulio ("scadente")** — correzioni strutturali: UN solo
  campo di noise per il corpo (basso→pozzo blu, alto→massa rosa, mezzo→pelle viola →
  pozzo e rosa ADIACENTI come la reference), aqua solo come frangia sottile (v1 aveva
  chiazze ciano enormi da due noise indipendenti), lembo che SCHIARISCE (guscio
  traslucido, v1 scuriva), pozzo concavo con labbro illuminato, sheen ceroso doppio
  (broad+tight) sul rilievo. Preset: wobble 0.4, distortion 0.3, detail 0.5.
  **v3 dopo secondo feedback device ("bruttissimo")** — la v2 su device mostrava solo
  pelle viola + vene scure + spec bianchi: il gfbm a 2 ottave sta quasi tutto in
  [0.35,0.65] e le soglie di rosa/pozzo non scattavano mai; il "labbro" era una banda
  globale (le vene). Fix v3: campo contrast-normalizzato ((F-0.5)*3.4+0.5), labbro
  confinato al bordo del pozzo, vene attenuate, spec satin (0.10/0.22). Animazione:
  ribollire continuo (drift costante del dominio noise ≈ Velocity/W Blender) + soglie
  di rosa/pozzo che respirano (le masse si aprono/chiudono).
  **v4 dopo terzo feedback ("2D, poca profondità, fuori contesto vs water/metal")** —
  cambio strutturale: (a) il campo colore è campionato in un punto INTERNO lungo il
  raggio rifratto (IOR 1.35, offset 0.45) → parallasse reale, le masse stanno sotto la
  pelle; (b) coat lucido che riflette lo STESSO env IBL degli altri material
  (sampleEnvSoft, riflessione al grazing + glint) → aura ancorata alla luce di scena;
  (c) interno che si spegne verso il lembo (path length) + velo lavanda fresnel;
  (d) masse più grandi (scale 0.85). **v4 DA VALIDARE.**
  Animazione: Giulio vuole replicare anche l'animazione originale Blender e poi
  modificarla — servono video del playback viewport e/o come è pilotato l'input
  Velocity (driver #frame su W 4D = morph in place, o traslazione Position = flusso
  direzionale).
  Struttura del node graph (interamente EMISSIVA, niente env → porting AGSL più facile):
  - Smoke (interno): noise emissivo Scale 1.8 / Detail 2.6 / Intensity 10.4 / Glow 3.1,
    mix con Transparent BSDF via Layer Weight Facing (Blend 0.05 e 0.3, ramp Pos 0.218/0.368).
  - Symbiote (corpo): noise animato Velocity 1.0 / Scale 1.7 / Detail 0.5 / Distortion 0.2,
    Color1 blu scuro / Color2 viola / Color2 violetto, facing Blend 0.25.
  - Aura Power (rim): NodeGroup emissivo Scale 1.7 / Detail 2 / Blend1 0.4 / Blend2 0.3 /
    Emission 1.0, gated da Layer Weight Blend 0.05 + ramp Ease Pos 0.514.
  Micro-rilievo superficie: riusare silhouette liscia + relief esistente.
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
1. **Giulio (device)**: validare mercury col laboratorio (material × env × HDR × slider).
   Annotare divergenze residue per asse così l'orchestratore corregge il singolo
   parametro. (water e glass: FATTI 2026-07-06, preset congelati.)
1-bis. **aura/symbiote**: implementata — validazione su device da parte di Giulio insieme
   a glass/mercury. Se servono correzioni fini, annotare per asse (colori / scala blob /
   quantità flussi rosa / intensità rim verde / lucentezza).
2. Congelare i preset vincenti restanti (env default: water=sunset-sea ✓, glass=teal
   lab-2 ✓, mercury=mercury-env da confermare) e l'hdr default per-material
   (water ✓ = off, glass ✓ = on).
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
   Inoltre: aggiungere i link alle pagine BlenderKit di lab-12/13/14 e water-env
   (licenza free già confermata; mancano i riferimenti — li fornisce Giulio).
3. Spec Nitro: prop liquidMetal/fluid inutilizzate (pulizia rinviata).
4. iOS: material orb assenti in Metal; porting IBL + `.core` condiviso.
5. `MaterialOrb` e' legacy → `MaterialView` (skin) in un R2 successivo.
6. HDRI `studio.png` 382KB → ottimizzare prima della pubblicazione.

## Blocchi aperti
- BLOCKED (Giulio): validazione visiva mercury su device + eventuali correzioni fini
  per asse. (water e glass: sbloccati e congelati 2026-07-06.)
- BLOCKED (Giulio): validazione visiva della nuova aura/symbiote su device
  (ground truth ricevuta e porting fatto il 2026-07-06).
- BLOCKED (Giulio/Mac): tutto iOS.
