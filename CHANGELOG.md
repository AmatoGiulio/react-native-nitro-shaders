# CHANGELOG

Storico append-only delle sessioni. Ogni voce la scrive solo l'orchestratore a fine sessione.

## [da compilare] — Setup — Creazione governance iniziale
- Creati CLAUDE.md, AGENTS.md, STACK.md, CHANGELOG.md, HANDOFF.md
- Nome pacchetto deciso: react-native-nitro-shaders
- Roadmap Fase 1-5 definita in STACK.md

## [2026-07-03] - Fase 1 - Shared ShaderSurface core
- Sostituito lo scaffold `isRed` nello spec Nitro con props core `color`, `animated`, `paused`, `debugTime`.
- Creato wrapper TypeScript `ShaderSurface` e demo minima in `apps/example/App.tsx`.
- Implementata shared surface iOS con `MTKView`, pipeline Metal minimale e colore solido driven da JS.
- Implementata shared surface Android con custom `View`, `RuntimeShader` API 33+ e fallback statico sotto API 33.
- Aggiunti shader reference `shared-surface.metal` e `shared-surface.agsl`.
- Verifica: `bun run typecheck` resta bloccato perché `nitrogen/generated` è ancora generato sul vecchio contratto `isRed`; serve rigenerare Nitrogen.

## [2026-07-03] - Fase 1 - Fix runtime Android + validazione emulatore
- Fix Invariant Violation "Tried to register two views with the same name NitroShaders": doppia chiamata a `getHostComponent` (in `index.ts` e `ShaderSurface.tsx`); registrazione ora solo nel core, `index.ts` ri-esporta.
- Fix rendering Android fullscreen: `canvas.drawPaint()` riempiva l'intero clip del device (clipChildren=false di RN); sostituito con `drawRect` sui bounds della view, sia path AGSL sia fallback.
- Codegen Nitrogen risulta già rigenerato sul contratto Fase 1; `bun run typecheck` passa.
- Verificato su emulatore Pixel 6a API 34: quadrato 220×220 colore solido via RuntimeShader, centrato, driven da JS.
- Scartato: nessuna modifica a spec/nitro.json necessaria.
- Aperto: validazione iOS richiede Mac (BLOCKED per Giulio, vedi HANDOFF).

## [2026-07-03] - Fase 2 - FluidGradient (MVP) — Android validato
- F2-D1 (Sonnet): spec Nitro esteso con `shader`, `colors[]`, `speed`, `intensity`, `scale`, `warp`, `grain`; codegen rigenerato; plumbing prop Kotlin/Swift; componente pubblico `FluidGradient` + `materials/fluid.ts`; demo aggiornata. Approvato con deviazione: `style` tipizzato via `ComponentProps` (doppia installazione react-native nel monorepo).
- F2-D2 (Opus): `fluid-gradient.agsl` (value noise, fBm 4 ottave, domain warp, palette 6 colori, dithering) caricato da assets; selezione material per nome in ShaderSurfaceView; fallback LinearGradient statico <API 33. Un rigetto in revisione: AGSL vieta indici dinamici su array uniform → palette riscritta branchless a indici costanti.
- F2-D3 (Opus): `fluid-gradient.metal` + pipeline cache per nome in Swift. Deviazione approvata: fragment compilato da stringa inline (come path esistente), file .metal come fonte di riferimento — wiring default library rimandato al Mac.
- Validato da Giulio su emulatore API 34: gradiente organico animato 220×220.
- Aggiunta reference visiva `docs/refs/smoke-shaders.png` (swoosh black chrome liquido) → input per Fase 3 LiquidChrome.
- Il core Fase 1 non ha richiesto modifiche strutturali per ospitare il primo material (validazione dell'architettura).
