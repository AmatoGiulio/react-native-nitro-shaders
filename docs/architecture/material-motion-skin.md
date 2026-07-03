# ARCHITECTURE — Material × Motion × Skin

Fonte di verita' dell'architettura pubblica della libreria (decisa da Giulio,
2026-07-03). Sostituisce il modello precedente "un componente per effetto"
(`FluidGradient`, `LiquidMetal`, `MaterialOrb`). Questo documento è la fonte di
verità dell'API pubblica; il reference tecnico è
`../engineering/shader-techniques.md`.

## Idea

La libreria espone **materiali "pelle viva"**: superfici procedurali animate
che possono rivestire qualsiasi target. Tre assi ortogonali:

```
Material (aspetto)  ×  Motion (moto)  ×  Skin (dove vive)
```

Un elemento renderizzato = un material + un motion (opzionale, ha default) +
una skin (background, testo, vettore SVG).

### Metafora guida (Giulio)

I material "stanno sopra a tutto, come se indossassero un vestito":

- la **Skin** (view / testo / svg) e' il **corpo** che indossa;
- il **Material** e' il **tessuto e i tratti** di quel vestito (chrome, acqua,
  iridescente, gradiente...);
- il **Motion** e' la **vestibilita'**: come quel vestito si muove addosso.

Ne segue la regola dura: il Material NON conosce la forma che riveste. La
silhouette appartiene sempre alla Skin, mai allo shader del material.

## Asse 1 — Material

Cinque materiali pubblici, piatti (niente famiglie/sottocategorie):

| Nome pubblico | Descrizione | Parametri propri | Stato |
|---|---|---|---|
| `fluidGradient` | fusione in movimento di n colori | `colors[]`, `intensity`, `scale`, `warp`, `grain` | Android validato (Fase 2) |
| `liquidMetal` | derivato Paper Design (Apache 2.0) | quelli attuali di LiquidMetal | Android validato (Fase 3) |
| `metal` | chrome liquido chiaro, vene scure larghe | `color` (temperatura 0..1) | shading esistente in material-orb.agsl mode 0 |
| `water` | gel azzurro translucido, rim ciano | `tint` | mode 1 |
| `iridescent` | perla con bande arcobaleno al bordo | `hue` | mode 2 |

Ogni material e' un modulo shader (un blocco AGSL / una fragment Metal) che
calcola SOLO il colore della superficie per pixel. La forma non e' affar suo.

```ts
type Material =
  | 'fluidGradient' | 'liquidMetal' | 'metal' | 'water' | 'iridescent'  // shorthand: default
  | { type: 'fluidGradient'; colors: string[]; intensity?: number; scale?: number; warp?: number; grain?: number }
  | { type: 'liquidMetal'; /* prop attuali */ }
  | { type: 'metal'; color?: number }
  | { type: 'water'; tint?: number }
  | { type: 'iridescent'; hue?: number }
```

## Asse 2 — Motion

Il moto e' una categoria di oggetti separata, passata come prop `motion`.
Regola ibrida decisa:

- i **tipi** di moto sono generici e condivisi tra material;
- i **default** sono per-material: ogni material dichiara il proprio moto
  naturale con i propri valori (registro `MOTION_DEFAULTS[material]`);
- il material **interpreta** il moto: gli stessi parametri muovono cose
  diverse (in `metal` il warp sposta le pieghe, in `fluidGradient` il domain
  warp dei colori).

```ts
type Motion =
  | 'none'                       // statico (frame fisso, niente ticker)
  | { type: 'none' }
  | { type: 'flow';   speed?: number; amplitude?: number; warp?: number; detail?: number; seed?: number }
  | { type: 'wobble'; speed?: number; amplitude?: number; warp?: number; detail?: number; seed?: number }
  | { type: 'loop';   speed?: number; amplitude?: number; period?: number; seed?: number }
```

- `flow` — drift fBm continuo, non periodico (il moto attuale di fluidGradient
  e del marbling metal).
- `wobble` — deformazione organica lenta "che respira" (l'attuale moto orb).
- `loop` — periodico seamless (la tecnica loopTravel/phase/crossfade gia'
  implementata in F3-D2 per LiquidMetal circle), per animazioni esportabili
  o cicliche.

Contratto uniform condiviso (tutte le piattaforme):

```
u_motionType   float   // 0 none, 1 flow, 2 wobble, 3 loop
u_motionSpeed  float
u_motionAmp    float
u_motionWarp   float
u_motionDetail float
u_motionSeed   float
u_motionPeriod float   // solo loop
```

`animated`/`paused`/`debugTime` restano prop del componente (lifecycle del
ticker), non del Motion: il Motion descrive COME si muove, non SE il clock
gira.

## Asse 3 — Skin (target)

Il material puo' diventare la pelle di:

| Componente | Target | Implementazione Android | Implementazione iOS |
|---|---|---|---|
| `MaterialView` | background / rettangolo | `canvas.drawRect` con Paint+RuntimeShader | fragment full-quad (attuale) |
| `MaterialText` | testo | `canvas.drawText` con lo stesso Paint | rasterizzazione testo → alpha mask/stencil sul quad |
| `MaterialSvg` | path vettoriale | `canvas.drawPath` (path da SVG `d`) | path → alpha mask/stencil |

Principio chiave (metafora del vestito, decisa): **il masking sta nel layer
nativo di disegno, NON nello shader**. Lo shader del material non conosce la
silhouette. Conseguenza operativa: la silhouette orb wobbly oggi cablata in
`material-orb.agsl` ESCE dallo shader. Il material calcola solo il colore
della superficie; forma e alpha le mette la Skin.

L'orb non e' piu' un material: diventa (se serve in demo) una forma/skin
procedurale. Le forme wobbly procedurali possono avere una alpha-stage
condivisa opzionale, ma vivono nel layer Skin, non nel material.

## API pubblica (target finale)

```tsx
<MaterialView  material="metal" style={styles.bg} />
<MaterialView  material={{ type: 'fluidGradient', colors: ['#f0f', '#0ff', '#333'] }}
               motion={{ type: 'flow', speed: 0.8 }} style={styles.bg} />
<MaterialText  material="water" motion="none" style={styles.title}>Nitro</MaterialText>
<MaterialSvg   path="M4 8C..." material="iridescent" viewBox="0 0 24 24" style={styles.icon} />
```

Prop comuni a tutte le skin: `material`, `motion?`, `animated?`, `paused?`,
`debugTime?`, `style`.

## Mappatura dall'esistente

| Oggi | Diventa |
|---|---|
| `ShaderSurface` (core) | resta il runtime interno; `MaterialView` e' il suo wrapper pubblico |
| `FluidGradient` | `MaterialView material="fluidGradient"` (alias deprecato mantenuto durante la migrazione) |
| `LiquidMetal` | `MaterialView material="liquidMetal"` (idem) |
| `MaterialOrb` | RIMOSSO come componente: shading → materials `metal`/`water`/`iridescent`; silhouette orb → forma demo |
| `material-orb.agsl` | split: shading nei tre material; silhouette fuori dallo shader |
| prop `speed`/`wobble`/`distortion`/`detail` | assorbite dal Motion (`speed`, `amplitude`, `warp`, `detail`) |
| `materialColor` | parametro del material (`color`/`tint`/`hue`) |
| loop seamless F3-D2 | motion `loop` |

## Struttura src (aggiorna STACK.md)

```
src/
├── core/        # ShaderSurface runtime, ticker, uniform plumbing
├── materials/   # definizione TS dei 5 material + default
├── motions/     # tipi Motion, MOTION_DEFAULTS per material, risoluzione uniform
├── skins/       # MaterialView, MaterialText, MaterialSvg (componenti pubblici)
└── index.ts
```

(`effects/` viene assorbita da `skins/`.)

## Roadmap v2

1. **R1 — Contratto**: tipi TS `Material`/`Motion`, spec Nitro con uniform
   motion condivise + uniform per-material; nessun rendering nuovo. DoD:
   typecheck verde, codegen rigenerato.
2. **R2 — Android materials piatti**: split di material-orb in shading puro,
   `MaterialView` con i 5 material su rect, motion plumbing. DoD: demo con i
   5 material, validazione visiva Giulio.
3. **R3 — Skin testo Android**: `MaterialText` via drawText. DoD: demo testo,
   validazione Giulio.
4. **R4 — Skin SVG Android**: `MaterialSvg` via drawPath. DoD: demo icona.
5. **R5 — iOS**: porting materials + skin (mask/stencil), risoluzione debito
   default library Metal. BLOCKED finche' Giulio non valida R2 su Android.
6. **R6 — Demo galleria** e alias deprecati rimossi prima della pubblicazione.

## Decisioni prese (Giulio, 2026-07-03)

- **Motion: modello ibrido.** Tipi di moto condivisi (`none`/`flow`/`wobble`/
  `loop`), default e interpretazione per-material, `motion` opzionale e
  sovrascrivibile.
- **Silhouette fuori dallo shader.** Metafora del vestito: material = tessuto,
  skin = corpo, motion = vestibilita'. L'orb non e' un material.
- **Naming skin: `MaterialView` / `MaterialText` / `MaterialSvg`.**
- **Alias rimossi subito.** `FluidGradient`, `LiquidMetal`, `MaterialOrb`
  come componenti pubblici vengono eliminati: nessun utente esterno ancora,
  API pulita da subito. Restano solo le tre Skin.

## Decisioni ancora aperte

- `fluidGradient`: `speed`/`warp` attuali migrano interamente nel Motion
  (proposta) o restano anche come parametro material. Da confermare in R1.
