# Condivisione shader tra Android (AGSL) e iOS (Metal)

Analisi e strategia per **non riscrivere la matematica due volte** (richiesta di
Giulio, Fase 0). Conclusione operativa in fondo.

## Vincolo di fondo: dove gira il codice

- La matematica degli shader gira sulla **GPU**: AGSL/SkSL su Android, MSL (Metal)
  su iOS. Il **C++/Nitro gira sulla CPU**. → La matematica GPU **non** può essere
  scritta in C++ e usata dalla GPU. C++ resta utile solo per logica CPU condivisa
  (oggi coperta dal TypeScript, che gira uguale su iOS e Android).

## Buona notizia: SkSL e MSL sono quasi uguali per le funzioni pure

SkSL (AGSL) usa gli stessi nomi di tipo di Metal — `float2`, `float3`, `float4`,
`half4` — e le stesse builtin: `mix`, `clamp`, `pow`, `fract`, `floor`, `dot`,
`normalize`, `reflect`, `refract`, `cross`, `atan(y,x)`, `acos`, `sin`, `cos`,
`sqrt`. Quindi le **funzioni pure** (hash, noise, fbm 2D/3D, `rotate3`, fresnel,
relief, perturbazione normale) sono **portabili quasi verbatim** tra le due.

Le differenze NON stanno nella matematica, ma nell'**I/O**:

| Aspetto | AGSL / SkSL | Metal / MSL |
|---|---|---|
| Entry point | `half4 main(float2 fragCoord)` | `fragment float4 f(VertexOut in [[stage_in]], ...)` |
| Uniform | `uniform float2 u_x;` | campo di una `struct` in un `constant ...& [[buffer(0)]]` |
| Env texture | `uniform shader u_env; u_env.eval(px)` | `texture2d<float>` + `sampler`, `env.sample(s, uv)` |
| Preprocessore | **assente** (no `#define`/`#include`) | presente, ma non necessario |

## Strategia (nessun C++, nessun transpiler pesante)

Poiché la matematica è già portabile e le differenze sono solo di I/O:

1. **File "core" condivisi** — le funzioni pure vivono in file `.core` (uno per
   modulo: `noise.core`, `ibl.core`, `geometry.core`, e un `orb.core` con la logica
   dei material). Sono scritti nel sottoinsieme comune SkSL↔MSL e si usano **come
   sono** su entrambe le piattaforme (copia/concatenazione, non riscrittura).
2. **Adapter di I/O per piattaforma** (piccoli): definiscono `main`/entry, le
   uniform, e wrappano il campionamento dell'environment:
   - `sampleEnv(dir)` è l'unico punto che diverge → in AGSL chiama `u_env.eval`, in
     MSL `env.sample`. Si isola in un file `env-sample.agsl` / `env-sample.metal`.
3. **Composizione** — il nativo (o un build step) concatena
   `adapter-piattaforma + core condivisi + entry` per ottenere il source finale.
   Su Android la composizione è a runtime (Kotlin legge i moduli); su iOS lo stesso
   a compile/build time.

Così la matematica si scrive **una volta** (i `.core`); per aggiungere iOS si
scrivono solo i due adapter di I/O, non la matematica.

### Perché NON un transpiler / non C++
- Transpiler (GLSL→SPIRV→MSL) = dipendenze e CI per un guadagno nullo, dato che
  SkSL e MSL sono già quasi identici per le funzioni pure.
- C++ = dominio CPU, non applicabile alla GPU.

## Stato e sequenza

- **Ora (Fase 0)**: refactor SOLID lato registry (TS + Kotlin) + questa strategia
  documentata. Gli shader Android restano invariati (parità visiva).
- **Modularizzazione fisica in `.core` + adapter**: si fa insieme al **porting iOS**
  (l'unico secondo consumatore che rende reale la condivisione). Predisporre prima
  file `.core` senza iOS sarebbe infrastruttura senza consumatore.
- Riferimento material orb: [orb-materials.md](orb-materials.md).
