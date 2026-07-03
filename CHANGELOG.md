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
