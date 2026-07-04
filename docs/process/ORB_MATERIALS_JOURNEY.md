# Orb Materials — il percorso fino al risultato

Racconto onesto di come siamo arrivati ai tre material orb (`metal`, `water`,
`iridescent`) 3D e vivi. Serve a non ripetere i vicoli ciechi e a ricordare **cosa
ha fatto il salto**. Cronologia 2026-07-03 / 2026-07-04.

## Obiettivo

Sfere di materiale "pelle viva" che sembrano rendering 3D: metallo liquido, gel
d'acqua, bolla di sapone iridescente. Con riflessi di luce **naturali** e movimento
organico. Reference: l'app *Liquid Metal Shaders* di @jc_builds
(`references/materials/*.png` + `liquid-orb-metal-swiftui-reference.mp4`) e material
PBR di BlenderKit (clean water, liquid metal, soap bubble).

## I tentativi falliti (e perché)

1. **Shading 2D marmorizzato.** Colore da noise/fBm 2D sul piano, senza modello di
   illuminazione. → **Disco piatto**: nessun volume, nessuna sfera.

2. **Sfera analitica + environment procedurale.** Normale sferica
   `z = sqrt(1-|p|²)` + `studioEnv()` finto (softbox disegnati con `envBlob`/
   `sin`) + fresnel + specular Blinn-Phong. → Sembrava **plastica/argilla**:
   orizzonte duro, specular che vagava a "occhio", niente riflesso credibile.

3. **Tuning dell'environment procedurale (bande `sin`).** Aggiunte striature
   metalliche con `sin(reflection * f)`. → **Righe verticali regolari** (a
   corrugazione), l'opposto dell'organico.

4. **Raymarch SDF di una sfera deformata da noise 3D.** Vera geometria 3D con
   silhouette organica e normale dal gradiente SDF. → La 3D c'era, ma l'ambiente
   era ancora **procedurale/finto** → restava una "palla lucida", non liquido che
   riflette una stanza. (E rischio performance.)

5. **Matcap procedurale / env-mapping v3 (`reflect(-V,N)` + stanza virtuale).**
   → **Palla da biliardo**: un environment inventato con la matematica non produce
   una stanza con finestre credibile.

Errori tecnici trasversali scoperti lungo la strada (documentati per non ripeterli):
- **Orientamento Y**: in AGSL `fragCoord.y` cresce verso il basso → luce/environment
  risultavano capovolti finché non si è flippato `uv.y` (o `N.y`).
- **Scala uv**: `uv = (frag-c)/size` dà `|uv|≈0.5` ai bordi di una view quadrata → la
  sfera riempiva solo una calotta. Corretto con `/(0.5*size)`.
- **Shadowing built-in SkSL**: una variabile locale chiamata `floor` ombreggia la
  funzione `floor()` e rompe la compilazione. Rinominare.
- **Compositing alpha**: premoltiplicare il colore per l'alpha e restituire alpha
  dritto crea un alone scuro sul bordo AA.

## La diagnosi che ha sbloccato tutto

Guardando davvero le reference BlenderKit (sfera metallica nera, bolla di sapone):
tutte mostrano il **riflesso di finestre/softbox di uno studio**. Quei bianchi a
forma di finestra sono un **HDRI di ambiente reale riflesso**. Conclusione: i
"riflessi naturali" non si *inventano* con `sin()` — si *riflettono da un'immagine*.

Ricerca collaterale ("importare un material Blender 1:1"): non fattibile — un
material Blender è un node tree per path tracer, senza export GLSL affidabile; glTF
porta solo parametri PBR, non i nodi procedurali. Ma il **look** di quei material in
Blender viene dall'**HDRI del World**. Anche l'import "vero" dipenderebbe da un
environment. Vedi memoria `reference-are-hdri-reflective`.

## La soluzione (cosa ha fatto il salto)

**Mini-PBR con Image-Based Lighting**, cioè il modello di Blender/Eevee in real-time:

1. **Environment reale**, non procedurale. Un HDRI equirettangolare di studio
   (`assets/env/studio.png`, CC0 Poly Haven) passato allo shader come
   `uniform shader u_env` (BitmapShader in Kotlin), campionato dal vettore di
   riflessione (`reflect`) e di rifrazione (`refract`). → riflessi di finestre veri.
   **Questo è stato il salto n.1.**

2. **Material = preset di parametri PBR** (come un Principled BSDF): metal =
   metallico specchiante; water = dielettrico con `transmission`/`ior` (vede
   attraverso l'ambiente); iridescent = thin-film sopra un dielettrico. L'ambiente
   è **separato** dal material: tutti riflettono la stessa stanza.

3. **Superficie viva in 3D.** Il rilievo si campiona con **noise 3D sulla superficie
   sferica** `(p.x, p.y, z)` — non con noise 2D sul piano — e il dominio del noise
   **ruota nel tempo** su assi Y+X (`rotate3`). Più un bulge radiale ("spinta da
   dentro"). → la skin vive *sulla sfera 3D* e orbita in tutte le direzioni.
   **Questo è stato il salto n.2** (feedback di Giulio: "3D vera, forza da dentro,
   orbita in ogni direzione").

## Lezioni da tenere

- Non *lucidare* una sfera: darle una **stanza da riflettere** (IBL).
- Il "vivo/3D" viene dal **noise 3D sulla superficie sferica + rotazione**, non dal
  bump 2D sul piano proiettato.
- Le reference fotorealistiche sono quasi sempre **IBL**: cerca l'environment prima
  di inventare i riflessi.

## Stato e strada

Risultato accettabile su Android per tutti e tre (validato da Giulio, 2026-07-04).
Prossimi passi in `HANDOFF.md`: parametrizzazione (rotazione per-asse,
`roughness`/`transmission`/`ior`), cleanup, porting iOS Metal, ottimizzazione asset.
Dettaglio tecnico dell'engine: `../engineering/orb-materials.md`.
