# Piano operativo (2026-07-04)

Piano deciso da Giulio dopo il traguardo orb-IBL. Tre fasi in ordine. La validazione
visiva di ogni fase la fa Giulio (regola no-Argent). Aggiornare l'HANDOFF man mano.

## Reference target (rettifica 2026-07-04)

> RETTIFICA di Giulio: **metal NON cambia** (resta colore + forma attuali). La ref
> cromata di prima (`metal-glass-target.png`) NON è più il target di metal. Il
> nuovo target riguarda **fluidGradient**.

Due nuovi material target, file già nel repo:

### `aura` → `references/materials/aura.webp`
Sfera "neon / energy orb" su sfondo nero: glow **verde** (basso) e **magenta/viola**
(alto/lati); interno **viola/indaco** con **flussi rosa-magenta luminosi** e punto
luce blu al centro. Bioluminescente. (Era la ref "fluid".)

### `glass` → `references/materials/glass.webp`
Sfera di **vetro/chrome liquido scuro** traslucido: onde larghe, riflessi
viola/lavanda/blu con accenti **rossi** e highlight bianchi, zone scure.
La gemma rossa in basso a destra è un **artefatto di ritaglio → ignorare**.

### metal
Colore INVARIATO; forma 1:1 con `references/materials/metal.png` (blob organico).

## Fase 0 — Architettura & condivisione codice (analisi + refactor)

Obiettivo Giulio: file più piccoli con responsabilità chiare (SOLID), migliori
tecniche, e **non riscrivere la matematica due volte** per Android/iOS — valutando
il C++.

### Analisi: dove può vivere il codice condiviso

Fatto tecnico chiave: **la matematica degli shader gira sulla GPU** (AGSL/SkSL su
Android, MSL su iOS). Il **C++/JSI (Nitro) gira sulla CPU**. Quindi la matematica
dello shader NON può essere scritta in C++ e usata dalla GPU su entrambe le
piattaforme: sono domini di esecuzione diversi. Le conseguenze:

1. **Logica CPU** (risoluzione material/motion → parametri, preset, parsing colori)
   è **già condivisa via TypeScript**: RN esegue lo stesso TS su iOS e Android.
   Spostarla in C++ non dà vantaggio (solo complessità), a meno di doverla chiamare
   da codice nativo puro. → per ora resta in TS.
2. **Matematica GPU condivisa senza duplicare** — le opzioni reali (NON C++):
   - **(A, consigliata) Sorgente shader condiviso + adapter di compatibilità.**
     AGSL/SkSL e MSL condividono ~90% della sintassi per funzioni pure (noise, fbm,
     fresnel, reflect/refract, rotate, equirect). Si scrive la libreria **una volta**
     in file `.shared` + due "prelude" (alias di tipi: `vec2`↔`float2`, `half`, ecc.)
     e un piccolo build step (o concatenazione a runtime) compone
     `prelude-piattaforma + lib + material`. La matematica si mantiene una volta.
   - **(B) Transpiler** (scrivere in GLSL, SPIRV-Cross → MSL, adattare AGSL): più
     dipendenze/CI; overkill per una libreria di funzioni pure.
   - Raccomando **(A)**.
3. **C++/Nitro** resta la porta giusta SOLO per eventuale logica CPU pesante e
   condivisa non-JS in futuro (es. pre-filtrare l'HDRI in mip/irradiance una volta,
   decodifica asset). Non necessario ora; documentato come opzione.

### Refactor proposto (output visivo invariato)

Shader modulari (SRP), composti dal nativo:
```
assets/shaders/
├── lib/     noise.shared · ibl.shared · geometry.shared
├── materials/  metal.shared · water.shared · iridescent.shared · glass.shared · fluid.shared
└── prelude/   agsl.prelude · msl.prelude   (alias di tipi/funzioni)
→ build/runtime compone: prelude + lib + material selezionato
```
- **Registry material nativo** (Strategy pattern): un `ShaderMaterial` per nome,
  invece dei branch `if shader == ...` sparsi. Android e iOS caricano/compongono
  con la stessa struttura.
- **TS SOLID**: un file per material in `materials/`, registry aperto all'estensione
  (aggiungere un material = aggiungere un file, senza toccare il core); `skins/`
  (R2) separati dai material; `motions/` già ok.

### DoD Fase 0
Refactor a **output visivo identico** (nessuna regressione), `bun run typecheck`,
`bun test`, `assembleDebug` verdi, e Giulio conferma che i tre orb rendono come
prima. Documentazione aggiornata (questo file + engineering).

## Material definitivi (5)
`metal`, `water`, `iridescent`, **`aura`** (neon, ex-`fluidGradient`), **`glass`**
(chrome liquido scuro). `liquidMetal` (Paper) e `fluidGradient` ESCONO
(`fluidGradient` diventa `aura`).

**Forma condivisa**: la silhouette/moto organico a onde larghe di `glass.webp` è la
forma di riferimento dei material orb; `metal` la adotta (prima: cerchio wobbly).

## Fase 1 — rimuovere liquidMetal (Paper), fluidGradient → aura
- **Rimuovere `liquidMetal`**: componente `LiquidMetal`, `liquid-metal.agsl`/`.metal`,
  `materials/liquid-metal.ts`, attribution/ref se non più usati, prop Nitro non
  condivise. Rimuovere anche il vecchio `fluidGradient` come componente-effetto.
- **`fluidGradient` → `aura`**: material neon (`aura.webp`) reso come sfera (geometria
  orb): glow verde/magenta al bordo, interno viola con flussi rosa, punto luce blu.
- DoD: `aura` ≈ ref, `liquidMetal`/`fluidGradient` rimossi; typecheck/test/build
  verdi; validazione Giulio; doc aggiornata.

## Fase 2 — forma condivisa, metal, glass, poi il resto

1. **Forma condivisa**: adottare la forma/moto organico a onde larghe di
   `glass.webp` come geometria/silhouette dei material orb (Path nativo Kotlin
   `buildMaterialOrbPath` + relief della superficie).
2. **metal**: prende la **forma di glass** (punto 1) con le **proprietà di metal**;
   avvicinare colore e percezione a `references/materials/metal.png` (argento
   cromato, rim iridescente) più della resa attuale.
3. **glass** (nuovo material): chrome liquido scuro traslucido come `glass.webp`
   (riflessi viola/blu, accenti rossi, highlight bianchi). Gemma rossa = artefatto,
   ignorare.
4. Poi rifinire `water`, `iridescent` e `aura` verso il loro standard.
- DoD: ogni material validato da Giulio; doc + esempi aggiornati.

## Ordine e regole
- Fasi sequenziali; nessuna parte senza DoD della precedente.
- Ogni fase: implementare → typecheck/test/build → validazione visiva Giulio →
  aggiornare doc/HANDOFF/CHANGELOG.
- La nuova ref va aggiunta come **file fisico**, in **doc** (references +
  ASSET-LICENSES) e negli **esempi**.
