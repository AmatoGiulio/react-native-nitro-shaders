# CLAUDE.md

Questo file viene letto automaticamente da Claude Code all'inizio di ogni sessione.
Se sei un agente e stai leggendo questo file, le regole qui sotto sono vincolanti e non negoziabili.

## Cos'è questo progetto

`react-native-nitro-shaders` — libreria React Native con runtime shader nativo condiviso
(Metal su iOS, AGSL/RuntimeShader su Android), costruita su Nitro Modules.
Architettura: Material × Motion × Skin (vedi docs/architecture/material-motion-skin.md).

Fonte di verità dell'API: `docs/architecture/material-motion-skin.md`.
Reference tecnico (matematica shader, piattaforme): `docs/engineering/shader-techniques.md`.
Mappa completa della doc: `docs/README.md`.
Qualsiasi conflitto tra questo file, AGENTS.md, docs/process/STACK.md e l'architettura va
segnalato all'orchestratore, mai risolto autonomamente da un agente.

## Gerarchia decisionale — NON derogabile

```
Fable 5 (orchestratore)  →  UNICA mente decisionale del progetto
    ↓ direttive scritte, esplicite, con confini chiari
Opus / Sonnet (developer) →  implementano ESATTAMENTE la direttiva ricevuta
    ↓ task atomici, meccanici, senza ambiguità
Haiku (worker)             →  esegue alla lettera, zero interpretazione
```

Regole assolute:

1. **Solo l'orchestratore decide.** Developer e worker non scelgono tra alternative
   architetturali, non inventano naming, non aggiungono feature non richieste,
   non "migliorano" qualcosa non incluso nella direttiva.
2. **Se un agente non sa cosa fare, si ferma.** Non deve mai:
   - inventare un'API non specificata,
   - assumere un default non scritto nella direttiva,
   - procedere "per analogia" con un'altra parte del codebase senza conferma.
   Deve scrivere esplicitamente: `BLOCKED: [motivo]` e aspettare.
3. **Ogni output di developer/worker va approvato dall'orchestratore prima di essere
   considerato definitivo.** Nessun merge, nessun "done" implicito.
4. **Nessuna decisione di prodotto o di architettura viene presa fuori da
   questo file o da docs/process/STACK.md.** Se manca una regola per un caso, l'agente la
   richiede all'orchestratore — non la crea.

## Protocollo di sessione — obbligatorio

All'inizio di ogni sessione, l'orchestratore:
1. Legge `CHANGELOG.md` e `docs/process/HANDOFF.md` per capire lo stato esatto in cui si trova il progetto.
2. Dichiara la fase attiva (Fase 1..5, vedi docs/process/STACK.md → Roadmap) prima di assegnare task.

Alla fine di ogni sessione, l'orchestratore (mai developer/worker):
1. Aggiorna `CHANGELOG.md` con formato: `## [data] — [fase] — [una riga di sommario]`
   seguito da bullet list di cosa è cambiato, cosa è stato approvato, cosa è stato scartato.
2. Aggiorna `docs/process/HANDOFF.md` sovrascrivendolo (non è uno storico, è uno snapshot) con:
   - stato esatto del lavoro (cosa è completo, cosa è a metà),
   - prossimo task pianificato,
   - eventuali `BLOCKED` aperti che serve risolva Giulio direttamente.

Nessuna sessione si chiude senza questo aggiornamento.

## Cosa NON fare mai (per qualsiasi agente)

- **MAI usare Argent MCP / emulatori / simulatori / Metro in questo progetto.**
  L'orchestratore e gli agenti si limitano a implementare, eseguire `bun run typecheck`
  (nel package `packages/react-native-nitro-shaders`) e test unitari/funzionali.
  La validazione visiva su device è ESCLUSIVAMENTE di Giulio.
- Non installare dipendenze non previste in docs/process/STACK.md senza approvazione esplicita.
- Non modificare `nitro.json` / struttura HybridObject senza direttiva dell'orchestratore
  (rompe autolinking e codegen a cascata).
- Non scrivere codice "placeholder" o TODO silenziosi spacciandoli per completi.
- Non toccare più di un effetto/materiale per task, anche se sembra collegato.
- Non pubblicare, taggare versioni, o toccare `package.json#version` senza approvazione.

## File di riferimento

- `AGENTS.md` — definizione dettagliata dei ruoli e formato delle direttive
- `docs/process/STACK.md` — decisioni tecniche, struttura repo, roadmap, convenzioni di codice
- `CHANGELOG.md` — storico sessioni (append-only)
- `docs/process/HANDOFF.md` — stato corrente (sovrascritto ad ogni sessione)
