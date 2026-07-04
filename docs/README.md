# Docs

Mappa della documentazione. Ogni sezione ha uno scopo unico: se un'informazione
appartiene a più sezioni, vive in una sola ed è linkata dalle altre.

| Sezione | Scopo | Autorità |
|---|---|---|
| [architecture/](architecture/) | Il **cosa/perché** del sistema: design pubblico, decisioni. | Fonte di verità dell'API. |
| [engineering/](engineering/) | Il **come**: tecniche shader, note di piattaforma, runtime rules. | Reference tecnico, non decisionale. |
| [process/](process/) | Come lavoriamo: stack, roadmap, stato corrente. | Governance operativa. |
| [references/](references/) | Materiale esterno: screenshot target, licenze, attribution. | Solo riferimento, non codice. |

## Indice

### architecture
- [material-motion-skin.md](architecture/material-motion-skin.md) — architettura pubblica Material × Motion × Skin (fonte di verità dell'API).

### engineering
- [shader-techniques.md](engineering/shader-techniques.md) — matematica shader (noise/fBm, superficie metallica), AGSL vs Metal, masking skin, runtime rules.
- [orb-materials.md](engineering/orb-materials.md) — engine dei tre material orb (`metal`/`water`/`iridescent`): mini-PBR + IBL (riflette un HDRI di studio reale), superficie 3D viva, preset PBR.

### process
- [STACK.md](process/STACK.md) — stack, struttura repo, convenzioni, roadmap.
- [HANDOFF.md](process/HANDOFF.md) — snapshot dello stato corrente (sovrascritto ogni sessione).
- [ORB_MATERIALS_JOURNEY.md](process/ORB_MATERIALS_JOURNEY.md) — il percorso agli orb: tentativi falliti, la diagnosi e cosa ha fatto il salto (IBL + noise 3D).
- [OPERATIONAL-PLAN.md](process/OPERATIONAL-PLAN.md) — piano operativo corrente (Fase 0 architettura/code-sharing, Fase 1 fluidGradient-as-material, Fase 2 metal 1:1 + nuovo glass).

### references
- [materials/](references/materials/) — screenshot/video target per material (`metal`, `water`, `iridescent`, `fluidgradient`, overview, liquid orb motion).
- [liquid-metal/](references/liquid-metal/) — reference Paper Design + `ATTRIBUTION.md` (Apache 2.0).
- [smoke-shaders.png](references/smoke-shaders.png) — reference chrome/smoke standalone.
- [ASSET-LICENSES.md](references/ASSET-LICENSES.md) — provenienza e licenze degli asset di terzi (es. HDRI studio CC0).

## Fuori da docs/ (radice repo)
- [CLAUDE.md](../CLAUDE.md) — regole vincolanti per gli agenti e gerarchia decisionale.
- [AGENTS.md](../AGENTS.md) — ruoli e formato delle direttive.
- [CHANGELOG.md](../CHANGELOG.md) — storico append-only delle sessioni.
