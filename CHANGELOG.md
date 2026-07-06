# CHANGELOG

Storico append-only delle sessioni. Ogni voce la scrive solo l'orchestratore a fine sessione.

## [2026-07-06 sera] — Fase materiali — water VALIDATO da Giulio e congelato come default
- **water validato su device** (screenshot Giulio): preset default congelato = speed 2.0,
  wobble 0.38, distortion 0.0, detail 0.19, color 0.2, density 1.11, smooth 0.6, HDR off.
- **Env default water = sunset sea**: `water-env.png` sostituito con la copia di `lab-12.png`
  (era qwantani_dawn_puresky). Licenza lab-12 confermata free da Giulio →
  water-env senza debito licenze (ASSET-LICENSES.md aggiornato).
- **Preset estesi**: `MaterialOrbPreset` ora supporta `density`/`smooth`/`hdr` opzionali;
  `MaterialOrb` li usa come default per-material (fallback 1.0 / 0.0 / true). Il lab
  (apps/example) legge i nuovi campi e sincronizza il checkbox HDR al cambio material e
  al "Reset to defaults".
- Typecheck package OK; typecheck example a parità di baseline (resta il solo TS2307
  preesistente sulla risoluzione del modulo workspace).
- Restano DA VALIDARE: glass e mercury.
- **DECISIONE: aura sostituita** dal look del materiale BlenderKit "Symbiote With
  Aura Power" (emissivo: Smoke interno + corpo Symbiote viola + rim glow verde; niente
  env). Nome pubblico `aura` invariato.
- **aura/symbiote IMPLEMENTATA** (ground truth = render dei 3 layer isolati forniti da
  Giulio): mode 3 riscritto in material-orb.agsl — Smoke fog lavanda (gfbm Scale 1.8,
  Detail 2.6) + corpo Symbiote (noise Scale 1.7, 1 ottava, Distortion 0.2: pozzi blu
  face-on, flussi rosa con frangia aqua, viola verso il bordo) + rim glow verde/ciano
  a chiazze animate; key light finta top-left (shading + speculare waxy). Silhouette
  liscia estesa a mode 3 (Kotlin smoothSilhouette ≥2.5 + skip rim-fold in AGSL).
  Rimossi il vecchio blocco plasma/capsule e capsuleMask. Knob live: distortion→
  distorsione noise, detail→scala, materialColor→copertura flussi rosa. Build example
  Android OK, typecheck OK. DA VALIDARE su device.
- **glass validato su device e congelato** (screenshot Giulio): preset default = speed 2.0,
  wobble 0.4, distortion 0.07, detail 0.69, color 0.5, envRot 2.85, HDR on. Env default
  `glass-env.png` sostituito con copia di `lab-2.png` (teal scuro; era wooden_studio_08).
  ⚠️ lab-2 = thumbnail BlendKit SOLO SVILUPPO, 1.1MB → da sostituire/ottimizzare prima
  della pubblicazione. Aggiunto `envRot` opzionale ai preset (usato da MaterialOrb e lab).
- **aura/symbiote v2** (feedback device: v1 "scadente, non si avvicina"): un solo campo
  di noise per pozzo blu + massa rosa + pelle (adiacenti come la ref), aqua ridotta a
  frangia sottile, lembo traslucido che schiarisce, pozzo concavo con labbro, sheen
  ceroso doppio. Preset aura: wobble 0.4, distortion 0.3, detail 0.5. DA RIVALIDARE.
- **aura/symbiote v3** (feedback v2: "bruttissimo" — solo pelle+vene+spec bianchi):
  causa = gfbm compresso in [0.35,0.65], soglie mai raggiunte e labbro-banda globale.
  Fix: contrast-normalize del campo, labbro solo sul bordo pozzo, spec satin.
  Animazione: boiling morph continuo (drift del dominio ≈ Velocity/W di Blender) +
  respiro delle soglie rosa/pozzo. DA RIVALIDARE.
- **aura/symbiote v4** (feedback v3: "2D, poca profondità, fuori contesto rispetto a
  water e metal"): masse colore campionate in un punto interno lungo il raggio
  rifratto (parallasse sotto pelle traslucida), coat lucido IBL dallo stesso env degli
  altri material (grazing + glint), attenuazione interna verso il lembo, masse più
  grandi. DA RIVALIDARE.

## [2026-07-06] — Fase materiali — glass nuovo, mercury rivisto, water→gel, env CC0
- **Fix build Android example**: namespace/applicationId allineati a `com.anonymous.example`
  (era `com.example` → `R`/`BuildConfig` unresolved); rimosso BOM e autolinking.json stantio.
- **Fix crash lab env**: `MaterialOrb` passava `repetition=undefined` (→ null su prop Nitro
  non-null) quando si tornava all'env "auto"; ora passa `0` (= env default del material).
- **glass (mode 5, nuovo)**: dispersion glass ispirato al BlenderKit "Dispersion glass shader"
  (EEVEE). Rifrazione env con dispersione RGB per-canale, superficie a bande (`glassBands`),
  silhouette liscia dedicata. Env di default `glass-env.png` = Poly Haven wooden_studio_08 (CC0).
  Ground truth documentata in `docs/references/blender-dispersion-glass.md`.
- **mercury**: rivisto per condividere forma + motion di glass (silhouette liscia a onde,
  relief `glassBands`); mantiene la resa specchio liquido lead-silver. Rimossa la vecchia
  silhouette pebble/l=2 per mercury.
- **water → gel (mode 1)**: ridisegnato sul "Gel shader" Blender di Giulio. Vetro trasparente
  IOR 1.473 + guscio glass IOR 1.8 ai bordi, tinta ciano tenue (Volume Absorption 0.4),
  superficie a onde fBM fedele alla Noise Texture (Scale 2, Detail 2, Distortion 5) via
  `waterRipples`, silhouette liscia. Movimento water invariato. Env di default `water-env.png`
  = Poly Haven qwantani_dawn_puresky (CC0).
- **Env HDRI aggiunti** (PNG equirect 1024×512): lab-7 studio, lab-8 mercury, lab-9 glass
  (wooden_studio_08 CC0), lab-10 hangar_interior (CC0), lab-11 qwantani_dawn_puresky (CC0),
  lab-12 sunset-sea, lab-13 calm-sea, lab-14 purple-haze (lab-12/13/14 asset BlendKit forniti
  da Giulio — SOLO SVILUPPO, licenza da verificare). "auto" ripristina il default per-material.
- **water finalizzato — pattern fedele alla Noise Texture reale**: dopo aver isolato la noise
  pura in Blender (plug → Surface), corretto il tipo di trama: **marmo morbido** (fBM Scale 2,
  Detail 2 = 2 ottave, Distortion 5, gradient/Perlin noise `gnoise`/`gfbm`), NON chop/vortici.
  Superficie quasi liscia (displacement 0.065) + gradiente verticale (marmo in alto, pancia
  liscia). Base Color = Mix(ciano #68F3FF, blu #7277FF) dalla noise; Mix Shader Glass1.8/
  Principled1.473 via Fresnel; Volume Absorption density 0.4 (Beer-Lambert).
- **Slider nativi + tuning live**: installato `@react-native-community/slider` (Expo SDK57),
  sostituito lo slider custom. Aggiunti 3 knob live riusando prop legacy dello spec (NO codegen):
  Density→`intensity`, Smooth→`softness`, Env Rot→`angle` (rotazione env). Uniform shader
  `u_intensity`/`u_softness`/`u_envRot`.
- **Tentato e rimandato**: env come sfondo dietro la sfera (composite "in scena") — la view
  nativa non è trasparente, va reso l'env come background NEL draw nativo. Rollback fatto.
- **Doc**: `ASSET-LICENSES.md` aggiornato; `blender-dispersion-glass.md` creato.
- Processo: implementazioni delegate a developer subagent + tuning diretto dell'orchestratore;
  svolta metodologica = isolare la Noise pura in Blender invece di indovinare il pattern a occhio.
- **DA VALIDARE da Giulio su device**: resa finale water/glass/mercury. HDR boost OFF per water/glass.

## [da compilare] — Setup — Creazione governance iniziale
- Creati CLAUDE.md, AGENTS.md, STACK.md, CHANGELOG.md, HANDOFF.md
- Nome pacchetto deciso: react-native-nitro-shaders
- Roadmap Fase 1-5 definita in STACK.md

## [2026-07-03] - Fase 1 - Shared ShaderSurface core
- Sostituito lo scaffold `isRed` nello spec Nitro con props core `color`, `animated`, `paused`, `debugTime`.
- Creato wrapper TypeScript `ShaderSurface` e demo minima in `apps/example/App.tsx`.
- Implementata shared surface iOS con `MTKView`, pipeline Metal minimale e colore solido driven da JS.
- Implementata shared surface Android con custom `View`, `RuntimeShader` API 33+ e fallback statico sotto API 33.
- Aggiunti shader reference `shared-surface.metal` e `shared-surface.agsl`.
- Verifica: `bun run typecheck` resta bloccato perché `nitrogen/generated` è ancora generato sul vecchio contratto `isRed`; serve rigenerare Nitrogen.

## [2026-07-03] - Fase 1 - Fix runtime Android + validazione emulatore
- Fix Invariant Violation "Tried to register two views with the same name NitroShaders": doppia chiamata a `getHostComponent` (in `index.ts` e `ShaderSurface.tsx`); registrazione ora solo nel core, `index.ts` ri-esporta.
- Fix rendering Android fullscreen: `canvas.drawPaint()` riempiva l'intero clip del device (clipChildren=false di RN); sostituito con `drawRect` sui bounds della view, sia path AGSL sia fallback.
- Codegen Nitrogen risulta già rigenerato sul contratto Fase 1; `bun run typecheck` passa.
- Verificato su emulatore Pixel 6a API 34: quadrato 220×220 colore solido via RuntimeShader, centrato, driven da JS.
- Scartato: nessuna modifica a spec/nitro.json necessaria.
- Aperto: validazione iOS richiede Mac (BLOCKED per Giulio, vedi HANDOFF).

## [2026-07-03] - Fase 2 - FluidGradient (MVP) — Android validato
- F2-D1 (Sonnet): spec Nitro esteso con `shader`, `colors[]`, `speed`, `intensity`, `scale`, `warp`, `grain`; codegen rigenerato; plumbing prop Kotlin/Swift; componente pubblico `FluidGradient` + `materials/fluid.ts`; demo aggiornata. Approvato con deviazione: `style` tipizzato via `ComponentProps` (doppia installazione react-native nel monorepo).
- F2-D2 (Opus): `fluid-gradient.agsl` (value noise, fBm 4 ottave, domain warp, palette 6 colori, dithering) caricato da assets; selezione material per nome in ShaderSurfaceView; fallback LinearGradient statico <API 33. Un rigetto in revisione: AGSL vieta indici dinamici su array uniform → palette riscritta branchless a indici costanti.
- F2-D3 (Opus): `fluid-gradient.metal` + pipeline cache per nome in Swift. Deviazione approvata: fragment compilato da stringa inline (come path esistente), file .metal come fonte di riferimento — wiring default library rimandato al Mac.
- Validato da Giulio su emulatore API 34: gradiente organico animato 220×220.
- Aggiunta reference visiva `docs/refs/smoke-shaders.png` (swoosh black chrome liquido) → input per Fase 3 LiquidChrome.
- Il core Fase 1 non ha richiesto modifiche strutturali per ospitare il primo material (validazione dell'architettura).
## [2026-07-03] - Fase 3 - LiquidMetal reference + API demo
- Acquisita reference Paper Design `liquid-metal` in `docs/refs/` con attribution Apache 2.0; decisione: usarla come riferimento/derivata dichiarata, non come apertura a image mask/logo upload nell'MVP.
- Worktree corrente contiene porting LiquidMetal in corso: `liquid-metal.agsl`, `liquid-metal.metal`, plumbing Kotlin/Swift, spec Nitro esteso e wrapper pubblico `LiquidMetal`.
- F3-D1 prima interpretazione: API TS `LiquidMetal` estesa con `variant?: 'silver' | 'aqua' | 'pearl'`; rigettata da Giulio perche' le tre ball della reference sono materiali diversi, non variant del LiquidMetal Paper.
- Decisione corretta: LiquidMetal Paper resta com'e'; le ball diventano una famiglia separata di material/orb da analizzare e implementare dopo, con nomi/API propri.
- Verifica: `bun run typecheck` alla root non esiste (`Script not found "typecheck"`); verifica corretta eseguita nel package `packages/react-native-nitro-shaders` con `bun run typecheck` verde (`tsc --noEmit`).
- F3-D1b (Developer): correttivo approvato. Rimossi `variant`, `LiquidMetalVariant` e `LIQUID_METAL_VARIANTS`; example riportato a demo singola `LiquidMetal` Paper con `shape="circle"`.
- F3-D2 (Developer): loop LiquidMetal `shape="circle"` reso piu' pulito introducendo `loopTravel = 3.0`, `phase`, `loopT`, crossfade del noise non periodico e `direction -= loopT` nei tre sorgenti AGSL/Metal/Swift inline. Daisy/metaballs lasciati fuori scope perche' hanno motion locale non periodico.
- Verifica F3-D2: `bun run typecheck` nel package verde. Serve ancora validazione visiva Android post-fix.
- Giulio ha validato LiquidMetal Paper su Android dopo il fix loop.
- Aggiunte reference piu' accurate in `docs/refs/materials/`; creata analisi `docs/process/MATERIALS_ANALYSIS.md`. Decisione: nuova famiglia separata `MaterialOrb`, non variant di `LiquidMetal`.

## [2026-07-03] - Fase 4 - MaterialOrb liquidChrome Android
- F4-D1: creato componente pubblico `MaterialOrb` separato da `LiquidMetal`, con `material="liquidChrome"` e props `speed`, `wobble`, `distortion`, `detail`, `materialColor`; demo example aggiornata.
- Android: aggiunto shader `material-orb.agsl`, selezione `shader == "materialOrb"` e uniform plumbing Kotlin; aggiornati generated minimi Nitro (`NitroShadersConfig.json`, generated Kotlin spec) per le nuove props.
- Validazione: `bun run typecheck` verde e `:app:assembleDebug` verde.
- Runtime Android: primo crash AGSL per helper `saturate` duplicato risolto rinominandolo `orbSaturate`; app avviata su emulatore e screenshot acquisiti.
- Rifinitura visuale: F4-D1b ha rimosso noise maculato; F4-D1c ha reintrodotto marbling chrome largo e morbido. Stato attuale: base visibile e stabile, ancora da iterare verso reference per piu' liquid blob e highlights organici.

## [2026-07-03] - Fase 4 - MaterialOrb completo (3 material, Android, da validare)
- F4-D2: riscritto `material-orb.agsl` con geometria orb condivisa (silhouette wobble + SDF + normali da fold field a bassa frequenza) e tre material mode su `u_orbMaterial`: 0 liquidChrome (base argento chiara, vene scure larghe, highlight ampi, tinte verde/viola solo al bordo — correzione del render troppo scuro/turbolento), 1 liquidGlass (gel azzurro lattiginoso, rim ciano, caustiche soffuse, spec piccolo, alpha leggermente translucida), 2 iridescentGlass (perla lattiginosa, bande arcobaleno vincolate al bordo via fresnel).
- TS: `MaterialOrbMaterial` esteso a `'liquidChrome' | 'liquidGlass' | 'iridescentGlass'`; aggiunto `MATERIAL_ORB_PRESETS` con i default per-material osservati dalla reference (MATERIALS_ANALYSIS); `MaterialOrb` ora risolve i default dal preset del material scelto.
- Demo example: tre orb impilati con label, come la reference overview.
- Nessuna modifica necessaria a Kotlin/nitro spec: plumbing uniform gia' completo da F4-D1.
- Verifica: rigenerato Nitrogen (gitignorato, mancava su questo Mac), `bun run typecheck` verde nel package; `:app:installDebug` verde (prebuild Expo eseguito, cartella `apps/example/android` generata localmente).
- Direttiva di Giulio recepita e scritta in CLAUDE.md: MAI usare Argent/emulatori/Metro in questo progetto; solo implementazione + typecheck + unit test. Validazione visiva solo di Giulio.
- Scartato: validazione visiva su emulatore da parte dell'agente (vietata dalla nuova direttiva).

## [2026-07-03] - Ri-architettura - Material × Motion × Skin
- Giulio ha ridefinito l'API: la libreria espone "materiali pelle viva" su tre assi ortogonali. Creato `docs/spec/ARCHITECTURE.md` come nuova fonte di verita' dell'API pubblica.
- **Material** (5, piatti, niente famiglie): `fluidGradient`, `liquidMetal` (Paper Apache 2.0), `metal`, `water`, `iridescent`. Lo shader del material calcola solo il colore della superficie, mai la forma.
- **Motion** = categoria di oggetti separata (prop `motion`), modello ibrido deciso da Giulio: tipi condivisi `none`/`flow`/`wobble`/`loop`, default e interpretazione per-material, opzionale e sovrascrivibile. Definite le uniform motion condivise da aggiungere allo spec Nitro.
- **Skin** = 3 componenti pubblici `MaterialView`/`MaterialText`/`MaterialSvg`. I material possono rivestire background, testo e path SVG. Metafora di Giulio (il vestito): skin = corpo, material = tessuto/tratti, motion = vestibilita'. Regola dura: silhouette nel layer nativo di disegno, non nello shader.
- Decisioni prese da Giulio: naming skin `MaterialView/Text/Svg`; alias `FluidGradient`/`LiquidMetal`/`MaterialOrb` RIMOSSI subito (nessun utente esterno); l'orb non e' piu' un material (silhouette fuori dallo shader). Decisione aperta: se `speed`/`warp` di fluidGradient migrano interamente nel Motion (da confermare in R1).
- Aggiornati: STACK.md (struttura src `core/materials/motions/skins`, naming, roadmap v2 R1..R6), CLAUDE.md (divieto Argent per gli agenti), MATERIALS_ANALYSIS.md (marcato "superato in parte"). Nuove memorie: `architecture-material-motion-skin`, `no-argent-orchestrator`.
- Nessun codice v2 ancora scritto: prossimo task R1 (contratto TS `Material`/`Motion` + uniform Nitro + codegen, DoD typecheck verde). Codice esistente (i vecchi componenti e material-orb) da migrare.

## [2026-07-03] - R1 - Contratto Material/Motion (2 dev in parallelo)
- Orchestratore ha splittato R1 in due stream su file disgiunti e delegato a due developer in parallelo (Dev A/Opus contratto TS, Dev B/Sonnet contratto nativo), poi integrato di persona.
- Dev A: `src/materials/catalog.ts` (`MaterialName` + `MATERIAL_NAMES`) e `src/motions/index.ts` (`MotionType`, `Motion`, `ResolvedMotion`, `MOTION_TYPE_VALUES`, `MOTION_DEFAULTS` per-material, `resolveMotion()`). Semantica ibrida: default per-material sovrascrivibili da `motion` (stringa = solo tipo, oggetto = merge parametri).
- Dev B: spec Nitro esteso (additivo) con 7 uniform motion condivise (`motionType/Speed/Amp/Warp/Detail/Seed/Period`); codegen Nitrogen rigenerato (propagato in 12 file generati); override Kotlin in `HybridNitroShaders.kt` che memorizzano il valore (nessun `setFloatUniform`, per non crashare AGSL non dichiaranti l'uniform).
- Swift BLOCKED (legittimo): `ios/HybridNitroShaders.swift` non implementa nemmeno le prop orb esistenti (orbMaterial/wobble/detail/materialColor assenti). Nessun pattern da replicare → storage motion lato Swift RIMANDATO a R5 (fase iOS, gia' BLOCKED per Mac). Registrato come debito iOS.
- Integrazione orchestratore: export pubblici in `index.ts` (`MaterialName`, `Motion`, `MotionType`); unit test funzionale `src/motions/index.test.ts` via `bun test` (runner integrato, zero nuove dipendenze).
- Verifica: `bun run typecheck` verde; `bun test` 6/6 pass (20 assert). DoD R1 raggiunta.
- Decisione aperta ancora da confermare in R2: se `speed`/`warp` di fluidGradient migrano interamente nel Motion.

## [2026-07-03] - Cleanup - Repo senza ambiguita' + ristrutturazione docs
- Direttiva Giulio (massima priorita'): eliminare file/doc obsoleti, zero ambiguita', docs con pattern ricorrente.
- Rimossi file spazzatura: debug PNG (`material-orb-*.png` root, `apps/material-orb-final-candidate.png`), root `index.ts` (bun-init "Hello via Bun"), root `README.md` bun-init (riscritto reale), campo `"module": "index.ts"` da package.json root.
- Eliminata documentazione obsoleta che contraddiceva la nuova architettura: `docs/spec/ARC_shader_native_spec.md` (vecchio spec: naming/API/roadmap superati) e `docs/process/MATERIALS_ANALYSIS.md`. La matematica shader ancora valida dell'ARC spec e' stata DISTILLATA in `docs/engineering/shader-techniques.md`.
- docs/ ristrutturata con pattern architecture/engineering/process/references + `docs/README.md` indice. `ARCHITECTURE.md` → `docs/architecture/material-motion-skin.md`.
- Consolidati i ref in `docs/references/`: `materials/` (screenshot target puliti di Giulio: metal/water/iridescent/fluidgradient/preview), `liquid-metal/` (Paper Design reference + ATTRIBUTION, preview con nomi puliti), `smoke-shaders.png`.
- Aggiornati tutti i riferimenti ai path vecchi in CLAUDE.md, AGENTS.md, STACK.md, HANDOFF.md, memorie e commenti nel codice; scan orfani = pulito.
- Verifica: `bun run typecheck` verde, `bun test` 6/6. Nessuna regressione.

## [2026-07-04] - Reference audit - Liquid orb video + screenshot comparison
- Letti `CLAUDE.md`, `AGENTS.md`, `docs/README.md`, `docs/process/STACK.md`, `docs/process/HANDOFF.md`, `docs/architecture/material-motion-skin.md`, `docs/engineering/orb-materials.md` e `docs/engineering/shader-techniques.md`.
- Aggiunta reference video caricata da Giulio in `docs/references/materials/liquid-orb-metal-swiftui-reference.mp4`.
- Confrontato lo screenshot corrente di Giulio con `docs/references/materials/metal.png`, `water.png`, `iridescent.png`: target confermato come sfera sospesa con ombra morbida, bordo organico pulito, illuminazione studio e material naming v2 (`metal`/`water`/`iridescent`).
- Analisi: lo screenshot corrente resta utile come stato intermedio, ma non soddisfa ancora le reference per artefatti neri sul bordo, assenza di ombra di contatto, chrome troppo blu/meno metallico, water troppo scuro/saturo e iridescent troppo bianco con bande solo sottili al rim.
- Nessun codice di produzione modificato; nessuna validazione device eseguita dagli agenti.

## [2026-07-04] - Pipeline audit - Video analysis tooling + R2 diagnosis
- Installato tooling locale di sistema `ffmpeg`/`ffprobe` via Homebrew per analizzare la reference video senza aggiungere dipendenze al package.
- Estratti metadati video: H.264, 1080×1920, 60 fps, 18.7s, 1122 frame.
- Estratti frame e crop temporanei in `/tmp/rn-nitro-shaders-video-analysis/` per leggere motion, silhouette, shadow e differenze material; nessun derivato temporaneo aggiunto al repo.
- Ricerca tecnica: Android `RuntimeShader`/AGSL come brush nel Canvas drawing pipeline; SwiftUI/Metal reference simili usano pipeline componibile con shader color/layer/time, non un unico mega shader accoppiato a forma e UI.
- Diagnosi: la pipeline corrente `MaterialOrb` e' ancora legacy e accoppia material, silhouette e motion nello shader AGSL; per arrivare alla reference serve R2 con skin/orb demo nativa, ombra nativa separata, shader material puro e uniform `u_motion*`.

## [2026-07-04] - R2 visual pipeline - Native orb skin + pure material shader
- Commit baseline pulito creato prima dell'implementazione: `24d539f`.
- Android `MaterialOrb` demo migrato a pipeline piu' vicina a Material × Motion × Skin: shadow ellittica nativa + Path organico nativo; il Paint usa `RuntimeShader` solo per il colore del material.
- `material-orb.agsl` riscritto come shader material puro: niente silhouette, niente `alpha`/discard, output opaco dentro la skin; aggiunte uniform `u_motion*` e fallback legacy.
- `MaterialOrb.tsx` ora passa anche `motionType`, `motionSpeed`, `motionAmp`, `motionWarp`, `motionDetail`, `motionSeed`, `motionPeriod`; i preset legacy restano supportati per la demo.
- Preset orb allineati ai valori visibili nella reference video per water/iridescent e a un chrome piu' neutro.
- Verifica: `bun run typecheck` verde, `bun test` 6/6, `./gradlew :app:assembleDebug` verde. Nessuna validazione visuale device eseguita dagli agenti.

## [2026-07-04] - R2 visual tuning - Orb reference pass 2
- Ricevuto screenshot Giulio post split: miglioramento strutturale confermato, ma target ancora lontano.
- Corretto disallineamento principale tra Path nativo e shader: l'orb Path era centrato sopra al centro shader, causando rim/highlight fuori scala e bordo verde spesso su `iridescent`.
- Ridotto wobble nativo di default e avvicinata l'ombra alla reference con ellisse piu' larga e meno alta.
- Tuning AGSL: normal bump meno aggressivo; chrome meno nero/bruciato e piu' neutro; water piu' lattiginoso con rim/corpo gel; iridescent con thin-film meno saturo e highlight meno bruciati.
- Preset `liquidChrome` e `iridescentGlass` ridotti in wobble/distortion per evitare look planetario e bordo troppo spesso.
- Verifica: `bun run typecheck` verde, `bun test` 6/6, `./gradlew :app:assembleDebug` verde. AGSL resta da validare visualmente su device da Giulio.

## [2026-07-04] - R2 visual tuning - Reflection/membrane pass
- Ricevuto screenshot Giulio: ancora poca tridimensionalita' e assenza di riflessioni percepibili sui tre material.
- `material-orb.agsl`: sostituito il modello "noise come colore" con membrane normal + environment reflection procedurale.
- Aggiunti pannelli/softbox/ribbon riflessi (`studioEnv`) e perturbazione da onde acustiche (`acousticWave`) per simulare bolla viva senza gravita'.
- Introdotta densita' per material: `metal` piu' denso/rigido e speculare, `water` piu' morbido/caustico, `iridescent` intermedio/perlaceo.
- Kotlin Path: edge wobble ora usa la densita' del material, cosi' la skin non si muove uguale per tutti.
- Verifica: `bun run typecheck` verde, `bun test` 6/6, `./gradlew :app:assembleDebug` verde. Validazione visuale runtime resta a Giulio.

## [2026-07-04] - Orb materials - TRAGUARDO: mini-PBR + IBL + superficie 3D viva
- Svolta dopo ~10 iterazioni procedurali fallite: le reference riflettono un HDRI di studio reale, non un env procedurale. Passaggio a Image-Based Lighting.
- Analisi ref BlenderKit (clean water / liquid metal / soap bubble) + ricerca fattibilita' "import material Blender" (non fattibile 1:1: node tree, no export GLSL, glTF solo parametri PBR). Conclusione: replicare Eevee = PBR + IBL.
- `material-orb.agsl` riscritto come mini-Principled BSDF: campiona un HDRI equirettangolare (`assets/env/studio.png`, CC0 Poly Haven studio_small_08) via `uniform shader u_env` (BitmapShader + `setInputShader` in Kotlin). Material = preset PBR: metal (metallico), water (dielettrico + transmission/refract), iridescent (thin-film).
- Superficie 3D viva: rilievo con NOISE 3D sulla superficie sferica `(p,z)` ruotato nel tempo su assi Y+X (`rotate3`/`reliefRot`) + bulge radiale ("spinta da dentro"). Centra i 3 requisiti di Giulio (3D vera, forza da dentro, orbita in ogni direzione).
- Validato visivamente da Giulio su Android: metal/water/iridescent accettabili.
- Cleanup codebase: rinominati i material a `metal`/`water`/`iridescent` (via i vecchi `liquidChrome`/`liquidGlass`/`iridescentGlass`) in `material-orb.ts`, `MaterialOrb.tsx`, demo `App.tsx`; rimosso campo ridondante `materialName`/`MATERIAL_ORB_DEFAULTS`.
- Doc riallineate al traguardo: EDD `orb-materials.md` riscritto per IBL (rimosso l'env procedurale morto); nuovo `ORB_MATERIALS_JOURNEY.md` (post-mortem dei tentativi + cosa ha fatto il salto); HANDOFF ripulito a snapshot; README/indice aggiornati; memoria `reference-are-hdri-reflective`.
- Verifica: `bun run typecheck` verde, `bun test` 6/6, `./gradlew :app:assembleDebug` BUILD SUCCESSFUL.
- Prossimo: parametrizzazione (rotazione per-asse, roughness/transmission/ior), poi cleanup R2 (MaterialView) e iOS Metal.

## [2026-07-04] - Aura refine - Rim verde/magenta
- Ricevuto screenshot Giulio con `aura` ancora troppo magenta/cyan e senza separazione chiara del rim verde.
- Ritocco localizzato nel solo ramo `aura` (mode 3) di `material-orb.agsl`: ridotte leggermente le stream rosa, rim reso stabile su coordinate sferiche (`p.y`) invece della normale perturbata, aggiunto glow interno verde su basso/top e magenta sui lati.
- Nessuna modifica a metal/water/iridescent, Kotlin, TS, spec Nitro o demo.
- Verifica: `bun run typecheck` verde, `bun test` 6/6, `./gradlew :app:assembleDebug` BUILD SUCCESSFUL.
- Validazione visuale runtime resta a Giulio.

## [2026-07-04] - Aura refine - Gas lento e rim animato
- Feedback Giulio: movimento `aura` troppo nervoso; le macchie devono fondersi lentamente come gas densi; il neon verde al bordo deve ruotare/muoversi.
- Preset `aura` abbassato in `material-orb.ts`: speed/wobble/distortion/detail ridotti per rendere motion e silhouette piu' dolci.
- Branch `aura` in `material-orb.agsl`: dominio colore passato a noise largo e lento, stream rosa trasformate in glow morbido, rim verde calcolato in un dominio sferico rotante (`rimSp`) con variazione `rimFlow`.
- Nessuna modifica a metal/water/iridescent, Kotlin, spec Nitro o demo.
- Verifica: `bun run typecheck` verde, `bun test` 6/6, `./gradlew :app:assembleDebug` BUILD SUCCESSFUL.
- Validazione visuale runtime resta a Giulio.

## [2026-07-04] - Aura refine - Superficie perlata e gradienti
- Feedback Giulio: superficie ancora troppo ruvida; deve diventare piu' liscia/perlata, con macchie colore piu' gradienti come la ref.
- Preset `aura` ulteriormente ammorbidito in `material-orb.ts`: speed/wobble/distortion/detail ridotti.
- Branch `aura` in `material-orb.agsl`: normale visiva miscelata verso la normale sferica (`auraN`) per ridurre rilievo percepito; campi colore a frequenza ancora piu' bassa; transizioni indigo/violet/magenta/pink allargate; aggiunto velo `pearl` e bloom interno morbido al posto di vene nette.
- Nessuna modifica a metal/water/iridescent, Kotlin, spec Nitro o demo.
- Verifica: `bun run typecheck` verde, `bun test` 6/6, `./gradlew :app:assembleDebug` BUILD SUCCESSFUL.
- Validazione visuale runtime resta a Giulio.

## [2026-07-04] - Aura research reset - Procedural PBR gel
- Feedback Giulio: i pass precedenti erano fuori strada; richiesta ricerca online e reference BlenderKit specifica.
- Ricerca: asset BlendKit `Symbiote With Aura Power` = materiale procedurale sci-fi per Eevee/Cycles, tag Symbiote/Organic/Alien/Power/Energy; modello corretto = material PBR procedurale, non gradiente emissivo piatto. Riferimenti tecnici: Noise/ColorRamp per maschere larghe, Fresnel/Layer Weight per mixing/rim.
- Branch `aura` in `material-orb.agsl` riscritto come gel organico traslucido: normale smooth, riflessione/trasmissione HDRI (`sampleEnvSoft` + `refract`), lobi interni grandi tipo ColorRamp, thin-film leggero, rim emissivo verde/magenta animato via Fresnel.
- Preset `aura` resta lento/morbido in `material-orb.ts`.
- Nessuna modifica a metal/water/iridescent, Kotlin, spec Nitro o demo.
- Verifica: `bun run typecheck` verde, `bun test` 6/6, `./gradlew :app:assembleDebug` BUILD SUCCESSFUL.
- Validazione visuale runtime resta a Giulio.

## [2026-07-04] - Aura refine - Lobi reference-driven
- Feedback screenshot Giulio: la resa era ancora un gradiente sferico; mancavano i lobi riconoscibili della reference BlendKit.
- Branch `aura` in `material-orb.agsl`: maschere colore spostate su coordinate sferiche visibili (`p`) invece del dominio noise compresso; aggiunti tre lobi principali (foglia magenta alto-sinistra, massa rosa destra, massa bassa), valle blu centrale/ribbon, rim verde piu' forte top/basso, rim magenta laterale e due highlight perlati.
- Nessuna modifica a metal/water/iridescent, Kotlin, spec Nitro o demo.
- Verifica: `bun run typecheck` verde, `bun test` 6/6, `./gradlew :app:assembleDebug` BUILD SUCCESSFUL.
- Validazione visuale runtime resta a Giulio.

## [2026-07-04] - Aura refine - Asimmetria, profondita' e motion
- Feedback screenshot Giulio: resa ancora troppo simmetrica, quasi ferma, piatta e non percepita come 3D.
- Branch `aura` in `material-orb.agsl`: aggiunto helper `capsuleMask` e sostituite le ellissi statiche con maschere capsule organiche in dominio sferico 3D rotante (`wp/gp` + drift/fBm), cosi' foglia, massa destra, massa bassa, valle blu e highlight non restano ancorati a pattern speculari.
- Aumentata la lettura PBR/depth del gel: normale aura miscelata con la normale base per mantenere superficie liscia, ma riflessione/trasmissione HDRI piu' presenti, `depthShade` legato a `z`, thin-film sul bordo e rim verde/magenta animato via dominio `rimSp`.
- Preset `aura` in `material-orb.ts` ritoccato a `speed: 0.72`, `wobble: 0.28`, `distortion: 0.24`, `detail: 0.58` per rendere visibile il movimento senza tornare nervoso.
- Nessuna modifica a metal/water/iridescent, Kotlin, spec Nitro o demo.
- Verifica: `bun run typecheck` verde, `bun test` 6/6, `./gradlew :app:assembleDebug` BUILD SUCCESSFUL.
- Validazione visuale runtime resta a Giulio.

## [2026-07-04] - Aura refine - Identita' viva e meno bianco
- Feedback screenshot Giulio: molto meglio, ma i bordi avevano ancora troppo bianco e il materiale risultava finto, con colori troppo separati invece che in collisione organica.
- Branch `aura` in `material-orb.agsl`: riflesso HDRI reso piu' rough e tinto dal colore interno (`coloredRefl`) invece di sommare luce bianca pura; ridotta l'energia del rim e degli highlight speculari bianchi.
- Aggiunta maschera `collision` tra zone magenta e blu: dove i colori si incontrano vengono compressi/sporcati verso viola profondo, con leggera densita' scura, per dare una fusione piu' materica e meno digitale.
- Introdotto `greenBody` per far contaminare il corpo/rim con verde-ciano in modo piu' organico; ridotta saturazione/opacita' del blocco blu e del `pearlVeil`.
- Nessuna modifica a metal/water/iridescent, Kotlin, spec Nitro o demo.
- Verifica: `bun run typecheck` verde, `bun test` 6/6, `./gradlew :app:assembleDebug` BUILD SUCCESSFUL.
- Validazione visuale runtime resta a Giulio.

## [2026-07-04] - Aura refine - Specifica cellula/biglia liquida
- Giulio ha fornito analisi millimetrica della reference: sfera semi-trasparente tipo biglia/cellula, nucleo indaco profondo, due masse rosa fluide che orbitano/collidono, bordo Fresnel verde in basso e magenta sui lati/top, riflessi glaze controllati, doppia membrana sottile e glint centrale.
- Branch `aura` in `material-orb.agsl`: sostituita la distribuzione precedente con dominio `orbit` lento; introdotte due masse principali `leftMass`/`rightMass`, veli superiori/inferiori, nucleo `coreWell`, collisione magenta/blu e palette piu' profonda (`deepBlue` meno chiaro).
- Aggiunti `membraneInner`/`membraneOuter` per il doppio bordo trasparente, `coreGlow`/`coreGlint` per il piccolo punto centrale, e speculari glaze sottili tinti azzurro/rosa invece di bordi bianchi larghi.
- Nessuna modifica a metal/water/iridescent, Kotlin, spec Nitro o demo.
- Verifica: `bun run typecheck` verde, `bun test` 6/6, `./gradlew :app:assembleDebug` BUILD SUCCESSFUL.
- Validazione visuale runtime resta a Giulio.

## [2026-07-04] - Aura refine - Perlata opaca edge-lit
- Feedback Giulio: rimuovere la piccola lucina interna; i colori devono essere piu' opachi e illuminarsi soprattutto ai bordi; riflessioni perlate/opache, non lucide.
- Branch `aura` in `material-orb.agsl`: rimosso `coreGlint`, eliminate le speculari interne `leftSpec`/`rightSpec` e l'`envSpark` glossy; il core resta solo un bagliore blu molto attenuato.
- Riflesso HDRI reso ancora piu' rough (`sampleEnvSoft` piu' alto) e molto meno pesante nel mix; il corpo ora privilegia colore interno opaco e assorbimento, con damping centrale (`edgeLight`) e wash perlato solo sul bordo.
- Nessuna modifica a metal/water/iridescent, Kotlin, spec Nitro o demo.
- Verifica: `bun run typecheck` verde, `bun test` 6/6, `./gradlew :app:assembleDebug` BUILD SUCCESSFUL.
- Validazione visuale runtime resta a Giulio.

## [2026-07-04] - Aura refine - Plasma gradient composition
- Nuova reference Giulio: screenshot parametri Backgrounds Supply Gradient Lab, preset `Plasma`, 5 colori, zoom 1.40, complexity 3.00, smoothness 0.60, speed 0.40, brightness 1.00, contrast 1.05, saturation 1.10, grain 0.015; video allegato `gradient-1080p-6s-1783191770745.mp4`.
- Analizzato il video localmente in `/tmp` (non copiato nel repo: 20 MB): 1920×1080, 6.125s, 240 fps. Estratti 13 frame a step 0.5s; motion osservato = drift continuo di masse grandi, non vortice/rumore fine. L'indigo trasla da destra/alto verso sinistra/centro e ritorna; il caldo cresce/respira; il magenta fa da ponte morbido.
- Branch `aura` in `material-orb.agsl`: composizione riscritta verso plasma opaco a bassissima frequenza: `indigoCenter`/`indigoUpper`, `goldTop`/`goldBottom`, `coralBridge`, `magentaVeil`; palette indigo/violet/magenta/orange/gold; rimosso verde-ciano dominante residuo.
- Motion `aura`: preset TS portato a `speed: 0.4`, `wobble: 0.22`, `distortion: 0.18`, `detail: 0.6`; nel shader aumentato solo il drift largo (`slowT`, `drift`, `orbitA`, noise time) per seguire il video senza introdurre dettaglio nervoso.
- Verifica: `bun run typecheck` verde, `bun test` 6/6, `./gradlew :app:packageDebug --stacktrace` BUILD SUCCESSFUL dopo un primo fail transitorio di `:app:packageDebug` senza stacktrace utile; `./gradlew :app:assembleDebug` finale BUILD SUCCESSFUL.
- Validazione visuale runtime resta a Giulio.

## [2026-07-04] - Aura refine - Stop glass material model
- Feedback screenshot Giulio: composizione vicina, ma percezione materiale ancora sbagliata; l'aura sembrava una biglia di vetro.
- Causa individuata nel ramo `aura`: nonostante palette plasma, il codice usava ancora normal PBR, `refract`, `sampleEnvSoft`, `depthShade` da `z` e Fresnel fisico; questo reintroduceva una lettura vetrosa.
- Branch `aura` in `material-orb.agsl`: rimosso il modello glass/PBR dal solo mode 3. Il corpo ora e' colore plasma matte/emissivo (`internal`) clipped dalla skin dell'orb; niente refraction/HDRI/depth shading sul corpo. Restano solo edge glow e membrana opaca controllati da `baseN`/Fresnel leggero per mantenere la silhouette.
- Nessuna modifica a metal/water/iridescent, Kotlin, spec Nitro o demo.
- Verifica: `bun run typecheck` verde, `bun test` 6/6, `./gradlew :app:assembleDebug` BUILD SUCCESSFUL.
- Validazione visuale runtime resta a Giulio.

## [2026-07-04] - Aura refine - Target BlendKit + motion video
- Chiarimento Giulio: target materiale/composizione = immagine BlendKit `thumbnail_6a19d5c6...webp`; il video `gradient-1080p-6s...mp4` serve solo per il comportamento motion.
- Correzione: rimossa dalla `aura` la palette errata arancio/oro derivata dal video. Il ramo `mode 3` torna alla struttura target: nucleo indaco, massa blu superiore, lobo rosa alto-sinistra, lobo rosa destra, massa bassa, velo magenta, rim ciano/verde basso+alto e magenta laterale.
- Il motion largo/lento analizzato dal video resta applicato al dominio (`slowT`, `drift`, `orbitA`, noise time), ma non detta piu' colori o composizione.
- Nessuna modifica a metal/water/iridescent, Kotlin, spec Nitro o demo.
- Verifica: `bun run typecheck` verde, `bun test` 6/6, `./gradlew :app:assembleDebug` BUILD SUCCESSFUL.
- Validazione visuale runtime resta a Giulio.

## [2026-07-05] - Metal target clarification
- Giulio ha chiarito che `glass.webp` era stato considerato solo come possibile riferimento di forma per `metal`, ma la prova visiva non e' piaciuta.
- Target `metal` confermato: `docs/references/materials/metal.png` per forma/percezione del frame e `docs/references/materials/liquid-orb-metal-swiftui-reference.mp4` per motion/lettura dinamica. `glass.webp` resta solo target del material `glass`.
- Aggiornati `docs/process/OPERATIONAL-PLAN.md`, `docs/process/HANDOFF.md` e `docs/architecture/material-motion-skin.md` per rimuovere la direttiva "metal prende forma da glass".

## [2026-07-05] - Mercury + Env Lab - Nuovo material e laboratorio di tuning runtime
- Consolidamento metal committato (`d160578`): chrome validato, morph in place delle pieghe (Lissajous al posto della rotazione del dominio), rim-fold coupling (le intaccature della silhouette continuano dentro come pieghe), battito cardiaco sincrono Path+shader, ombra reale (silhouette proiettata con blur piramidale), sag gravitazionale.
- Nuovo material `mercury` (mode 4, quinto material): mercurio liquido denso — specchio nitido tinta piombo (F0 ~0.75), oscillazione di goccia l=2 prolato/oblato con asimmetria fisica, S-curve di contrasto dedicata. Registry Open/Closed rispettato (entry TS + ramo AGSL + density/ombra Kotlin).
- Diagnosi pipeline condivisa con Giulio: il 90% del look = environment riflesso; i render di riferimento usano env astratti ad alto contrasto (masse nere + fasci bianchi, es. GSG Pro Studios Metal), non stanze fotografiche; il PNG LDR clampa le luci a 1.0.
- Pseudo-HDR in `sampleEnv` (inverse tonemap, boost fino a 6x sopra lum 0.70), ora toggle runtime via prop `hdr` di MaterialOrb (uniform `u_hdrBoost` sullo slot legacy `grain`; hdr=false = look validato pre-boost).
- Laboratorio runtime nella demo: tabs per switchare i 5 material live (slider sincronizzati al preset), riga di anteprime per switchare 7 environment (`assets/env/lab-0..6.png`, indice sullo slot legacy `repetition` offset +100, prop `environment`), checkbox HDR.
- Ground truth Blender automatizzata: `tools/blender/ground_truth.py` (headless, sfera chrome + env → PNG); renderizzate le ground truth dei 7 env: candidati migliori lab-1 (pannelli luce su nero) e lab-5 (tunnel neon).
- Licenze: lab-0 deriva da preview pubblica GSG (DA SOSTITUIRE prima della pubblicazione, come da ASSET-LICENSES); lab-1..6 da thumbnail BlendKit (stessa nota).
- Verifica: `bun run typecheck` verde, `bun test` 6/6, `./gradlew :app:assembleDebug` verde.
- Aperto: validazione visiva di mercury e scelta env definitivo (Giulio, ora sul PC Windows con Blender per il lavoro sugli environment).
