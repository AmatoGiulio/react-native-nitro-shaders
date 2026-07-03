# STACK.md

Decisioni tecniche vincolanti. Un developer non sceglie tra alternative qui
elencate come "decise" — le esegue. Le sezioni marcate "aperto" sono le uniche
su cui l'orchestratore può ancora deliberare (e solo lui).

## Stack

| Layer | Scelta | Motivo |
|---|---|---|
| Monorepo | Turborepo + bun workspaces | build cache incrementale, coerente con toolchain Nitro |
| Native bridge | Nitro Modules (`react-native-nitro-modules`) | JSI diretto, no bridge async, coerente con le altre lib nitro-* |
| Codegen | Nitrogen | genera binding TS → Swift/Kotlin/C++, riduce boilerplate manuale |
| iOS rendering | Metal (MTKView / CAMetalLayer via HybridObject) | accesso diretto GPU, no overhead |
| Android rendering | AGSL (`RuntimeShader`, API 33+) con fallback OpenGL/RenderScript per <33 | AGSL è lo standard moderno; fallback obbligatorio per copertura device |
| App di esempio | Expo (bare, con prebuild) | coerente col toolchain standard di Giulio, permette dev loop rapido |
| Package manager | bun | allineato a create-nitro-module e community Nitro |
| Lingua codice/commenti | inglese | libreria open source pubblica |
| Lingua doc di progetto (questi file) | italiano | uso interno |

## Struttura repository

```
react-native-nitro-shaders/
├── apps/
│   └── example/              # Expo app, demo + dev loop, non pubblicata
├── packages/
│   └── nitro-shaders/        # il pacchetto pubblico (npm: react-native-nitro-shaders)
│       ├── src/
│       │   ├── core/         # runtime condiviso: ShaderSurface, uniform buffer,
│       │   │                 # lifecycle, noise/fBm comuni. NON pubblico da solo.
│       │   ├── materials/    # logica shader per famiglia (fluidGradient, liquidChrome...)
│       │   ├── effects/      # componenti React pubblici che espongono i materials
│       │   └── index.ts      # unico entry pubblico del pacchetto
│       ├── ios/               # HybridObject Swift + Metal
│       ├── android/           # HybridObject Kotlin + AGSL
│       ├── cpp/                # shared C++ (se serve logica cross-platform non-shader)
│       ├── nitro.json
│       └── package.json
├── docs/
│   └── spec/ARC_shader_native_spec.md
├── CLAUDE.md
├── AGENTS.md
├── docs/process/STACK.md
├── CHANGELOG.md
├── docs/process/HANDOFF.md
└── turbo.json
```

Decisione presa (vedi conversazione precedente): **un solo pacchetto pubblico
ora**, modulare internamente. Estrazione di `core` in pacchetto privato separato
solo dopo che 2+ material sono stabili (non prima — vedi CHANGELOG per quando
questa condizione si verifica).

## Convenzioni di naming

- Componenti pubblici: `PascalCase` diretti (`FluidGradient`, `LiquidChrome`),
  nessun prefisso `Nitro`/`RN` nel nome del componente — solo nel nome del pacchetto.
- HybridObject: prefisso `Hybrid` + nome capability (`HybridShaderSurface`, non
  `HybridFluidGradientView` — l'oggetto nativo è il runtime, non l'effetto).
- Props booleane: sempre positive (`animated`, mai `notAnimated`/`disableAnimation`).
- File shader nativi: `<material>.metal`, `<material>.agsl` — nome material in
  lowercase-kebab, coerente col nome in `materials/`.

## Roadmap (fasi)

1. **Fase 1 — Core runtime**: `ShaderSurface` HybridObject (Metal + AGSL),
   uniform buffer generico, lifecycle (mount/unmount/resize), noise/fBm condivisi.
   Nessun material ancora. Definition of done: un quadrato con un colore solido
   renderizzato via shader su entrambe le piattaforme, driven da JS.
2. **Fase 2 — FluidGradient (MVP)**: primo material reale sopra il core.
   Valida che il core sia sufficiente senza modifiche strutturali.
3. **Fase 3 — LiquidChrome**: secondo material. Se richiede modifiche al core,
   quelle modifiche sono il segnale che il core non era ancora stabile in Fase 1
   — si documentano in CHANGELOG come "core breaking change, motivo: X".
4. **Fase 4 — valutazione split**: solo dopo Fase 3, l'orchestratore valuta se
   estrarre `@internal/core-native` (privato) e se separare `LiquidChrome` in
   pacchetto pubblico a sé — decisione esplicita di Giulio, non automatica.
5. **Fase 5 — effetti aggiuntivi**: dot-grid/pixel-matrix (ispirato a Immagine 4),
   varianti/preset con nome (`variant="mercury"`) sopra i material esistenti.

Nessuna fase parte prima che la precedente abbia Definition of Done verificata.

## Aperto (decide solo l'orchestratore, con Giulio se serve)

- Se il fallback Android <API 33 usa OpenGL ES custom o si accetta minSdk 33
  (da valutare in base a share device target reale — vedi analytics FP Holding
  se rilevante, altrimenti dati pubblici Android).
- Naming esatto dei preset in Fase 5 (`mercury`, `aurora`, ecc. — vanno scelti,
  non copiati 1:1 da shaders.com per motivi di originalità/branding).
