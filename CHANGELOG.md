# CHANGELOG

Storico append-only delle sessioni. Ogni voce la scrive solo l'orchestratore a fine sessione.

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
