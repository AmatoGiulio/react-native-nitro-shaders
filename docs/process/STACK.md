# STACK.md

Decisioni tecniche vincolanti. Un developer non sceglie tra alternative qui
elencate come "decise" — le esegue. Le sezioni marcate "aperto" sono le uniche
su cui l'orchestratore può ancora deliberare (e solo lui).

> **Architettura pubblica: vedi `docs/architecture/material-motion-skin.md`
> (Material × Motion × Skin).** Quel documento è la fonte di verità dell'API.
> STACK.md ne recepisce struttura src, naming e roadmap; in caso di conflitto
> vince l'architettura. Reference tecnico: `docs/engineering/shader-techniques.md`.

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
│       │   │                 # ticker, lifecycle, noise/fBm comuni. NON pubblico da solo.
│       │   ├── materials/    # definizione TS dei 5 material + default (aspetto)
│       │   ├── motions/      # tipi Motion, MOTION_DEFAULTS per material, → uniform
│       │   ├── skins/        # componenti pubblici MaterialView/MaterialText/MaterialSvg
│       │   └── index.ts      # unico entry pubblico del pacchetto
│       ├── ios/               # HybridObject Swift + Metal
│       ├── android/           # HybridObject Kotlin + AGSL
│       ├── cpp/                # shared C++ (se serve logica cross-platform non-shader)
│       ├── nitro.json
│       └── package.json
├── docs/
│   ├── README.md            # indice della documentazione
│   ├── architecture/        # design pubblico (fonte di verità API)
│   ├── engineering/         # matematica shader, note piattaforma
│   ├── process/             # STACK.md, HANDOFF.md
│   └── references/          # screenshot target, attribution, ref esterni
├── CLAUDE.md
├── AGENTS.md
├── CHANGELOG.md
└── turbo.json
```

Nota: l'albero sopra è il TARGET. ATTUALE (2026-07-04):
`src/{core, effects, materials, motions, specs}` — c'è `effects/` (FluidGradient,
LiquidMetal, MaterialOrb) e NON c'è ancora `skins/`. `cpp/` non esiste ancora.
La migrazione a `skins/` è R2.

Decisione presa (vedi conversazione precedente): **un solo pacchetto pubblico
ora**, modulare internamente. Estrazione di `core` in pacchetto privato separato
solo dopo che 2+ material sono stabili (non prima — vedi CHANGELOG per quando
questa condizione si verifica).

## Convenzioni di naming

- Componenti pubblici TARGET = le tre **Skin**: `MaterialView`, `MaterialText`,
  `MaterialSvg`. ATTUALE (2026-07-04): esistono ancora `FluidGradient`,
  `LiquidMetal`, `MaterialOrb` in `src/effects/` (le Skin non esistono; da fare in
  R2). Nessun prefisso `Nitro`/`RN` nel nome del componente — solo nel pacchetto.
- Material = valore della prop `material` (stringa o oggetto), non un componente:
  `'fluidGradient' | 'liquidMetal' | 'metal' | 'water' | 'iridescent'`.
- Motion = valore della prop `motion`: `'none' | 'flow' | 'wobble' | 'loop'`
  (o oggetto con parametri).
- HybridObject: prefisso `Hybrid` + nome capability (`HybridShaderSurface`,
  l'oggetto nativo è il runtime, non l'effetto).
- Props booleane: sempre positive (`animated`, mai `notAnimated`/`disableAnimation`).
- File shader nativi: `<material>.metal`, `<material>.agsl` — nome material in
  lowercase-kebab, coerente col nome in `materials/`.

## Roadmap

Le Fasi 1–4 (core, FluidGradient, LiquidMetal, primo MaterialOrb) sono STORIA
COMPLETATA — vedi CHANGELOG. Da qui in poi vale la roadmap v2 dell'architettura
Material × Motion × Skin (dettaglio in `docs/architecture/material-motion-skin.md`):

1. **R1 — Contratto**: tipi TS `Material`/`Motion`, spec Nitro con uniform
   motion condivise + uniform per-material, codegen. DoD: typecheck verde.
2. **R2 — Skin `MaterialView` + cleanup**: creare `MaterialView` che espone i 5
   material; migrare/rimuovere `MaterialOrb`/`FluidGradient`/`LiquidMetal`;
   parametrizzare i material orb. Nota: il rendering orb (IBL) è GIÀ fatto in
   `MaterialOrb`; manca il wrapper Skin. DoD: validazione visiva Giulio.
3. **R3 — Skin testo Android** (`MaterialText` via drawText). DoD: Giulio.
4. **R4 — Skin SVG Android** (`MaterialSvg` via drawPath). DoD: Giulio.
5. **R5 — iOS**: porting materials + skin (mask/stencil), debito default
   library Metal. BLOCKED finché Giulio non valida R2 su Android.
6. **R6 — Demo galleria** + rimozione alias residui prima della pubblicazione.

Nessuna fase parte prima che la precedente abbia Definition of Done verificata.
Regola operativa vincolante: **gli agenti non usano Argent/emulatori/Metro**
(vedi CLAUDE.md); implementano, fanno typecheck e unit test. La validazione
visiva (DoD) la esegue solo Giulio.

## Aperto (decide solo l'orchestratore, con Giulio se serve)

- Se il fallback Android <API 33 usa OpenGL ES custom o si accetta minSdk 33
  (da valutare in base a share device target reale — vedi analytics FP Holding
  se rilevante, altrimenti dati pubblici Android).
- `fluidGradient`: `speed`/`warp` migrano interamente nel Motion o restano anche
  come parametro material (da confermare in R1 — vedi architecture doc).
