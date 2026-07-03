# HANDOFF

Snapshot dello stato corrente. Questo file viene SOVRASCRITTO ad ogni fine sessione,
non è uno storico (per quello c'è CHANGELOG.md).

## Stato attuale
Fase: 2 - FluidGradient COMPLETA e validata su Android (emulatore API 34, validazione visiva di Giulio). iOS implementato (D1+D3) ma non verificato — serve Mac.

## Completato
- Contratto Nitro Fase 2: `shader`, `colors[]`, `speed`, `intensity`, `scale`, `warp`, `grain` + prop Fase 1; codegen rigenerato, typecheck verde.
- `FluidGradient` componente pubblico (`src/effects/`), default in `src/materials/fluid.ts`.
- Android: `fluid-gradient.agsl` da assets (fBm 4 ottave + domain warp + palette branchless a indici costanti + dithering), selezione material per nome, fallback LinearGradient statico <API 33.
- iOS: `fluid-gradient.metal` + selezione/pipeline cache in `HybridNitroShaders.swift` (fragment compilato da stringa inline, .metal è la fonte di riferimento).

## Debito tecnico dichiarato (da azzerare, Giulio non vuole debito)
1. iOS: sorgente shader duplicato (stringa inline autoritativa vs file `fluid-gradient.metal`) — si risolve col wiring podspec/Xcode della default library, SOLO su Mac.
2. Android: `shared-surface.agsl` negli assets è inutilizzato (il material solid usa la costante inline `SHADER_SOURCE`) — unificare: o si carica anche il solid da assets o si elimina l'asset stale.
3. Rename `packages/react-native-nitro-shaders` → `packages/nitro-shaders` (preferenza espressa, mai eseguito).

## Prossimo task pianificato
- Bonifica debito 2 (task worker, meccanico).
- Su Mac: validazione iOS Fase 1+2 e debito 1.
- Poi Fase 3 — LiquidChrome: reference visiva in `docs/refs/smoke-shaders.png` (swoosh black chrome liquido: height field + normal map + chrome bands, variante blackChrome; prospettiva futura: maschera su shape/logo arbitraria — da decidere se rientra nel material o nella famiglia text/mask).

## Blocchi aperti
- BLOCKED (Giulio): build+validazione iOS su Mac (Fase 1 e 2).
