# Asset licenses & provenance

Asset di terzi bundlati o usati come reference nel repo.

## Bundlati nel pacchetto (spediti nell'app)

| Asset | Uso | Provenienza | Licenza |
|---|---|---|---|
| `packages/react-native-nitro-shaders/android/src/main/assets/env/studio.png` | Environment IBL (studio) riflesso dai material orb | Poly Haven — `studio_small_08` (HDRI), convertito a PNG equirettangolare 1024×512 | CC0 (public domain) |
| `packages/react-native-nitro-shaders/android/src/main/assets/env/mercury-env.png` | Environment riflesso dal material mercury | Greyscalegorilla Pro Studios Metal Vol2 05 (preview pubblica ritagliata) — SOLO SVILUPPO, da sostituire con env proprio prima della pubblicazione | DA SOSTITUIRE |
| `packages/react-native-nitro-shaders/android/src/main/assets/env/glass-env.png` | Environment di default del material glass (teal scuro, validato da Giulio 2026-07-06) | copia di `lab-2.png` — thumbnail BlendKit — SOLO SVILUPPO, da sostituire prima della pubblicazione (1.1MB: anche da ottimizzare) | DA SOSTITUIRE |
| `packages/react-native-nitro-shaders/android/src/main/assets/env/water-env.png` | Environment di default del material water/gel (tramonto sul mare, validato da Giulio 2026-07-06) | copia di `lab-12.png` — asset BlendKit (`clouds_2K_…`) fornito da Giulio, licenza free (confermata da Giulio 2026-07-06) | Free |
| `packages/react-native-nitro-shaders/android/src/main/assets/env/lab-10.png` | Env lab per test `water` — stand-in CC0 dell'HDRI BlendKit "metal-hangar" | Poly Haven — `hangar_interior` (HDRI 2K), tonemap Hable → PNG equirect 1024×512 via ffmpeg | CC0 (public domain) |
| `packages/react-native-nitro-shaders/android/src/main/assets/env/lab-11.png` | Env lab per il water/gel — cielo alba (riferimento Gel shader di Giulio) | Poly Haven — `qwantani_dawn_puresky` (esportato da Blender come EXR da Giulio), tonemap Hable → PNG equirect 1024×512 via ffmpeg | CC0 (public domain) |
| `packages/react-native-nitro-shaders/android/src/main/assets/env/lab-12.png` | Env lab — tramonto sul mare (cielo + oceano); env default di water | asset BlendKit (`clouds_2K_…`) fornito da Giulio, ridimensionato a 1024×512, licenza free (confermata da Giulio 2026-07-06) | Free |
| `packages/react-native-nitro-shaders/android/src/main/assets/env/lab-13.png` | Env lab — mare calmo che riflette il cielo | asset BlendKit (`the-calm-sea-…`) fornito da Giulio, ridimensionato a 1024×512 — SOLO SVILUPPO | DA VERIFICARE |
| `packages/react-native-nitro-shaders/android/src/main/assets/env/lab-14.png` | Env lab — cielo luminoso su mare (purple haze) | asset BlendKit (`purple-haze-sky_…`) fornito da Giulio, ridimensionato a 1024×512 — SOLO SVILUPPO | DA VERIFICARE |

## Reference (non spediti)

| Asset | Uso | Provenienza | Licenza |
|---|---|---|---|
| `references/materials/*.png`, `*.webp`, `*.mp4` | Target visivi orb (BlenderKit previews, app @jc_builds) | screenshot/preview di reference | uso solo come riferimento visivo interno |
| `references/materials/aura.webp` | Target material `aura` (neon energy orb: glow verde/magenta, interno viola con flussi rosa, punto luce blu centrale) | fornita da Giulio 2026-07-04 | riferimento visivo interno |
| `references/materials/glass.webp` | Target material `glass` (vetro/chrome liquido scuro traslucido, riflessi viola/blu + accenti rossi; la gemma rossa in basso a dx è un artefatto di ritaglio, da ignorare) | fornita da Giulio 2026-07-04 | riferimento visivo interno |

Nota: CC0 non richiede attribuzione, ma la provenienza è tracciata qui per chiarezza.
Se si sostituisce l'HDRI con un altro environment, aggiornare questa tabella.

TODO (Giulio): aggiungere i link alle pagine BlenderKit degli asset lab-12/13/14
(e water-env) in colonna Provenienza.
