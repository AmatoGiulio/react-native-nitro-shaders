# HANDOFF

Snapshot dello stato corrente. Questo file viene SOVRASCRITTO ad ogni fine sessione,
non e' uno storico (per quello c'e' CHANGELOG.md).

## Regole vincolanti nuove
1. Giulio ha vietato Argent MCP / emulatori / simulatori / Metro agli agenti
   (in CLAUDE.md). Gli agenti implementano, fanno typecheck e unit test; la
   validazione visiva su device la fa solo Giulio.
2. **Architettura pubblica: `docs/architecture/material-motion-skin.md`**
   (Material × Motion × Skin). E' la fonte di verita' dell'API. STACK.md la
   recepisce. Reference tecnico: `docs/engineering/shader-techniques.md`.
   Mappa doc: `docs/README.md`.
3. Repo ripulito: rimossi debug PNG, root index.ts/README bun-init, vecchio
   ARC spec (distillato in engineering) e MATERIALS_ANALYSIS (obsoleto). docs/
   ristrutturata in architecture/engineering/process/references.

## Stato attuale
Fase: architettura v2, **R1 COMPLETATA** (contratto TS + nativo). Il codice
esistente (FluidGradient, LiquidMetal, MaterialOrb, material-orb.agsl) e' ancora
quello vecchio e va migrato in R2. Il nuovo contratto convive additivo.

Ultima sessione (2026-07-04): audit reference richiesto da Giulio. Aggiunto il
video `docs/references/materials/liquid-orb-metal-swiftui-reference.mp4` alle
reference del progetto e confrontato lo screenshot corrente con le reference
`metal.png`, `water.png`, `iridescent.png`.

Tooling locale: installato `ffmpeg`/`ffprobe` via Homebrew per analisi video
reference. I frame/crop estratti vivono solo in
`/tmp/rn-nitro-shaders-video-analysis/` e non fanno parte del repo.

Implementazione successiva (2026-07-04): baseline committata (`24d539f`), poi
prima tranche R2 visual pipeline su Android:
- `MaterialOrb` resta una demo legacy, ma ora il rendering Android usa shadow
  ellittica nativa + Path organico nativo.
- `material-orb.agsl` e' material shader puro: non decide piu' silhouette/alpha.
- Lo shader orb consuma anche `u_motion*`; `MaterialOrb.tsx` passa motion uniform
  risolte accanto ai vecchi uniform per compatibilita' demo.
- Verifiche: `bun run typecheck`, `bun test`, `./gradlew :app:assembleDebug`.

## Architettura decisa (Giulio, 2026-07-03)
- **Material** (5, piatti): `fluidGradient`, `liquidMetal`, `metal`, `water`,
  `iridescent`. Lo shader calcola solo il colore, mai la forma.
- **Motion** ibrido: tipi condivisi `none`/`flow`/`wobble`/`loop`, default e
  interpretazione per-material, prop `motion` opzionale sovrascrivibile.
- **Skin** (3 componenti pubblici): `MaterialView`, `MaterialText`, `MaterialSvg`.
  Silhouette nel layer nativo di disegno, non nello shader (metafora del vestito).
- Alias `FluidGradient`/`LiquidMetal`/`MaterialOrb` RIMOSSI subito.
- Struttura src target: `core/`, `materials/`, `motions/`, `skins/`.

## Completato in questa sessione
- Scritto l'architettura in `docs/architecture/material-motion-skin.md` con
  decisioni chiuse + una decisione aperta (speed/warp di fluidGradient nel Motion).
- Aggiornati STACK.md (struttura src, naming, roadmap v2 R1..R6), CLAUDE.md
  (divieto Argent + nuovi path doc), AGENTS.md.
- Memorie: `no-argent-orchestrator`, `architecture-material-motion-skin`.
- (Precedente in questa sessione, ora da rifattorizzare in v2) shading dei tre
  material in `material-orb.agsl` + preset TS: base utile per lo shading dei
  material `metal`/`water`/`iridescent`, ma da spostare fuori dal contesto orb.

## R1 completata in questa sessione
- `src/materials/catalog.ts` (`MaterialName`, `MATERIAL_NAMES`).
- `src/motions/index.ts` (`Motion`, `MotionType`, `ResolvedMotion`,
  `MOTION_DEFAULTS`, `MOTION_TYPE_VALUES`, `resolveMotion`).
- Spec Nitro con 7 uniform motion (additive); codegen rigenerato; override Kotlin
  (storage-only). Export pubblici in `index.ts`. Unit test `bun test` 6/6.
- DoD raggiunta: typecheck verde + test verdi.

## Spec orb autoritativa
`docs/engineering/orb-materials.md` è l'EDD dei tre material orb (Giulio, 2026-07-03):
normale sferica analitica, env reflection procedurale, fresnel, specular, thin-film,
SSS, milestone F1-F4. Divergenze note dal codice (da chiudere): naming
liquidChrome→metal ecc.; silhouette in-shader → nativa (Fase 4); shader legge ancora
`u_speed/u_wobble/...` invece di `u_motion*`; iOS orb assente.
Decisione approccio (Giulio): si resta PROCEDURALI con environment-mapping
(matcap procedurale via `reflect(-V,N)` + studio virtuale in `envMap()`); matcap-PNG
scartato per ora. Shader = versione v3 di Giulio + fix scala uv/flip Y + rename var
`floor` (ombreggiava built-in SkSL).

Stato shading (round 4, direttiva Giulio): `material-orb.agsl` ora è uno shader PURO
(riempie il quad, `alpha=1`, nessuna silhouette dentro). La **silhouette è nativa**:
`HybridNitroShaders.kt` maschera con un `Path` circolare (AA hardware) invece di
`drawRect` — vincolo #1 EDD soddisfatto su Android. Fix inclusi: uv scalato a
`0.5*size` (la sfera riempie la maschera) + flip Y (luce dall'alto). I 3 material:
chrome ad alto contrasto con env animato, glass azzurro saturo + SSS rim, iridescent
thin-film concentrato al bordo. NON ancora validato visivamente.
Follow-up noti: la silhouette è un CERCHIO perfetto (il wobble organico e l'ombra di
contatto delle ref sono da reintrodurre come Path deformato + draw ombra nativa).
Decisione chiusa: `iridescent` = perla OPACA come la ref; EDD §3.1 allineato.

## Audit visuale reference (2026-07-04)
Screenshot corrente di Giulio:
- `liquidChrome`: base piu' vicina a vetro/blu che a cromo argento; manca env
  studio grigio/nero/bianco della reference `metal.png`; highlight superiore
  troppo bruciato e poco integrato; bordo con artefatti neri/clipping.
- `liquidGlass`: colore azzurro troppo scuro e saturo al centro rispetto a
  `water.png`; manca centro lattiginoso/traslucido con rim ciano morbido;
  speculare puntiforme presente ma non ancora "gel".
- `iridescentGlass`: centro quasi bianco uniforme; le bande arcobaleno sono
  sottili e confinate al bordo, mentre `iridescent.png` chiede perla opaca con
  bande piu' larghe, diffuse e leggibili anche verso la massa interna.
- Tutti e tre: manca l'ombra morbida sotto la sfera delle reference; la silhouette
  target e' organica ma pulita, non deve mostrare pixel/segmenti neri.

Implicazione per R2: prima di chiedere nuova validazione visiva a Giulio, il
developer deve trattare artefatti bordo + ombra nativa + env/lighting dei tre
material come criteri espliciti di accettazione della direttiva.

Analisi video reference:
- Video: H.264, 1080×1920, 60 fps, 18.7s. Reference UI: "Liquid Metal Shaders",
  tre personalita' dello stesso orb con slider live.
- La shadow e' sempre separata: ellisse grigia blur sotto l'orb, non prodotta dal
  fragment shader del material.
- La sfera e' piu' piccola della demo corrente, sospesa, con silhouette organica
  morbida ma pulita. Nessun segmento nero/pixel clipping al bordo.
- `metal`: argento neutro, bande scure larghe, white softbox leggibili, leggera
  contaminazione verde/viola solo al bordo.
- `water`: corpo blu lattiginoso, centro scuro ma non pieno, rim ciano luminoso,
  speculari bianchi piccoli; valori visibili nel video circa speed 1.39,
  wobble 0.60, distortion 0.55, detail 1.43, color 0.55.
- `iridescent`: perla opaca, centro crema/rosa/verde tenue, bande arcobaleno
  larghe e diffuse verso il bordo; valori visibili circa speed 0.90,
  distortion 0.40, detail 1.00, color alto.

Diagnosi pipeline:
- `MaterialOrb.tsx` passa ancora `shader="materialOrb"` e prop legacy
  `speed/wobble/distortion/detail/materialColor`.
- `material-orb.agsl` calcola ancora silhouette, alpha e material nello stesso
  fragment shader, invece del contratto v2 Material × Motion × Skin.
- `HybridNitroShaders.kt` disegna `materialOrb` con `drawRect` e commento legacy
  "shader defines its own organic silhouette via alpha"; quindi non esiste ancora
  lo stage nativo per orb path + shadow richiesto dalla reference.
- `u_motion*` esiste nel contratto R1 ma non guida ancora lo shader orb.

Strategia R2 raccomandata:
1. FATTO prima tranche demo Android: separare skin demo orb da material
   (`MaterialOrb` disegna shadow nativa + Path organico, poi Paint shaderizzato).
2. FATTO prima tranche demo Android: shader orb puro, alpha 1, niente clipping
   nero generato dal fragment shader.
3. FATTO per demo `MaterialOrb`: passaggio `u_motion*` accanto ai legacy uniform.
4. ANCORA DA VALIDARE/RIFINIRE su device: lighting finale `metal`, SSS/rim
   `water`, thin-film largo `iridescent` in base allo screenshot di Giulio.

## Prossimo task pianificato — R2 (Android materials piatti)
- `MaterialView` (skin background) come wrapper pubblico di ShaderSurface che
  accetta `material` + `motion` e passa gli uniform risolti (usa `resolveMotion`).
- Split di `material-orb.agsl`: shading puro dei material `metal`/`water`/
  `iridescent` SENZA silhouette (la forma la mette la skin/drawRect).
- Portare i 5 material su rettangolo; wiring uniform motion nei draw path Kotlin
  (ora gli AGSL devono DICHIARARE le uniform motion prima del setFloatUniform).
- Rimuovere i componenti/alias vecchi (`FluidGradient`/`LiquidMetal`/`MaterialOrb`)
  come da decisione, e i loro export.
- Confermare: `fluidGradient` speed/warp → Motion.
- DoD R2: validazione visiva Giulio dei 5 material su Android. Per i tre material
  orb, confronto obbligatorio contro `docs/references/materials/metal.png`,
  `water.png`, `iridescent.png` e
  `liquid-orb-metal-swiftui-reference.mp4`.

Prossimo task immediato:
- Giulio valida visivamente la nuova demo Android `MaterialOrb` su device.
- Se il bordo/ombra sono approvati, procedere allo split pubblico `MaterialView`
  e al rename API `liquidChrome/liquidGlass/iridescentGlass` →
  `metal/water/iridescent`.

## Debito tecnico
1. iOS: material non portati in Metal/Swift; debito default library Metal.
   NUOVO: `ios/HybridNitroShaders.swift` non implementa nemmeno le prop orb
   esistenti né le 7 motion (storage Swift rimandato a R5). Da sistemare quando
   si affronta iOS.
2. Rename `packages/react-native-nitro-shaders` -> `packages/nitro-shaders`.
3. Root `package.json` senza script `typecheck` (usare quello del package).
4. Nessun unit test nel repo: R1 e' l'occasione per un primo harness (es. mapping
   material/motion → uniform).
5. `material-orb.agsl` e `MaterialOrb.tsx`/`material-orb.ts` da smontare nella
   migrazione v2 (shading riusabile, silhouette e componente no).

## Blocchi aperti
- BLOCKED (Giulio): validazione visiva Android (DoD di ogni R2+).
- BLOCKED (Giulio/Mac): iOS in generale (R5) e validazione LiquidMetal/Fase 1-2.
