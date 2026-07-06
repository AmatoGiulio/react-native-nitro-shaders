# Asset licenses & provenance

Asset di terzi bundlati o usati come reference nel repo.

## Bundlati nel pacchetto (spediti nell'app)

| Asset | Uso | Provenienza | Licenza |
|---|---|---|---|
| `packages/react-native-nitro-shaders/android/src/main/assets/env/studio.png` | Environment IBL (studio) riflesso dai material orb | Poly Haven — `studio_small_08` (HDRI), convertito a PNG equirettangolare 1024×512 | CC0 (public domain) |
| `packages/react-native-nitro-shaders/android/src/main/assets/env/mercury-env.png` | Environment riflesso dal material mercury | Greyscalegorilla Pro Studios Metal Vol2 05 (preview pubblica ritagliata) — SOLO SVILUPPO, da sostituire con env proprio prima della pubblicazione | DA SOSTITUIRE |

## Reference (non spediti)

| Asset | Uso | Provenienza | Licenza |
|---|---|---|---|
| `references/materials/*.png`, `*.webp`, `*.mp4` | Target visivi orb (BlenderKit previews, app @jc_builds) | screenshot/preview di reference | uso solo come riferimento visivo interno |
| `references/materials/aura.webp` | Target material `aura` (neon energy orb: glow verde/magenta, interno viola con flussi rosa, punto luce blu centrale) | fornita da Giulio 2026-07-04 | riferimento visivo interno |
| `references/materials/glass.webp` | Target material `glass` (vetro/chrome liquido scuro traslucido, riflessi viola/blu + accenti rossi; la gemma rossa in basso a dx è un artefatto di ritaglio, da ignorare) | fornita da Giulio 2026-07-04 | riferimento visivo interno |

Nota: CC0 non richiede attribuzione, ma la provenienza è tracciata qui per chiarezza.
Se si sostituisce l'HDRI con un altro environment, aggiornare questa tabella.
