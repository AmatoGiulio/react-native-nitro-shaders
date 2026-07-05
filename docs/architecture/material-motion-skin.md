# ARCHITECTURE — Material × Motion × Skin

Fonte di verita' dell'architettura pubblica **target** (decisa da Giulio,
2026-07-03). Questo documento è la fonte di verità dell'API *voluta*; il reference
tecnico è `../engineering/shader-techniques.md`.

> ## ⚠️ STATO ATTUALE vs TARGET (aggiornato 2026-07-04)
> Questo documento descrive il TARGET. Lo stato REALE del codice oggi è diverso —
> non confondere i due:
>
> | Aspetto | TARGET (questo doc) | ATTUALE (codice) |
> |---|---|---|
> | Componenti pubblici | `MaterialView`/`MaterialText`/`MaterialSvg` | `FluidGradient`, `LiquidMetal`, `MaterialOrb` in `src/effects/` (le Skin NON esistono ancora) |
> | Struttura src | `core/ materials/ motions/ skins/` | `core/ effects/ materials/ motions/ specs/` (nessun `skins/`) |
> | Orb | "non è un material, silhouette esce, diventa forma demo" | `metal`/`water`/`iridescent` SONO material, resi come sfera 3D via IBL in un unico `material-orb.agsl`; silhouette già nativa (Path Kotlin) |
> | Render orb | env procedurale | **IBL** (riflette un HDRI reale) — vedi `../engineering/orb-materials.md` e `../process/ORB_MATERIALS_JOURNEY.md` |
>
> La migrazione TARGET (rimozione alias, `MaterialView`, `skins/`) è la roadmap R2+
> e NON è ancora stata eseguita. Le frasi sotto scritte al passato ("rimossi",
> "esce dallo shader") vanno lette come DECISIONI da attuare, non come fatti.
>
> **Tensione aperta emersa col traguardo IBL:** i material orb sono resi su una
> normale **sferica** (servono per una forma 3D). Come si comporta un material IBL
> sferico su una Skin piatta (testo/svg/rettangolo)? Da decidere — vedi
> "Decisioni ancora aperte".

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
| `metal` | metallo liquido cromato (IBL); target forma/percezione `metal.png` + video J | `materialColor`; target PBR `metallic`/`roughness` | IBL, mode 0 |
| `water` | gel/acqua translucido (IBL) | `materialColor`; target `transmission`/`ior` | IBL, mode 1 |
| `iridescent` | bolla di sapone thin-film (IBL) | `materialColor`; target `iridescence` | IBL, mode 2 |
| `aura` | neon energy orb (glow verde/magenta, flussi rosa) — ex `fluidGradient` | `colors`/glow | da fare (ref `aura.webp`) |
| `glass` | chrome liquido scuro traslucido (riflessi viola/blu, accenti rossi) | `materialColor`; transmission | da fare (ref `glass.webp`) |

Nota (2026-07-04): la lista definitiva è **5 material**: `metal`, `water`,
`iridescent`, `aura`, `glass`. `liquidMetal` (Paper) e `fluidGradient` ESCONO
(`fluidGradient` → `aura`) — vedi `../process/OPERATIONAL-PLAN.md` Fase 1.

Ogni material e' un modulo shader (un blocco AGSL / una fragment Metal) che
calcola SOLO il colore della superficie per pixel. La forma non e' affar suo.

```ts
type Material =
  | 'metal' | 'water' | 'iridescent' | 'aura' | 'glass'   // shorthand
  | { type: 'metal'; color?: number }
  | { type: 'water'; tint?: number }
  | { type: 'iridescent'; hue?: number }
  | { type: 'aura'; colors?: string[]; glow?: number }
  | { type: 'glass'; tint?: number }
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

Principio chiave (metafora del vestito): **il masking sta nel layer nativo di
disegno, NON nello shader**. Stato: già attuato per l'orb — silhouette (Path) e
ombra sono disegnate da Kotlin, lo shader riempie il quad.

Nota (2026-07-04): `metal`/`water`/`iridescent` SONO material (definiscono
l'aspetto e come riflettono la luce). La forma sferica del demo è una Skin. La
frase originale "l'orb non è un material" serviva solo a non confonderli con
l'effetto `liquidMetal`; resta però la tensione material-IBL-sferico ↔ Skin piatta
(vedi "Decisioni ancora aperte").

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
| `MaterialOrb` | componente demo LEGACY attuale (material `metal`/`water`/`iridescent`), da migrare a `MaterialView` |
| `material-orb.agsl` | shader unico con mode 0/1/2 (IBL); silhouette già nativa (Path Kotlin), non splittato in tre file |
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
- **Alias da rimuovere** (in R2, NON ancora fatto). `FluidGradient`,
  `LiquidMetal`, `MaterialOrb` verranno eliminati a favore delle tre Skin quando
  `MaterialView` esisterà. Oggi sono ancora i componenti pubblici.

## Decisioni ancora aperte

- `fluidGradient`: `speed`/`warp` attuali migrano interamente nel Motion
  (proposta) o restano anche come parametro material. Da confermare in R2.
- **Material orb IBL su Skin piatte.** I material `metal`/`water`/`iridescent`
  sono resi con IBL su normale sferica (pensati per una forma 3D). Su una Skin
  piatta (testo/svg/rettangolo) la normale sferica non ha senso ovvio. Opzioni da
  valutare: (a) i material orb valgono solo per Skin "a volume" (blob/sfera), e le
  Skin piatte usano material 2D (fluidGradient); (b) il material espone una
  "curvatura" configurabile; (c) altro. Emersa col traguardo IBL del 2026-07-04.
