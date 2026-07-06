# Reference: Blender "Dispersion Glass" (ground truth per material `glass`)

**Tipo:** cattura ground-truth di un materiale Blender fornito da Giulio.
**Scopo:** definire il target canonico del material `glass` del runtime nativo.
**Engine sorgente:** **EEVEE** (Blender 5.1.2) — non Cycles.
**Asset:** node group BlenderKit *"Dispersion glass shader"* + World HDRI
*"Studio Lighting FREE HDRI"* (`virtual-phot…c786d32.exr`).
**Target visivo correlato:** [`materials/glass.webp`](materials/glass.webp).
**Data cattura:** 2026-07-06.

> Nota licenze: il node group e l'HDRI sono asset BlenderKit "FREE". Come per
> `mercury-env` (vedi [ASSET-LICENSES.md](ASSET-LICENSES.md)), vanno trattati come
> **solo-sviluppo** finché la licenza non è verificata; env proprio o CC0 prima
> della pubblicazione.

---

## 1. Come l'abbiamo ottenuto (processo)

La cattura è stata fatta a schermo, senza `.blend`, ricostruendo il grafo dagli
screenshot dell'editor. Procedura ripetibile:

1. **Selezione oggetto** — nel viewport, click sulla sfera (`Sphere` in Outliner);
   materiale `Glass` assegnato allo slot.
2. **Shader Editor** — tab in alto **Shading**; l'area bassa mostra il node tree.
   `Home` (mouse sui nodi) per inquadrare tutto.
3. **Parametri esposti** — il materiale è un **node group** BlenderKit
   *"Dispersion glass shader"*: si leggono solo gli input esposti (tabella §3.4).
4. **Interno del group** — nodo group selezionato, `Tab` per entrarci, `Home` per
   inquadrare: si rivela il grafo reale (dispersione + displacement, §3.1–§3.3).
5. **Engine** — Properties → *Render Properties* → **Render Engine = EEVEE**.
6. **Modificatori** — Properties → *Modifiers* (chiave inglese) → **lista vuota**:
   la forma "fusa" NON è scolpita né da un Displace modifier, è **tutta materiale**
   (Displacement dello shader sulla UV-sphere fitta).

Conclusione di processo: forma e materiale coincidono. Non serve il `.blend` per il
target — bastano i parametri qui sotto + l'HDRI equivalente.

---

## 2. Meccanismo del look (perché appare così)

Il marmo bianco/nero **non è colore dipinto**. È vetro trasparente incolore la cui
**superficie è deformata** da una Wave Texture; questa superficie mossa **rifrange
il World HDRI** (bianco luminoso in alto, scuro intorno). Il bianco/nero visibile è
l'ambiente visto attraverso il vetro. La dispersione dà il sottile bordo cromatico.

Tre ingredienti, in ordine di peso sul look:
1. **Environment (HDRI studio)** — dominante. Senza, il vetro è "vuoto".
2. **Displacement (Wave Texture)** — la forma colata e lo swirl.
3. **Dispersione (3× refraction RGB)** — l'accento cromatico ai bordi.

---

## 3. Spec certificata del node group

### 3.1 Dispersione — 3× `Refraction BSDF` (Beckmann), ricombinati con Mix Shader (Factor 0.5)

| Ramo | Color | IOR |
|---|---|---|
| R | Rosso puro | **1.400** |
| G | Verde puro | **1.150** |
| B | Blu puro   | **1.500** |

Il trucco: separare la luce in R/G/B, rifrangere ogni canale a IOR leggermente
diverso, ricomporre. La spread di IOR = quantità di dispersione.

### 3.2 Base — `Glass BSDF`
- Distribution **Beckmann**, IOR **1.450**, **Thin Film** attivo.
- `Roughness` pilotata da input group *Glass roughness* (0.0).

### 3.3 Forma / swirl — `Wave Texture` → `Displacement`
- **Wave Texture**: profilo **Bands**, waveform **Sine**,
  Detail **2.0**, Detail Scale **1.0**, Detail Roughness **0.5**, Phase **0.0**.
  Scale ← *Pattern scale*, Distortion ← *Pattern distortion*.
- **Displacement**: **Object Space**, Midlevel **0.5**, Scale ← *Displacement strength*.
- Vettore: `Texture Coordinate` → `Mapping` (Point, Location/Rotation 0, Scale 1)
  → input *Mapping* del group.

### 3.4 Input esposti del group (valori del preset catturato)

| Input | Valore |
|---|---|
| Glass color | bianco |
| Dispersion roughness | **0.300** |
| Glass roughness | **0.000** |
| Pattern scale | **1.000** |
| Pattern distortion | **20.000** |
| Displacement strength | **0.100** |

---

## 4. World / HDRI

- Surface = **Background**, texture **Equirectangular**, Strength **1.000**,
  Color Space *Linear Rec.709*.
- **È parte integrante del target**: il bianco/nero del vetro È questo HDRI.
- Env di riferimento definitivo: **Poly Haven `wooden_studio_08` (CC0)** — soffitto
  azzurro + pannelli softbox bianchi + resto scuro (da qui il vetro viola/blu).
  (Il primo screenshot usava l'asset BlendKit "Studio Lighting FREE HDRI"
  `virtual-phot…c786d32.exr`, poi sostituito con l'equivalente CC0.)
- **Nel runtime è già cablato**: `env/glass-env.png` (wooden_studio_08 2K, tonemap
  Hable → PNG equirect 1024×512 via ffmpeg), env di default del material `glass`.
  Stesso env in Blender e nel runtime → confronto lecito.

---

## 5. Traduzione al runtime nativo (note, non implementazione)

EEVEE è già un rasterizzatore con rifrazione screen-space: il fragment shader
nativo può avvicinarsi molto. Mappatura di massima:

| Ingrediente Blender | Runtime (AGSL/Metal) |
|---|---|
| World HDRI | `u_env` equirect (env dedicato, es. nuovo `lab-N`) |
| Glass BSDF (IOR 1.45, transmission) | rifrazione via `refract` + `sampleEnvSoft`, Fresnel |
| 3× refraction RGB (IOR 1.40/1.15/1.50) | **1 sample env per canale** su vettori `refract` sfasati → aberrazione cromatica |
| Wave Texture → Displacement | perturbazione **normale** (bump) con stesso pattern bands·sine; silhouette solo se serve via Path nativo |
| Thin Film | opzionale, riuso del modello `iridescent` |

Non replicabile 1:1: rifrazione/dispersione fisica ray-traced (non serve per il look).
Punto critico: senza l'HDRI giusto il match fallisce a prescindere dai parametri.

---

## 6. Parametri → uniform (bozza mappatura)

Da rifinire in fase di implementazione; qui solo l'ancoraggio numerico.

| Concetto | Sorgente Blender | Uniform proposto |
|---|---|---|
| IOR base | Glass BSDF 1.45 | `u_ior` = 1.45 |
| Dispersione (spread IOR) | 1.50 − 1.15 = 0.35 | `u_dispersion` ≈ 0.15–0.35 (tuning) |
| Roughness vetro | 0.0 | `u_roughness` = 0.0 |
| Roughness dispersione | 0.3 | blur extra sui tap cromatici |
| Scala swirl | Pattern scale 1.0 | `u_warpScale` |
| Intensità swirl | Pattern distortion 20 | `u_warpAmount` |
| Ampiezza displacement | 0.1 | `u_bump` |
