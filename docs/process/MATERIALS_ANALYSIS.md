# Materials Analysis

Analisi operativa delle reference in `docs/refs/materials/`.

## Decisione di separazione

Queste reference NON sono il `LiquidMetal` Paper Design gia' portato.

`LiquidMetal` resta il materiale Paper, con API e shader propri.
Le nuove sfere sono una famiglia separata di shader knobs/materials. Categoria
commerciale di lavoro:

```txt
Procedural Material Knobs
```

oppure, per una demo/app piu' orientata al movimento:

```txt
Live Shader Knobs
```

Tecnicamente possono probabilmente condividere un renderer orb/surface e
divergere per material model:

- `metal`
- `water`
- `iridescent`

Nome di lavoro interno: `MaterialOrb`.

## Reference

- `docs/refs/materials/screenshot_001.png`: overview con tre sfere impilate.
- `docs/refs/materials/screenshot_002.png`: preset Metal selezionato.
- `docs/refs/materials/screenshot_003.png`: preset Water selezionato.
- `docs/refs/materials/screenshot_004.png`: preset Iridescent selezionato.

Gli screenshot mostrano anche knob condivisi:

- `speed`
- `wobble`
- `distortion`
- `detail`
- `color`

Questi nomi sono utili come direzione di API, ma vanno adattati allo stile della
libreria prima di diventare props pubbliche.

## Material 1 - Liquid Chrome

### Target visivo

Nome commerciale consigliato:

```txt
Liquid Chrome
```

Alias utili: Liquid Metal, Mercury Effect, Molten Chrome, Fluid Metal Shader,
Reflective Blob.

Nome tecnico:

```txt
reflective noise displacement + environment reflection
```

Sfera chrome/molten metal organica, non a bande regolari come il LiquidMetal
Paper. La superficie simula mercurio/acciaio liquido molto riflettente,
deformato da rumore/frattali, con:

- base argento/cromo;
- highlights bianchi larghi e morbidi;
- ombre grigio scuro localizzate;
- leggere sfumature viola/verde ai bordi;
- silhouette non perfettamente circolare, appena wobble.

### Modello tecnico probabile

Generative orb shader:

- SDF circolare con edge feather;
- normal map procedurale da height field/fBm;
- fake environment / matcap procedurale;
- rim light morbido;
- shadow ellittica separata nella demo, non necessariamente parte del material.

Non richiede texture esterne.

Keyword ricerca/reference:

- `liquid chrome shader`
- `mercury blob shader`
- `molten metal effect`
- `reflective metal blob`
- `procedural chrome material`

### Props/preset iniziali

```ts
type MaterialOrbPreset = 'metal' | 'water' | 'iridescent'
```

Metal default osservati dalla UI:

- `speed`: 1.00
- `wobble`: 1.00
- `distortion`: 0.77
- `detail`: 1.00
- `color`: 0.50

## Material 2 - Liquid Glass / Jelly

### Target visivo

Nome commerciale consigliato:

```txt
Liquid Glass
```

Alias utili: Aqua Glass, Jelly Blob, Gel Orb, Translucent Bubble Shader,
Frosted Liquid.

Nome tecnico:

```txt
translucent refraction + blur + fresnel
```

Sfera gel/acqua azzurra, translucida e morbida:

- corpo blu/ciano lattiginoso;
- bordo ciano luminoso;
- highlight speculare bianco piccolo in alto a destra;
- caustiche o cloud interni molto sfumati;
- silhouette piu' morbida e gelatinosa rispetto al metal.

### Modello tecnico probabile

Stesso renderer orb, shader mode diverso:

- SDF orb;
- normal map procedurale piu' morbida;
- translucency fake con colore interno;
- rim glow ciano;
- specular highlight localizzato;
- noise/fBm interno a bassa frequenza.

Non e' Apple-style backdrop glass per MVP: non campiona il background. Deve
sembrare piu' gel/acqua che pannello frosted UI.

Keyword ricerca/reference:

- `liquid glass orb shader`
- `jelly blob effect`
- `aqua glass material`
- `translucent gel shader`
- `soft bubble shader`

### Props/preset iniziali

Water default osservati:

- `speed`: 1.26
- `wobble`: 0.60
- `distortion`: 0.55
- `detail`: 1.10
- `color`: 0.55

## Material 3 - Iridescent Glass

### Target visivo

Nome commerciale consigliato:

```txt
Iridescent Glass
```

Alias utili: Pearlescent Material, Holographic Blob, Opal Glass, Soap Bubble
Effect, Chromatic Aberration Glass.

Nome tecnico:

```txt
chromatic aberration + thin-film/gradient iridescence
```

Sfera perla/sapone iridescente:

- base bianco latte / rosa tenue;
- bande cromatiche arcobaleno sul bordo;
- centro molto morbido e poco contrastato;
- highlight piccolo in alto a destra;
- bordo piu' saturo, soprattutto magenta/ciano/verde.

### Modello tecnico probabile

Stesso renderer orb:

- SDF orb;
- normal map procedurale morbida;
- fresnel/rim colorato;
- iridescence da fase angolare o normale (`sin` con offsets RGB);
- saturazione concentrata vicino al bordo;
- noise lento per deformare la banda.

Keyword ricerca/reference:

- `iridescent glass shader`
- `holographic blob effect`
- `pearlescent material shader`
- `soap bubble shader`
- `chromatic aberration glass`

### Props/preset iniziali

Iridescent default osservati:

- `speed`: 0.90
- `wobble`: 0.40
- `distortion`: 0.40
- `detail`: 1.00
- `color`: 0.65

## API proposta per prima tranche

La prima tranche deve restare surface-only e non introdurre maschere, testo,
texture o refraction.

```ts
type MaterialOrbProps = {
  material?: 'liquidChrome' | 'liquidGlass' | 'iridescentGlass'
  speed?: number
  wobble?: number
  distortion?: number
  detail?: number
  color?: number
  animated?: boolean
  paused?: boolean
  debugTime?: number
  style?: ViewStyle
}
```

Nota naming: `material` e' piu' chiaro di `variant` perche' Giulio ha chiarito
che non sono varianti del LiquidMetal Paper. Internamente possono comunque
condividere uno shader. I nomi brevi `metal`, `water`, `iridescent` restano
utili per UI labels, ma l'API pubblica dovrebbe preferire nomi non ambigui.

## Shader contract consigliato

Nuovo shader name:

```txt
materialOrb
```

Uniform aggiuntive:

```txt
orbMaterial: number // 0 liquidChrome, 1 liquidGlass, 2 iridescentGlass
wobble: number
distortion: number
detail: number
color: number
```

Riusa le uniform gia' presenti dove sensato:

- `speed`
- `animated`
- `paused`
- `debugTime`

## Roadmap consigliata

1. Implementare `MaterialOrb` solo Android AGSL, `material="liquidChrome"`,
   shape orb singola, shadow non inclusa nel material.
2. Aggiungere `liquidGlass` e `iridescentGlass` nello stesso shader come
   mode/preset.
3. Portare lo stesso shader in Metal/Swift.
4. Solo dopo validazione visiva, costruire la demo con segmented control e sliders.

## Rischi

- Se si tenta subito una demo interattiva completa, il task diventa UI invece che
  material validation.
- L'iridescent puo' diventare troppo saturo o troppo "arcobaleno sticker"; va
  vincolato al bordo tramite fresnel.
- Water puo' sembrare glass/refraction: evitare promesse di rifrazione reale nel
  MVP.
- I tre material devono essere separati dal LiquidMetal Paper nel naming e nei
  file per non contaminare la fase gia' validata.
