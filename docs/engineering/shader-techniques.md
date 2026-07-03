# Shader techniques

Reference tecnico per lo shading procedurale usato dai material. Distillato dal
vecchio spec originale (rimosso): qui resta SOLO la parte tecnica ancora valida.
L'architettura pubblica (nomi, API, roadmap) NON vive qui — vedi
[../architecture/material-motion-skin.md](../architecture/material-motion-skin.md).

## Piattaforme: AGSL ≠ Metal

- **Android**: AGSL definito come stringa a runtime. Per uno shader generativo
  puro → custom `View` + `Canvas.drawRect(Paint(shader))`. `RuntimeShader` da
  API 33+; sotto API 33 serve un fallback statico. Usare `RenderEffect
  .createRuntimeShaderEffect` SOLO quando si deve filtrare il contenuto già
  renderizzato di una view (glass/backdrop), non per i material generativi.
- **iOS**: pipeline Metal esplicita — `MTLDevice`, `MTLCommandQueue`,
  `MTLRenderPipelineState`, uniform buffer, `MTKView`/`CAMetalLayer`. Gli shader
  `.metal` vanno inclusi nel bundle e compilati nella default library (debito
  attuale: alcuni fragment sono compilati da stringa inline — vedi HANDOFF).
- AGSL non ha derivative functions (`dFdx`/`dFdy`/`fwidth`): approssimare le
  normali con differenze finite a step `eps` legato alla risoluzione.
- Simplex ≠ Perlin. Per 2D mobile spesso basta **hash noise + fBm + domain
  warping**: più semplice e prevedibile di un Simplex completo.

## Regole di runtime

1. JS monta il componente e passa props serializzabili; nessun frame guidato da
   `setState`/Reanimated/timer JS.
2. L'animazione vive lato nativo (CADisplayLink su iOS, Choreographer su Android).
3. Ogni material supporta `debugTime` per output deterministico (snapshot test).
4. Fallback statico per: Android < API 33, reduce motion, low power, GPU debole.
5. Color parser, clock nativo, shader registry e pipeline cache sono condivisi.
6. Performance target: 60 FPS fascia media, ≤ 4–5 ottave fBm, evitare branch/loop
   dinamici pesanti in shader, palette ≤ 6 colori.

## Noise / fBm

```txt
hash21(p)   -> pseudo-random da coord 2D
noise(p)    -> value noise con smoothstep interpolation
fbm(p)      -> somma di 3-5 ottave di noise, ampiezza decrescente
```

## Domain warping (base di fluidGradient)

```txt
p = uv * scale
q = vec2(fbm(p + time * velA), fbm(p + offset + time * velB))
r = p + warp * q
v = fbm(r + time * velC)
color = palette(v)
```

L'effetto è una simulazione **estetica** cinematica, non un solver fisico di
fluidi. Nessun Navier-Stokes.

## Superficie metallica (base di metal / liquidMetal)

Senza ray tracing, comporre:

1. **height field** procedurale da fBm.
2. **normal map** derivata dal campo (differenze finite in AGSL).
3. **fake environment mapping** con bande chrome procedurali.
4. **Fresnel semplificato**.
5. contrast curve + highlight speculari.

```txt
# normal senza derivatives (AGSL)
eps = 1.0 / min(resolution.x, resolution.y)
hx  = heightAt(uv + vec2(eps,0)) - heightAt(uv - vec2(eps,0))
hy  = heightAt(uv + vec2(0,eps)) - heightAt(uv - vec2(0,eps))
normal = normalize(vec3(-hx * distortion, -hy * distortion, 1.0))

# fake environment (bande chrome)
reflection = normal.xy * 0.5 + 0.5
bands = 0.45*sin(reflection.y*18 + normal.x*5)
      + 0.35*sin(reflection.x*12 + time*0.35)
      + 0.20*sin((reflection.x+reflection.y)*22)
chrome = smoothstep(0.25, 0.95, bands)

# fresnel + colore
fresnel = pow(1.0 - max(dot(normal, vec3(0,0,1)), 0.0), 3.0)
base    = mix(dark, light, chrome)
final   = contrast(base + highlight*pow(chrome, k) + fresnel*edgeTint)
```

### Rischi noti dei metallici

- **Banding** su gradienti larghi → grain/dithering sottile, noise ad alta
  frequenza, evitare curve piatte.
- **Consumo**: full-screen sempre animato → pause fuori viewport, `paused`,
  stop del clock quando non visibile, animazione lenta di default.
- **Leggibilità**: chrome ad alto contrasto disturba il testo sopra.

## Orb 3D: sfere metallo / vetro / iridescenti

Spec autoritativa per i tre material orb (normale sferica analitica, env
reflection procedurale, fresnel, specular, thin-film, alpha/SSS, milestone e
DoD): **[orb-materials.md](orb-materials.md)** (Engineering Design Document).

Anti-pattern (l'errore della prima versione): colorare con noise 2D senza usare la
normale per env/fresnel/specular → esce un disco piatto marmorizzato, non una sfera.

## Skin: come mascherare (testo / svg)

La silhouette NON sta nello shader (vedi architettura). Il masking vive nel
layer nativo di disegno:

- **Android**: `Canvas.drawText` / `Canvas.drawPath` con lo stesso `Paint`
  shaderizzato; oppure `TextView.paint.shader` per testo con accessibility.
- **iOS**: rasterizzare testo/path in una alpha texture (CoreText / path) e usarla
  come mask/stencil sul quad Metal, oppure overlay su label nativa per
  accessibility.

## Fonti da verificare durante l'implementazione

- Android RuntimeShader / AGSL: https://developer.android.com/reference/android/graphics/RuntimeShader
- AGSL overview: https://developer.android.com/develop/ui/views/graphics/agsl
- RenderEffect: https://developer.android.com/reference/android/graphics/RenderEffect
- Apple Metal: https://developer.apple.com/metal/ · MTKView: https://developer.apple.com/documentation/metalkit/mtkview
- Metal Shading Language spec: https://developer.apple.com/metal/Metal-Shading-Language-Specification.pdf
