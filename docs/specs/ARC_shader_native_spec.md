# DOCUMENTO DI SPECIFICHE TECNICHE PER ARCHITETTURA DI RENDERING SHADER NATIVA

**Destinatario:** Sviluppatore AI / Code Agent  
**Obiettivo:** sviluppare un modulo nativo per React Native New Architecture / Fabric, con dipendenze esterne ridotte a zero. Il modulo deve esporre componenti `View` ad alte prestazioni usando shader nativi:

- **iOS:** Metal / Metal Shading Language, preferibilmente shader `.metal` inclusi nel bundle e compilati nella pipeline Metal.
- **Android:** AGSL / `RuntimeShader` su Android 13+ / API 33; fallback controllato per versioni inferiori.

> Nota tecnica importante: AGSL permette shader definiti come stringa a runtime dentro la pipeline grafica Android. Su iOS, invece, la strada robusta è includere shader `.metal` nel bundle e creare la `MTLRenderPipelineState` dalla libreria Metal dell'app. Parlare genericamente di “compilazione runtime” per entrambe le piattaforme è impreciso.

---

# REVISIONE TECNICA DEL DOCUMENTO ORIGINALE

## Correzioni principali

### 1. “Simplex Noise = Rumore di Perlin” è impreciso
Simplex Noise non è semplicemente “Rumore di Perlin”. È un algoritmo successivo progettato da Ken Perlin per ridurre alcuni limiti del Perlin classico, specialmente in dimensioni alte. Nel documento va scritto:

> usare Value Noise, Gradient Noise, Perlin Noise o Simplex Noise in base a costo, qualità visiva e compatibilità shader.

Per mobile 2D, spesso una combinazione di **hash noise + fBm + domain warping** è più semplice e più prevedibile di un Simplex completo.

### 2. Navier-Stokes non va citato come base reale dell'effetto
Per un gradient mesh fluido UI non stiamo simulando una fluidodinamica fisicamente corretta. Stiamo generando un moto organico tramite rumore procedurale. Meglio evitare formule o promesse che facciano pensare a un solver fisico.

Correzione:

> l’effetto è una simulazione estetica cinematica, non una simulazione fisica dei fluidi.

### 3. Metal non equivale ad AGSL
AGSL è integrato nel rendering Android e può filtrare `View` o personalizzare `Canvas`. Metal è una pipeline grafica più esplicita: command queue, command buffer, render pass, pipeline state, drawable.

Correzione architetturale:

- iOS: `MTKView` o `CAMetalLayer`, `MTLDevice`, `MTLCommandQueue`, `MTLRenderPipelineState`, uniform buffer.
- Android: `RuntimeShader` + `Paint` per disegnare una custom view, oppure `RenderEffect.createRuntimeShaderEffect` quando serve filtrare il contenuto già renderizzato di una `View`.

### 4. Su Android non usare sempre `RenderEffect`
Nel documento originale si suggerisce `RenderEffect.createRuntimeShaderEffect` come strada principale. È corretta, ma non sempre ideale.

Scelta consigliata:

- **Shader generativo puro**: custom `View` + `Canvas.drawPaint(Paint(shader))`.
- **Shader applicato al contenuto della view**: `RenderEffect.createRuntimeShaderEffect`.
- **Testo shimmer / chrome**: `TextView.getPaint().setShader(...)` o custom text drawing.
- **Glass/refraction su contenuto sottostante**: `RenderEffect` chain, ma con limiti forti sulla cattura dello sfondo.

### 5. iOS blur/refractive glass è più difficile di quanto sembri
`UIVisualEffectView` offre blur nativo, ma non dà automaticamente accesso shader ai pixel sottostanti. Per rifrazione custom reale servono:

- una texture dello sfondo;
- una pipeline di render offscreen;
- un pass Metal che campiona quella texture con UV distorti;
- oppure un fallback meno fisico usando `UIVisualEffectView` + overlay speculari.

Il documento deve distinguere chiaramente tra:
- **glass blur nativo realistico e stabile**;
- **glass refraction custom**, più costoso e complesso.

### 6. Alcune frasi di mercato sono troppo assolute
Frasi come “estetica premium dominante” o “prodotto a valore commerciale altissimo” sono utili come direzione, ma non devono guidare l’implementazione. Meglio classificare ogni effetto con:

- impatto visivo;
- complessità tecnica;
- rischio di performance;
- compatibilità piattaforme;
- valore come libreria React Native.

---

# DECISIONE ARCHITETTURALE: UNA LIBRERIA UNICA, MODULARE INTERNAMENTE

## Scelta consigliata

La libreria deve nascere come **un solo pacchetto React Native**, con più effetti esportati ma con un unico runtime grafico nativo condiviso.

Nome provvisorio consigliato:

```txt
react-native-arc-shaders
```

Alternative più brevi:

```txt
react-native-chroma
react-native-luma
react-native-native-shaders
react-native-arc-render
```

Non partire con librerie separate come:

```txt
react-native-fluid-gradient
react-native-liquid-chrome
react-native-liquid-glass
react-native-chrome-text
```

Questa separazione sembra pulita a livello marketing, ma tecnicamente è inefficiente nella fase iniziale: ogni pacchetto finirebbe per ricostruire lifecycle nativo, Fabric component, display loop, gestione props, parser colori, fallback Android, pipeline Metal, invalidation frame, reduce motion, test performance e demo app.

La parte ad alto valore non è il singolo effetto. È il **motore shader React Native cross-platform**.

Formula corretta:

```txt
Core nativo condiviso
  -> ShaderSurface
    -> Material shaders
      -> Componenti pubblici specializzati
```

## Filosofia della libreria

La libreria non deve essere descritta come “collezione di effetti”. Deve essere un:

```txt
native shader rendering kit for React Native
```

Gli effetti sono preset/componenti sopra il runtime.

Questo evita di costruire due volte la stessa cosa e permette di aggiungere nuovi effetti senza cambiare architettura.

---

# CATEGORIZZAZIONE TECNICA DEGLI EFFETTI

Gli effetti vanno distinti per **famiglia tecnica**, non solo per resa estetica. Due effetti possono sembrare simili visivamente, ma richiedere pipeline completamente diverse.

## Categoria 1 — Procedural Surface Effects

Effetti generativi puri. Non leggono contenuto sottostante, non mascherano testo, non catturano texture. Disegnano direttamente una superficie.

### Effetti inclusi

```txt
FluidGradient
AuroraGradient
PlasmaGradient
```

### Pipeline condivisa

```txt
ShaderSurface
uniforms comuni
full-screen triangle / canvas paint
noise / fBm / domain warping
palette interpolation
dithering / grain
```

### Props comuni

```ts
type ProceduralSurfaceProps = ShaderEffectBaseProps & {
  colors?: string[];
  scale?: number;
  intensity?: number;
  speed?: number;
  grain?: number;
};
```

### Priorità

Questa è la categoria migliore per l’MVP perché è stabile, performante e non dipende dalla gerarchia UI sottostante.

---

## Categoria 2 — Reflective / Metallic Surface Effects

Effetti che simulano metallo, cromo, mercurio liquido, riflessi ambientali finti o matcap procedurali.

### Effetti inclusi

```txt
LiquidChrome
MercuryLiquid
BlackChrome
RainbowOilChrome
```

### Differenza chiave

`LiquidChrome` non è un testo shimmer e non è un glass blur. È una superficie completa con:

```txt
height field procedurale
normal map derivata
fake environment mapping
Fresnel semplificato
bande chrome ad alto contrasto
grain anti-banding
```

### Pipeline condivisa

```txt
ShaderSurface
ChromeMaterial
heightField()
normalFromHeight()
chromePalette()
contrast shaping
```

### Priorità

Molto alta. È l’effetto con maggiore “wow factor” e può diventare l’elemento distintivo della libreria.

---

## Categoria 3 — Text / Masked Shader Effects

Effetti applicati a testo, icone o maschere alpha. Tecnicamente non sono uguali agli effetti surface, perché il problema principale diventa la maschera.

### Effetti inclusi

```txt
ChromeText
ShimmerText
GradientText
IconShader
MaskedShader
```

### Differenza chiave rispetto a LiquidChrome

`LiquidChrome` disegna una superficie libera.

`ChromeText` applica un materiale chrome alla sagoma del testo.

Quindi condividono il materiale, ma non il renderer:

```txt
ChromeMaterial
  -> LiquidChromeSurface renderer
  -> ChromeText renderer
```

Non duplicare la matematica chrome. Duplicare solo ciò che è inevitabilmente specifico: layout testo, alpha mask, baseline, accessibility, multilinea, font metrics.

### Nota su ShaderMask generico

Un componente generico tipo:

```tsx
<ShaderMask shader="chrome">
  <Text>Premium</Text>
</ShaderMask>
```

è desiderabile, ma non deve essere nell’MVP. Richiede snapshot/offscreen rendering dei children, gestione layout asincrono, accessibilità, texture intermedie e fallback complessi.

Per MVP è più realistico esporre:

```tsx
<ChromeText text="Premium" />
<ShimmerText text="Upgrade" />
```

---

## Categoria 4 — Glass / Backdrop / Refraction Effects

Effetti che simulano vetro, blur, rifrazione o distorsione del contenuto sottostante.

### Effetti inclusi

```txt
GlassCard
LiquidGlass
RefractiveGlass
BackdropShader
```

### Differenza chiave

Questa famiglia è tecnicamente diversa perché può richiedere accesso ai pixel dietro la view.

Versione semplice:

```txt
blur nativo
tint overlay
border highlight
noise sottile
specular edge
```

Versione avanzata:

```txt
snapshot/offscreen texture del background
normal map procedurale
UV distortion
sampling del contenuto sottostante
compositing finale
```

### Priorità

Non partire dalla rifrazione reale. L’MVP deve contenere al massimo un `GlassCard` credibile, non un `RefractiveGlass` fisicamente ambizioso.

---

# MATRICE ANTI-DUPLICAZIONE

Questa tabella definisce cosa deve essere costruito una sola volta e cosa può essere specifico del singolo effetto.

| Blocco | Unico nel core | Specifico per effetto | Note |
|---|---:|---:|---|
| Fabric native component | Sì | No | Una base comune per superfici shader. |
| Display loop / clock | Sì | No | `CADisplayLink` su iOS, `Choreographer` su Android. |
| Pause/resume lifecycle | Sì | No | App background, view detached, low power, reduce motion. |
| Uniform binding | Sì | Parziale | Ogni effetto ha uniform proprie, ma stesso sistema di binding. |
| Color parser | Sì | No | Conversione JS color -> float4. |
| Shader registry | Sì | No | Mappa effetto -> shader source/pipeline. |
| Pipeline cache | Sì | No | Evita ricreazione pipeline Metal / RuntimeShader. |
| Dithering/grain | Sì | Parziale | Funzioni shared negli shader. |
| Noise/fBm utilities | Sì | Parziale | Shared shader code, parametri diversi per effetto. |
| Text layout | No | Sì | Solo per `ChromeText`, `ShimmerText`. |
| Backdrop capture | No | Sì | Solo per glass/refraction avanzati. |
| API wrapper React | No | Sì | Ogni effetto ha props amichevoli. |

---

# STRUTTURA PACCHETTO CONSIGLIATA

```txt
react-native-arc-shaders/
  src/
    index.ts
    components/
      ShaderSurface.tsx
      FluidGradient.tsx
      AuroraGradient.tsx
      LiquidChrome.tsx
      ChromeText.tsx
      ShimmerText.tsx
      GlassCard.tsx
    materials/
      fluid.ts
      chrome.ts
      glass.ts
      text.ts
    types/
      common.ts
      colors.ts
      uniforms.ts
    utils/
      normalizeColor.ts
      presets.ts

  ios/
    ARCShaderView.swift
    ARCShaderRenderer.swift
    ARCMetalPipelineCache.swift
    ARCShaderRegistry.swift
    Shaders/
      Shared.metal
      FluidGradient.metal
      LiquidChrome.metal
      ChromeText.metal

  android/
    src/main/java/com/arcshaders/
      ARCShaderView.kt
      ARCShaderRenderer.kt
      ARCShaderRegistry.kt
      ARCUniformBinder.kt
      ARCChromeTextView.kt
    src/main/assets/shaders/
      shared.agsl
      fluid_gradient.agsl
      liquid_chrome.agsl
      chrome_text.agsl

  example/
    src/screens/
      FluidGradientDemo.tsx
      LiquidChromeDemo.tsx
      ChromeTextDemo.tsx
      GlassCardDemo.tsx
```

## Regola importante

I componenti pubblici come `LiquidChrome` e `FluidGradient` devono essere wrapper TypeScript sopra lo stesso componente nativo base, quando possibile.

Esempio:

```tsx
export function LiquidChrome(props: LiquidChromeProps) {
  return (
    <ShaderSurface
      shader="liquidChrome"
      uniforms={mapLiquidChromeUniforms(props)}
      style={props.style}
    />
  );
}
```

Questo impedisce di creare una native view diversa per ogni effetto.

---

# API REACT NATIVE PROPOSTA

## Props base comuni

```ts
type ShaderEffectBaseProps = {
  style?: ViewStyle;
  speed?: number;        // default 1
  intensity?: number;    // default 1
  paused?: boolean;
  animated?: boolean;
  reduceMotionBehavior?: 'respect' | 'ignore' | 'static';
  debugTime?: number;
  onReady?: () => void;
  onError?: (error: { code: string; message: string }) => void;
};
```

## Surface shader generico interno

```ts
type ShaderSurfaceProps = ShaderEffectBaseProps & {
  shader: 'fluidGradient' | 'auroraGradient' | 'liquidChrome' | 'glassCard';
  uniforms?: Record<string, number | number[] | string | string[] | boolean>;
};
```

`ShaderSurface` può essere esportato, ma deve essere documentato come API avanzata. L’uso normale deve passare dai componenti specializzati.

## Componenti pubblici

```ts
export const ShaderSurface: React.ComponentType<ShaderSurfaceProps>;
export const FluidGradient: React.ComponentType<FluidGradientProps>;
export const AuroraGradient: React.ComponentType<AuroraGradientProps>;
export const LiquidChrome: React.ComponentType<LiquidChromeProps>;
export const ChromeText: React.ComponentType<ChromeTextProps>;
export const ShimmerText: React.ComponentType<ShimmerTextProps>;
export const GlassCard: React.ComponentType<GlassCardProps>;
```

## Props effetto: FluidGradient

```ts
type FluidGradientProps = ShaderEffectBaseProps & {
  colors: string[];
  scale?: number;
  warp?: number;
  grain?: number;
};
```

## Props effetto: LiquidChrome

```ts
type LiquidChromeProps = ShaderEffectBaseProps & {
  variant?: 'silver' | 'mercury' | 'blackChrome' | 'rainbowOil';
  scale?: number;
  flow?: number;
  distortion?: number;
  contrast?: number;
  highlightWidth?: number;
  highlightIntensity?: number;
  grain?: number;
  interactive?: boolean;
  pointerInfluence?: number;
};
```

## Props effetto: ChromeText

```ts
type ChromeTextProps = ShaderEffectBaseProps & {
  text: string;
  fontSize?: number;
  fontFamily?: string;
  fontWeight?: string;
  variant?: 'silver' | 'gold' | 'darkChrome' | 'rainbowChrome';
};
```

## Props effetto: GlassCard

```ts
type GlassCardProps = ShaderEffectBaseProps & {
  blurRadius?: number;
  tint?: 'light' | 'dark' | 'system';
  opacity?: number;
  edgeHighlight?: number;
  cornerRadius?: number;
  noise?: number;
};
```

---

# REGOLE GLOBALI DI IMPLEMENTAZIONE

1. JavaScript deve solo montare il componente e passare props serializzabili.
2. L’animazione deve vivere lato nativo, guidata da display link / choreographer.
3. Nessun frame deve dipendere da `setState`, Reanimated o timer JS.
4. Ogni effetto deve avere fallback statico per:
   - Android < API 33;
   - reduce motion;
   - low power mode;
   - dispositivi con GPU debole.
5. Ogni shader deve supportare `debugTime` per test deterministici.
6. Il parser colori, il clock nativo, il registry shader e la pipeline cache devono essere condivisi.
7. Gli effetti text-based non devono duplicare la matematica chrome: devono riusare il materiale chrome, cambiando solo renderer e maschera.
8. Gli effetti glass/refraction non devono bloccare l’MVP: vanno isolati come famiglia avanzata.

---

# COSA COSTRUIRE E COSA NON COSTRUIRE NELL'MVP

## Costruire subito

```txt
ShaderSurface core
FluidGradient
LiquidChrome
fallback statico Android < 33
example app con benchmark base
```

## Costruire nella seconda fase

```txt
ChromeText
ShimmerText
GlassCard semplificato
preset gallery
debugTime snapshots
```

## Non costruire subito

```txt
ShaderMask generico
RefractiveGlass reale
BackdropShader con snapshot/offscreen texture
fallback OpenGL ES completo
editor visuale shader
15 effetti demo non rifiniti
```

Motivo: questi blocchi sono tecnicamente attraenti ma rischiano di trasformare l’MVP in una piattaforma troppo ampia prima che il core sia validato.

---

# ROADMAP ARCHITETTURALE AGGIORNATA

## MVP 1 — Core + Surface Effects

Obiettivo: dimostrare che il runtime nativo funziona.

```txt
ShaderSurface
FluidGradient
LiquidChrome
```

Piattaforme:

```txt
iOS Metal
Android AGSL API 33+
Android fallback statico
```

## MVP 2 — Premium UI Components

Obiettivo: rendere la libreria utile in UI reali.

```txt
ChromeText
ShimmerText
GlassCard
```

## MVP 3 — Advanced Rendering

Obiettivo: espandere verso effetti complessi.

```txt
ShaderMask
RefractiveGlass
BackdropShader
```

Questa fase deve partire solo dopo benchmark e validazione dei primi componenti.


# EFFETTO 1: INTERACTIVE GRADIENT MESH FLUID

## ROI
**Molto alto.** È l’effetto con miglior rapporto tra resa visiva, complessità e utilità in UI reali.

## Obiettivo visivo
Gradiente organico in movimento continuo, con transizioni cromatiche morbide e non lineari. Deve funzionare come:

- hero background;
- card premium;
- onboarding;
- paywall;
- empty state;
- visual identity dinamica.

## Modello matematico corretto
L’effetto è basato su rumore procedurale e domain warping.

```txt
p = uv * scale
q = vec2(
  fbm(p + time * velocityA),
  fbm(p + offset + time * velocityB)
)

r = p + warp * q
v = fbm(r + time * velocityC)

color = palette(v)
```

## Rumore consigliato

Per MVP:

- hash noise 2D;
- smooth interpolation;
- fBm a 3-5 ottave;
- domain warping leggero.

Evitare Simplex completo nella prima versione se aumenta troppo codice e rischio bug.

## iOS / Metal

Implementare una `UIView` Fabric che contiene `MTKView`.

Pipeline:

1. Creare `MTLDevice`.
2. Creare `MTLCommandQueue`.
3. Caricare shader dal bundle con `device.makeDefaultLibrary()`.
4. Creare `MTLRenderPipelineState`.
5. Aggiornare uniform buffer con:
   - `u_time`;
   - `u_resolution`;
   - `u_scale`;
   - `u_warp`;
   - palette colori normalizzata.
6. Renderizzare un full-screen triangle o quad.

Nota: preferire full-screen triangle per ridurre overhead e problemi di seam.

## Android / AGSL

Per shader generativo puro usare custom `View`:

```kotlin
private val shader = RuntimeShader(FLUID_SHADER)
private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
  this.shader = shader
}

override fun onDraw(canvas: Canvas) {
  shader.setFloatUniform("u_time", time)
  shader.setFloatUniform("u_resolution", width.toFloat(), height.toFloat())
  canvas.drawPaint(paint)
}
```

Usare `Choreographer` per invalidare la view quando animata.

## Performance target

- 60 FPS su fascia media.
- 120 FPS opzionale su device compatibili.
- massimo 5 ottave fBm.
- evitare loop dinamici in shader.
- palette massimo 6 colori.

---

# EFFETTO 2: SHIMMER CHROME / METALLIC REFLECTION

## ROI
**Alto.** Molto utile per badge premium, CTA, importi, card virtuali, testi hero e paywall.

## Obiettivo visivo
Testo o superficie attraversata da riflessi metallici. L’effetto deve sembrare una superficie lucida, non un semplice gradiente lineare.

## Modello visivo
Per UI 2D non serve un PBR completo. Serve una composizione leggera:

```txt
band = smoothstep(a, b, sin(uv.x * frequency + uv.y * skew + time))
specular = pow(max(band, 0), shininess)
base = metalPalette(uv)
final = base + specular * highlightColor
alpha = textMaskAlpha
```

## Correzione fisica
Il modello di Phong è un buon riferimento storico per specularità, ma in questo effetto il più delle volte non abbiamo vere normali 3D. Quindi il documento deve parlare di:

- Phong-inspired specular;
- fake normal map;
- anisotropic highlight;
- matcap/chrome bands.

## iOS / Metal

Due modalità:

### Modalità A: testo come texture alpha
1. Renderizzare il testo in una texture alpha con CoreText.
2. Passare la texture allo shader Metal.
3. Lo shader usa alpha come maschera.
4. Output: colore chrome solo dove `alpha > 0`.

### Modalità B: overlay su native label
Meno potente, ma più accessibile:

1. `UILabel` nativa per accessibility.
2. `CAMetalLayer` sopra come effetto visuale.
3. Sincronizzare bounds, font metrics e scale.

## Android / AGSL

Per MVP:

- usare `TextView`;
- applicare shader a `TextView.paint.shader`;
- aggiornare uniform in `onDraw`;
- mantenere accessibility della `TextView`.

Attenzione: selezione/copia testo e rendering multi-linea vanno testati. Non promettere “selezionabile” senza test.

---

# EFFETTO 3: LIQUID GLASS BLUR / REFRACTIVE GLASSMORPHISM

## ROI
**Medio-alto, ma con rischio tecnico alto.**

## Obiettivo visivo
Pannello traslucido con blur, highlight sui bordi, rifrazione morbida e deformazione dello sfondo.

## Versione MVP consigliata
Non partire dalla rifrazione reale. Partire da:

1. blur nativo;
2. tint layer;
3. bordo speculare;
4. highlight animato molto leggero;
5. noise/grain sottilissimo per evitare banding.

## Versione avanzata
Rifrazione tramite normal map procedurale:

```txt
height = fbm(uv * scale + time)
normal = normalize(vec3(dFdx(height), dFdy(height), 1.0))
distortedUV = uv + normal.xy * refraction
background = sample(inputTexture, distortedUV)
```

Nota: in AGSL le derivative functions e il sampling vanno verificati rispetto alla sintassi supportata. Non assumere parità piena con GLSL.

## iOS

Scelte:

### Livello 1: fallback robusto
- `UIVisualEffectView`;
- tint overlay;
- border/highlight con CoreAnimation o Metal overlay.

### Livello 2: custom Metal reale
- render offscreen dello sfondo;
- texture input passata allo shader;
- sampling con UV distorti;
- compositing finale.

Questa seconda strada è più costosa e va trattata come fase successiva.

## Android

Scelte:

### Android 12+ / API 31+
- `RenderEffect.createBlurEffect`.

### Android 13+ / API 33+
- `RuntimeShader`;
- `RenderEffect.createRuntimeShaderEffect`;
- `RenderEffect.createChainEffect` per concatenare blur + distorsione.

Limite importante: concatenare effetti sulla gerarchia può costare più di una custom view generativa. Va misurato.

---

# EFFETTO 4: LIQUID CHROME / MERCURIO LIQUIDO

## ROI
**Molto alto per effetto wow, medio per uso quotidiano.**

Questo è l’effetto da aggiungere come elemento distintivo della libreria. È più memorabile del semplice shimmer chrome perché crea una superficie fluida, speculare e organica simile a mercurio, metallo liquido o chrome deformato.

## Riferimento visivo
Superficie argentata molto riflettente, con onde morbide, highlights bianchi intensi, bande scure ad alto contrasto e deformazioni continue. L’immagine allegata è un buon target estetico: non è vetro, non è blur, non è gradiente; è una simulazione stilizzata di superficie metallica riflettente.

## Dove usarlo
- hero background;
- splash screen;
- paywall premium;
- badge “Pro”;
- card fintech;
- artwork generativo;
- loading screen di lusso;
- transizioni tra stati importanti.

Non è ideale per superfici con molto testo sopra: il contrasto può diventare aggressivo.

## Differenza rispetto a Shimmer Chrome
`Shimmer Chrome` è principalmente un highlight che attraversa testo o superficie.

`Liquid Chrome` è una superficie completa, deformata, con normale procedurale, riflessi ambientali finti e specularità ad alto contrasto.

## Modello matematico

L’effetto può essere implementato senza ray tracing usando:

1. height field procedurale;
2. normal map derivata dal campo;
3. fake environment mapping;
4. Fresnel semplificato;
5. contrast curve;
6. highlight speculari.

### Height field

```txt
p = uv * scale

h1 = fbm(p + time * flowA)
h2 = fbm(p * 1.7 + vec2(h1) + time * flowB)

height = h1 * 0.55 + h2 * 0.45
```

### Normal map procedurale

Se lo shader supporta derivative functions:

```txt
dx = dFdx(height)
dy = dFdy(height)
normal = normalize(vec3(-dx * distortion, -dy * distortion, 1.0))
```

Fallback senza derivatives:

```txt
eps = 1.0 / min(resolution.x, resolution.y)
hx = heightAt(uv + vec2(eps, 0.0)) - heightAt(uv - vec2(eps, 0.0))
hy = heightAt(uv + vec2(0.0, eps)) - heightAt(uv - vec2(0.0, eps))
normal = normalize(vec3(-hx * distortion, -hy * distortion, 1.0))
```

### Fake environment mapping

Invece di campionare un ambiente reale, generare bande chrome procedurali:

```txt
reflection = normal.xy * 0.5 + 0.5

bands =
  0.45 * sin(reflection.y * 18.0 + normal.x * 5.0) +
  0.35 * sin(reflection.x * 12.0 + time * 0.35) +
  0.20 * sin((reflection.x + reflection.y) * 22.0)

chrome = smoothstep(0.25, 0.95, bands)
```

### Fresnel semplificato

```txt
viewDir = vec3(0.0, 0.0, 1.0)
fresnel = pow(1.0 - max(dot(normal, viewDir), 0.0), 3.0)
```

### Colore finale

```txt
baseSilver = mix(vec3(0.08), vec3(0.92), chrome)
highlight = pow(chrome, highlightPower) * highlightIntensity
final = baseSilver + highlight + fresnel * edgeTint
final = contrast(final)
```

## Palette varianti

### `silver`
- nero profondo;
- grigio acciaio;
- bianco speculare.

### `mercury`
- argento freddo;
- blu pallido;
- ombre quasi nere;
- highlight bianco latte.

### `blackChrome`
- base molto scura;
- riflessi grigio fumo;
- highlight stretti.

### `rainbowOil`
- base chrome;
- aberrazione colore leggera;
- bande iridescenti controllate.

## Props consigliate

```ts
type LiquidChromeProps = ShaderEffectBaseProps & {
  variant?: 'silver' | 'mercury' | 'blackChrome' | 'rainbowOil';
  scale?: number;          // default 2.4
  flow?: number;           // default 0.35
  distortion?: number;     // default 1.2
  contrast?: number;       // default 1.4
  highlightWidth?: number; // default 0.65
  highlightIntensity?: number; // default 1.1
  grain?: number;          // default 0.015
  interactive?: boolean;
  pointerInfluence?: number;
};
```

## iOS / Metal

Implementare come `MTKView` full-bleed.

Uniform buffer:

```c
struct LiquidChromeUniforms {
  float2 resolution;
  float time;
  float scale;
  float flow;
  float distortion;
  float contrast;
  float highlightWidth;
  float highlightIntensity;
  float grain;
  int variant;
};
```

Shader:

- full-screen triangle;
- funzione `hash21`;
- funzione `noise`;
- funzione `fbm`;
- funzione `heightField`;
- funzione `normalFromHeight`;
- funzione `chromePalette`;
- fragment output `float4(finalColor, 1.0)`.

Ottimizzazione:

- massimo 4 ottave fBm;
- evitare branch pesanti per variante: trasformare la variante in uniform e usare palette precomputate lato CPU quando possibile;
- usare `half` dove accettabile, `float` per coordinate/rumore se il banding diventa visibile.

## Android / AGSL

Implementare come custom `View` generativa, non `RenderEffect`, perché l’effetto non deve filtrare contenuto sottostante.

Pipeline:

```kotlin
class LiquidChromeView(context: Context) : View(context) {
  private val shader = RuntimeShader(LIQUID_CHROME_SHADER)
  private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    this.shader = shader
  }

  override fun onDraw(canvas: Canvas) {
    shader.setFloatUniform("u_resolution", width.toFloat(), height.toFloat())
    shader.setFloatUniform("u_time", currentTimeSeconds)
    shader.setFloatUniform("u_scale", scale)
    shader.setFloatUniform("u_flow", flow)
    shader.setFloatUniform("u_distortion", distortion)
    canvas.drawPaint(paint)
  }
}
```

Fallback Android < API 33:

1. fallback statico con `GradientDrawable` + bitmap noise opzionale;
2. oppure OpenGL ES solo se la libreria accetta una complessità maggiore;
3. non bloccare l’adozione della libreria per supportare subito shader animati sotto API 33.

## Interazione touch opzionale

L’effetto può reagire al puntatore senza coinvolgere JS frame-by-frame.

Native props:

```ts
interactive?: boolean;
pointerInfluence?: number;
```

Comportamento:

- Android: gestire `onTouchEvent`, aggiornare uniform `u_pointer`.
- iOS: gestire `touchesMoved`, aggiornare uniform `pointer`.
- decay nativo verso centro quando il touch termina.

Formula:

```txt
p = uv + pointerInfluence * falloff(distance(uv, pointer)) * direction
```

## Rischi tecnici

### Banding
Molto probabile su gradienti metallici larghi.

Mitigazione:

- dithering/grain molto sottile;
- noise ad alta frequenza;
- evitare curve troppo piatte;
- usare framebuffer con formato colore adeguato se possibile.

### Consumo energetico
Effetto full-screen sempre animato può consumare.

Mitigazione:

- pause quando fuori viewport;
- ridurre FPS se low power mode;
- `paused` prop;
- stop display link quando view non visibile;
- animazione lenta di default.

### Leggibilità
Chrome ad alto contrasto può disturbare il testo.

Mitigazione:

- overlay scrim opzionale;
- variante `blackChrome`;
- blur leggero o vignette;
- non usarlo sotto body text.

## Priorità MVP Liquid Chrome

1. `LiquidChromeView` full-screen su Android API 33+.
2. `LiquidChromeView` full-screen su iOS Metal.
3. Props: `variant`, `speed`, `intensity`, `scale`, `distortion`, `paused`.
4. Fallback statico.
5. Demo screen con:
   - hero background;
   - premium card;
   - masked text/logo opzionale.
6. Test performance:
   - Android fascia media;
   - iPhone 60Hz;
   - iPhone ProMotion.

---

# CLASSIFICA ROI / COMPLESSITÀ AGGIORNATA

| Effetto | Wow | Utilità reale | Complessità | Rischio | Priorità |
|---|---:|---:|---:|---:|---:|
| Interactive Gradient Mesh Fluid | Alto | Molto alta | Media | Medio | 1 |
| Liquid Chrome / Mercurio Liquido | Molto alto | Media | Media-alta | Medio | 2 |
| Shimmer Chrome / Metallic Text | Alto | Alta | Media | Medio-basso | 3 |
| Liquid Glass / Refractive Glass | Molto alto | Media | Alta | Alto | 4 |

## Lettura critica
Liquid Glass è forse l’effetto più “di moda”, ma è anche quello con più trappole tecniche. Se l’obiettivo è una libreria pubblicabile e stabile, partirei da:

1. **Fluid Gradient**
2. **Liquid Chrome**
3. **Chrome Text**
4. **Liquid Glass avanzato**

---

# ROADMAP IMPLEMENTATIVA

## Fase 1 — Core shader runtime

- creare modulo Fabric;
- creare base native view iOS/Android;
- aggiungere display loop nativo;
- gestire lifecycle:
  - mounted;
  - attached;
  - detached;
  - app background;
  - reduce motion;
  - low power.

## Fase 2 — Fluid Gradient

- MVP generativo;
- palette props;
- fBm;
- domain warping;
- dithering.

## Fase 3 — Liquid Chrome

- height field;
- normal map;
- chrome bands;
- varianti palette;
- fallback statico.

## Fase 4 — Chrome Text

- Android TextView paint shader;
- iOS texture alpha da CoreText oppure overlay Metal;
- test multilinea;
- test accessibility.

## Fase 5 — Liquid Glass

- fallback blur nativo;
- highlight layer;
- solo dopo: refraction custom con texture input.

---

# TEST E VALIDAZIONE

## Test visivi

Ogni shader deve supportare `debugTime` per generare output deterministico.

```ts
<LiquidChromeView debugTime={1.25} />
```

Generare snapshot per:

- 320x180;
- 390x844;
- 768x1024;
- dark/light background;
- intensità minima/massima.

## Test performance

Metriche minime:

- frame time medio;
- frame time p95;
- dropped frames;
- CPU usage;
- GPU usage se disponibile;
- memory allocation durante animazione;
- consumo stimato con animazione 60s.

## Test compatibilità

Android:

- API 33+ AGSL;
- API 31-32 fallback;
- API <31 fallback statico;
- GPU Mali, Adreno, Tensor.

iOS:

- iOS 15+ Metal;
- device 60Hz;
- device ProMotion;
- simulator solo per smoke test, non per benchmark.

---

# FONTI TECNICHE DA VERIFICARE DURANTE IMPLEMENTAZIONE

- Android `RuntimeShader` e AGSL: https://developer.android.com/reference/android/graphics/RuntimeShader
- Android AGSL overview: https://developer.android.com/develop/ui/views/graphics/agsl
- Android AGSL usage with `RenderEffect`: https://developer.android.com/develop/ui/views/graphics/agsl/using-agsl
- Android `RenderEffect`: https://developer.android.com/reference/android/graphics/RenderEffect
- Apple Metal: https://developer.apple.com/metal/
- Apple `MTKView`: https://developer.apple.com/documentation/metalkit/mtkview
- Apple Metal Shading Language Specification: https://developer.apple.com/metal/Metal-Shading-Language-Specification.pdf
- Perlin, K. “An Image Synthesizer”, SIGGRAPH 1985.
- Phong, B. T. “Illumination for Computer Generated Pictures”, Communications of the ACM, 1975.
- Kawase, M. “Frame Buffer Postprocessing Effects in DOUBLE-S.T.E.A.L.”, GDC 2003.
- McEwan, Sheets, Gustavson, Richardson. “Efficient computational noise in GLSL”, 2012.

---

# CONCLUSIONE OPERATIVA

Il documento originale è valido come direzione estetica, ma aveva alcune affermazioni troppo assolute e alcuni dettagli tecnici da correggere:

- AGSL e Metal non vanno trattati come equivalenti;
- Simplex Noise non va confuso con Perlin Noise;
- Liquid Glass non va stimato come effetto semplice;
- `RenderEffect` non deve essere la soluzione predefinita per ogni shader Android;
- Liquid Chrome merita una sezione autonoma perché è un effetto diverso dal semplice shimmer.

La versione consigliata della libreria deve partire da effetti generativi puri, perché sono più controllabili, performanti e pubblicabili:

```txt
Fluid Gradient -> Liquid Chrome -> Chrome Text -> Liquid Glass avanzato
```
