# HANDOFF

Snapshot dello stato corrente. Questo file viene SOVRASCRITTO ad ogni fine sessione,
non e' uno storico (per quello c'e' CHANGELOG.md).

## Stato attuale
Fase: 3 - LiquidMetal Paper validato su Android da Giulio. Fase 2 FluidGradient COMPLETA e validata su Android (emulatore API 34, validazione visiva di Giulio). iOS Fase 1+2 implementato ma non verificato - serve Mac.

## Completato
- Contratto Nitro Fase 2: `shader`, `colors[]`, `speed`, `intensity`, `scale`, `warp`, `grain` + prop Fase 1; codegen rigenerato, typecheck verde.
- `FluidGradient` componente pubblico (`src/effects/`), default in `src/materials/fluid.ts`.
- Android: `fluid-gradient.agsl` da assets (fBm 4 ottave + domain warp + palette branchless a indici costanti + dithering), selezione material per nome, fallback LinearGradient statico <API 33.
- iOS: `fluid-gradient.metal` + selezione/pipeline cache in `HybridNitroShaders.swift` (fragment compilato da stringa inline, .metal e' la fonte di riferimento).
- Reference LiquidMetal Paper Design aggiunta in `docs/refs/` con attribution Apache 2.0.
- Porting LiquidMetal in corso nel worktree: shader AGSL/Metal, plumbing Kotlin/Swift, spec Nitro e wrapper pubblico.
- Decisione Giulio: LiquidMetal Paper va mantenuto com'e'. Le tre ball della reference sono materiali diversi/futuri, non variant di LiquidMetal.
- F3-D1b approvato: rimossi `silver/aqua/pearl` da `LiquidMetal`; example riportato a demo singola del materiale Paper con `shape="circle"`.
- F3-D2 approvato: loop del materiale comune LiquidMetal per `shape="circle"` reso seamless con tempo interno loopato (`loopTravel`, `phase`, `loopT`) e crossfade del noise non periodico in AGSL, Metal file e Swift inline.
- LiquidMetal Paper validato da Giulio dopo il fix loop.
- Aggiunte reference `docs/refs/materials/` per nuova famiglia separata `MaterialOrb`; analisi iniziale scritta in `docs/process/MATERIALS_ANALYSIS.md`.
- F4-D1 MaterialOrb Android avviato: `material="liquidChrome"` implementato, demo example aggiornata, build/debug install ok, runtime AGSL senza crash dopo rename `orbSaturate`.
- Screenshot runtime corrente: `material-orb-marble.png` alla root del repo (debug artifact non necessariamente da committare).

## Debito tecnico dichiarato (da azzerare, Giulio non vuole debito)
1. iOS: sorgente shader duplicato (stringa inline autoritativa vs file `fluid-gradient.metal`) - si risolve col wiring podspec/Xcode della default library, SOLO su Mac.
2. Android: `shared-surface.agsl` negli assets era inutilizzato; ora il solid viene caricato da assets nel worktree corrente, ma va validato su device insieme a LiquidMetal.
3. Rename `packages/react-native-nitro-shaders` -> `packages/nitro-shaders` (preferenza espressa, mai eseguito).
4. Root `package.json` non ha script `typecheck`; la verifica corrente va eseguita da `packages/react-native-nitro-shaders` con `bun run typecheck`.
5. LiquidMetal Paper validato Android; resta da validare iOS su Mac.
6. LiquidMetal shape `daisy`/`metaballs`: motion locale non reso periodico in F3-D2; trattare solo se quelle shape entrano nella demo/API validata.
7. MaterialOrb liquidChrome e' stabile su Android ma visivamente ancora da rifinire: serve piu' blob/marbling organico vicino a `docs/refs/materials/screenshot_002.png`.

## Prossimo task pianificato
- F4-D1 follow-up: rifinire `MaterialOrb liquidChrome` Android verso la reference, poi validare Giulio prima di aggiungere `liquidGlass` e `iridescentGlass`.
- F3-D3: allineare e validare LiquidMetal iOS su Mac; contestualmente risolvere il debito iOS della default library se possibile.
- Dopo validazione LiquidMetal: aprire una fase separata per classificare le ball reference come materiali propri (es. chrome orb, aqua gel orb, pearl/iridescent orb), senza legarle all'API Paper.

## Blocchi aperti
- BLOCKED (Giulio): build+validazione iOS su Mac (Fase 1 e 2).
- BLOCKED (Giulio/Mac): validazione iOS LiquidMetal.
