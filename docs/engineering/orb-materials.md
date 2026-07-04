# Engineering: Orb 3D Materials (mini-PBR + IBL)

**Project:** Material × Motion × Skin (v2)
**Platforms:** Android (AGSL / RuntimeShader) — validato. iOS (Metal) — da portare.
**Status:** implementato e validato su Android (2026-07-04).
**Autorità:** documento tecnico dell'engine dei tre material orb (`metal`, `water`,
`iridescent`). API pubblica: [../architecture/material-motion-skin.md](../architecture/material-motion-skin.md).
Percorso e vicoli ciechi: [../process/ORB_MATERIALS_JOURNEY.md](../process/ORB_MATERIALS_JOURNEY.md).

> Storia: gli approcci procedurali (environment finto con `sin`, matcap
> procedurale, raymarch senza ambiente reale) sono stati abbandonati perché non
> producono riflessi naturali. Vedi il JOURNEY. Questo documento descrive SOLO
> l'architettura vincente.

---

## 1. Idea: come Blender/Eevee, in real-time

Una sfera resa come un **Principled BSDF con Image-Based Lighting**: riflette e
rifrange un **HDRI di studio reale**. L'ambiente è separato dal material; il
material è un **preset di parametri PBR**. È la stessa filosofia con cui i material
BlenderKit ottengono il loro look (dal World HDRI).

File: `android/src/main/assets/shaders/material-orb.agsl`,
`android/src/main/assets/env/studio.png` (equirettangolare, CC0 Poly Haven).

## 2. Environment (IBL)

- `uniform shader u_env;` + `uniform float2 u_envSize;` — l'HDRI equirettangolare
  passato da Kotlin come `BitmapShader` (`RuntimeShader.setInputShader`, tile REPEAT
  in U / CLAMP in V).
- Campionamento da direzione 3D → equirettangolare:
  ```glsl
  u = atan(dir.z, dir.x) / (2π) + 0.5;
  v = acos(clamp(dir.y,-1,1)) / π;      // 0 = alto
  color = u_env.eval(vec2(u,v) * u_envSize).rgb;
  ```
- `sampleEnvSoft(dir, rough)`: 5 tap tangenti per riflessi più morbidi (roughness).

## 3. Geometria 3D + superficie viva

- Normale sferica analitica: `z = sqrt(1-|p|²)`, `p = uv/0.81` (per riempire la
  maschera nativa, raggio Path `0.405*min`). `uv.y` flippato (AGSL Y-down).
- **Rilievo con noise 3D SULLA superficie** `sp = (p.x, p.y, z)` — non 2D sul piano.
- Il dominio del noise **ruota nel tempo** su assi Y+X (`rotate3`, `reliefRot`) →
  la skin orbita in tutte le direzioni.
- `perturbNormal3`: gradiente 3D → componente **tangenziale** (pieghe) + **bulge
  radiale** lungo la normale ("spinta da dentro").

## 4. I tre material (preset PBR)

| Material | Modello | Parametri chiave |
|---|---|---|
| `metal` (mode 0) | metallico: riflessione a specchio dell'IBL, tinta base, fresnel | reflection = `sampleEnvSoft(R)`, tint via `u_materialColor` |
| `water` (mode 1) | dielettrico: `transmission` (refract → vede attraverso), reflection via Fresnel, rim + highlight finestre | `ior 1.33`, body tint azzurro, thickness ∝ profondità |
| `iridescent` (mode 2) | thin-film su dielettrico (bolla di sapone) | `0.5+0.5*cos(2π*(band + RGBoffset))`, band ∝ fresnel + rilievo |

Tonemap morbido finale + dithering per evitare banding.

## 5. Uniform (contratto attuale)

Consumati dallo shader (Kotlin li setta in `setMaterialOrbUniforms`):
`u_env`, `u_envSize`, `u_resolution`, `u_time`, `u_orbMaterial`,
e i parametri via `resolved(motion, legacy)` = `max(u_motion*, u_legacy)`:
`speed`, `wobble`→amp, `distortion`→warp, `detail`, `u_motionSeed`, `u_materialColor`.

Nota: la **rotazione per-asse** è per ora hardcoded (`t*0.30` / `t*0.42`). Va esposta
come parametro nella parametrizzazione (vedi HANDOFF → prossimi passi).

## 6. Skin (silhouette) e ombra — native

La silhouette organica (Path wobbly) e l'ombra ellittica sono disegnate da Kotlin
(`buildMaterialOrbPath`, `drawMaterialOrbShadow`), non dallo shader. Lo shader
riempie il Path (`drawPath`) e restituisce `alpha = 1`.

## 7. iOS (da fare)

Portare l'engine in Metal: `MTKView`, l'HDRI come `MTLTexture` campionata allo
stesso modo, `refract`/`reflect` nativi, silhouette via stencil/Path. Stessi
parametri e stesso env per parità visiva con Android.

## 8. Trappole (dal JOURNEY)

- AGSL `fragCoord.y` è verso il basso → flippare `uv.y`.
- `uv` va scalato a `0.5*size` o la sfera è una calotta.
- Non usare nomi di built-in SkSL (`floor`) come variabili.
- Output straight-alpha (no premoltiplicazione) per evitare aloni sul bordo.
- Performance: noise 3D + gradiente a 4 tap è il costo dominante; tenere fBm3 a 3
  ottave, orb piccoli. Ottimizzare l'HDRI (WebP / 512×256) prima della pubblicazione.

## Riferimenti

- Reference visive: `docs/references/materials/` (+ video @jc_builds).
- IBL/matcap: https://github.com/hughsk/matcap · Poly Haven (HDRI CC0): https://polyhaven.com
- AGSL: https://developer.android.com/develop/ui/views/graphics/agsl
- Metal Shading Language: https://developer.apple.com/metal/Metal-Shading-Language-Specification.pdf
