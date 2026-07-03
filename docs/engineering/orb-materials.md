# Engineering Design Document: Orb 3D Materials

**Project:** Material × Motion × Skin (v2)
**Target Platforms:** Android (AGSL / RuntimeShader) & iOS (Metal)
**Status:** Ready for implementation
**Autorità:** documento di implementazione dei tre material orb (`metal`, `water`,
`iridescent`). L'API pubblica vive in
[../architecture/material-motion-skin.md](../architecture/material-motion-skin.md);
le tecniche generali in [shader-techniques.md](shader-techniques.md).

---

## 0. Riconciliazione con lo stato attuale (leggere prima)

Questo EDD è il TARGET. Divergenze note dal codice attuale, da chiudere nelle
milestone qui sotto:

- **Naming**: i material pubblici sono `metal` / `water` / `iridescent` (vedi
  `MaterialName`). L'attuale componente `MaterialOrb` usa ancora i nomi vecchi
  `liquidChrome`/`liquidGlass`/`iridescentGlass` (mode 0/1/2 dell'AGSL) e va
  rimosso nella migrazione a `MaterialView` (R2).
- **Silhouette**: l'AGSL attuale calcola la silhouette DENTRO lo shader (SDF +
  wobble). L'EDD la vuole NATIVA (Path/Stencil). Coerente con la milestone:
  resta in-shader come scaffolding fino alla **Fase 4** (skin+mask), poi esce.
- **Motion**: R1 ha aggiunto le uniform `u_motion*` allo spec Nitro, ma lo shader
  legge ancora le vecchie `u_speed/u_wobble/u_distortion/u_detail`. La migrazione
  a `u_motion*` è parte di Fase 2/4.
- **iOS**: nessuno shader orb in Metal ancora (con `materialOrb` iOS disegna solo
  il `solid` nero). È la Fase 3.

---

## 1. Vincoli architetturali

1. **Silhouette esterna allo shader** (target): il material calcola *solo* il
   colore del pixel sul quad; la forma (bolla/blob/sfera/testo/svg) è ritagliata
   da maschere native (Path/Canvas su Android, Stencil su iOS).
2. **Motion layer**: `speed`, `warp`, `amplitude`, `detail`, `seed` arrivano da un
   ticker nativo come uniform, uguali su entrambe le piattaforme.
3. **No derivative functions**: AGSL non ha `dFdx`/`dFdy`. Le normali di
   superficie si calcolano con **differenze finite** (step `eps` legato alla
   risoluzione passata come uniform). Su Metal si usa lo stesso metodo per parità.
4. **Performance**: 60 FPS fascia media. Max 4–5 ottave fBm. Evitare branch
   dinamici pesanti (preferire `mix`/`clamp`/`smoothstep`). Palette limitata.

---

## 2. L'ingrediente base: normale sferica analitica

Tutti e tre i material derivano l'ottica da una normale 3D generata dal pixel del
quad (camera ortografica).

```glsl
// uv centrato sul quad, in [-1, 1]
vec2 p = uv;
float z = sqrt(1.0 - clamp(dot(p, p), 0.0, 1.0));
vec3 N  = normalize(vec3(p.x, p.y, z));
vec3 V  = vec3(0.0, 0.0, 1.0);                       // vista ortografica
float fresnel = pow(1.0 - max(dot(N, V), 0.0), 5.0); // Schlick
```

`N` va poi perturbata da un height field a bassa frequenza (piccola ampiezza) per
il moto di superficie — vedi §3.2 per le differenze finite.

---

## 3. Specifica dei tre material

### 3.1 `iridescent`

**Target shippato = perla opaca** (come `references/materials/iridescent.png`):
base lattiginosa opaca al centro, bande arcobaleno morbide concentrate verso il
bordo via fresnel. È la variante che implementiamo di default (decisione Giulio,
2026-07-03: si segue la ref, non la bolla trasparente).

La variante **bolla di sapone trasparente** qui sotto resta documentata come
opzione (centro trasparente) ma NON è il default: usare `alpha` opaco (~1) e una
base perlacea invece di `mix(vec3(0.0), iriCol, fresnel)` se si vuole la perla.

```glsl
// thin-film interference
float thickness = fresnel * 0.8 + 0.2;
vec3  iriCol    = 0.5 + 0.5 * cos(6.28318 * (thickness + vec3(0.0, 0.33, 0.67)));
// highlight Blinn-Phong
float spec  = pow(max(dot(N, normalize(L + V)), 0.0), 80.0);
// alpha: centro trasparente, bordi opachi
float alpha = 0.1 + fresnel * 0.9;
vec3  final = mix(vec3(0.0), iriCol, fresnel) + vec3(spec);
```

Trappola: concentrare l'iridescenza troppo sul rim estremo produce un anello
sottile e aliasato → allargare le bande (frequenza più bassa) e sfumarle via dal
bordo estremo.

### 3.2 `metal` / liquidMetal (cromo / mercurio)

Metallo liquido deformato, riflessi contrastati chiaro/scuro.

```glsl
// perturbazione della normale: muovere la superficie, non la sfera
float height = fbm(p * u_motionScale + u_time * u_motionSpeed);
float eps = 1.0 / min(uResolution.x, uResolution.y);
float hx  = fbm((p + vec2(eps, 0.0)) * u_motionScale + u_time * u_motionSpeed) - height;
float hy  = fbm((p + vec2(0.0, eps)) * u_motionScale + u_time * u_motionSpeed) - height;
vec3 deformN = normalize(vec3(-hx * u_motionWarp, -hy * u_motionWarp, 1.0));
vec3 finalN  = normalize(mix(N, deformN, 0.5));

// fake environment (matcap / bande chrome) campionato da finalN.xy
float env = finalN.y * 0.5 + 0.5;                 // gradiente alto/basso (studio)
env += sin(finalN.x * 30.0 + u_time * 0.5) * 0.1; // striature metalliche
vec3 final = mix(vec3(0.05), vec3(1.0), env);     // grigio scuro → bianco
final += fresnel * 0.5;                            // fresnel bianco ai bordi
```

Per un look più "studio softbox" invece di bande dure: sostituire il gradiente
lineare con una env a due stop (pavimento → mid → cielo) + key light larga in
alto (vedi implementazione AGSL corrente `studioEnv`).

### 3.3 `water` / liquidGlass (gel azzurro)

Acqua/gel translucido con subsurface scattering (luce che esce dai bordi).

```glsl
// SSS fake via rim
float rim = 1.0 - max(0.0, dot(N, V));
float sss = pow(rim, 3.0) * 0.6;

vec3 baseColor = vec3(0.3, 0.6, 0.8);   // blu desaturato (centro)
vec3 glowColor = vec3(0.5, 0.9, 1.0);   // ciano brillante (bordi)

// rifrazione finta: distorce le coord di campionamento col normale
vec2 uv_distorted = uv + N.xy * 0.02;

float alpha = mix(0.2, 0.9, rim);       // centro più trasparente dei bordi
vec3  final = mix(baseColor, glowColor, sss);
```

---

## 4. Pipeline di implementazione

### 4.1 Android (AGSL — RuntimeShader)

- API 33+; fallback texture statica < API 33.
- `MaterialView` disegna un `Rect` con `Canvas` + `Paint(RuntimeShader)`.
- Uniform: `u_time` (Choreographer), `u_resolution`, `u_motionSpeed/Amp/Warp/Detail/Seed`.
- NON usare `dFdx`/`dFdy` → differenze finite (§3.2). Preferire `mix`/`clamp`.

### 4.2 iOS (Metal / MetalKit)

- `MTKView` + `CAMetalLayer`; shader `.metal` nella default library.
- `MTLBuffer` con struct C allineata alle STESSE uniform di Android.
- Alpha blend via `MTLRenderPipelineDescriptor` (blending enabled).
- Silhouette via **stencil buffer**: disegnare il Path/Bezier nello stencil, poi
  il quad shaderizzato solo dove lo stencil è marcato.

---

## 5. Milestone (DoD)

| Fase | Descrizione | Criteri di accettazione |
|---|---|---|
| **F1 — Shader puro** | Fragment shader unico che dimostra normale sferica + 3 material, `u_time` fermo. | Le 3 ref riproducibili staticamente su un quad. |
| **F2 — Android** | Integrazione in RuntimeShader AGSL, differenze finite, `Canvas.drawRect`, consumo delle `u_motion*`. | 60 FPS su Pixel 6-class, nessun errore AGSL, validazione visiva Giulio. |
| **F3 — iOS** | Traduzione in MSL, `MTKView`, uniform buffer. | 60 FPS su iPhone 12+, output ≈95% identico ad Android a parità di parametri. |
| **F4 — Skin & Motion** | Uniform dal ticker Motion; maschera nativa silhouette (Path Android / Stencil iOS). | Forma ritagliata senza artefatti ai bordi. |

Mappatura sulla roadmap del repo (STACK.md): F2↔R2, F3↔R5, F4↔R2/R3/R4. F1 è un
passo di de-risking opzionale (prototipo su Shadertoy) prima di R2.

---

## 6. Trappole da evitare

- **Banding** gradienti: dithering a fine shader `final += (hash(uv+u_time)-0.5)/255.0`.
- **Tempo**: `u_time` float32 dal ticker nativo ogni frame. MAI passare il tempo
  dal thread JS.
- **DebugTime**: `u_debugTime` per snapshot deterministici (già presente come
  `debugTime` nel contratto). In debug il tempo esterno sovrascrive il clock.
- **Seed**: `u_motionSeed` fisso per consistenza visiva iOS↔Android; variabile solo
  se serve rumore diverso per device.

---

## 7. Riferimenti

- AGSL: https://developer.android.com/develop/ui/views/graphics/agsl
- Metal Shading Language: https://developer.apple.com/metal/Metal-Shading-Language-Specification.pdf
- MatCap (metal): https://github.com/hughsk/matcap
- Thin-film / glass (iridescent, water): Shadertoy `XdVfDd` (glass sphere refraction), `4sXGRj` (soap bubble).
