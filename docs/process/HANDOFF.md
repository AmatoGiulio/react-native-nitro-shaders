# HANDOFF

Snapshot dello stato corrente. Questo file viene SOVRASCRITTO ad ogni fine sessione,
non è uno storico (per quello c'è CHANGELOG.md).

## Stato attuale
Fase: 1 - Core runtime. Android COMPLETO e verificato su emulatore. iOS implementato ma NON verificato (macchina Windows, serve build su Mac).

## Completato
- Codegen Nitrogen rigenerato sul contratto Fase 1 (`color`, `animated`, `paused`, `debugTime`); `bun run typecheck` passa.
- Fix doppia registrazione host view: `getHostComponent('NitroShaders', ...)` era chiamato sia in `src/index.ts` sia in `src/core/ShaderSurface.tsx` → Invariant Violation al load del bundle. Ora registrato solo in `ShaderSurface.tsx`; `index.ts` ri-esporta.
- Fix rendering Android: `canvas.drawPaint()` su canvas hardware riempiva l'intero clip del device (RN imposta clipChildren=false) → shader fullscreen invece dei bounds della view. Sostituito con `canvas.drawRect(0,0,width,height,...)` sia per path AGSL sia per fallback <API 33.
- Verificato su emulatore Pixel 6a (API 34): quadrato 220×220 colore solido via RuntimeShader, centrato, driven da JS (cambio colore da App.tsx riflesso dopo reload).

## Prossimo task pianificato
- Verifica iOS su Mac: build apps/example, confermare che `HybridNitroShaders.swift` (MTKView) renderizzi il quadrato colore solido rispettando i bounds → chiude la Definition of Done di Fase 1.
- Dopo DoD verificata su entrambe le piattaforme: avvio Fase 2 (FluidGradient).

## Blocchi aperti
- BLOCKED (Giulio): validazione iOS non eseguibile da questa macchina (Windows). Serve run su Mac.
- Rename `packages/react-native-nitro-shaders` → `packages/nitro-shaders` ancora non eseguito (preferenza espressa, non prioritaria).
