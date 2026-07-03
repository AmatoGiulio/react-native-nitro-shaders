# react-native-nitro-shaders

Native shader rendering kit for React Native. A shared native runtime (Metal on
iOS, AGSL/`RuntimeShader` on Android) exposes **living-surface materials** that
can skin backgrounds, text and SVG paths.

The public API is organised on three orthogonal axes — **Material × Motion ×
Skin**. See [docs/architecture/material-motion-skin.md](docs/architecture/material-motion-skin.md).

## Monorepo layout

```
packages/react-native-nitro-shaders/   # the published package
apps/example/                          # Expo demo + dev loop (not published)
docs/                                  # architecture, engineering, process, references
```

## Docs

Start from [docs/README.md](docs/README.md).

- Governance and roadmap: [CLAUDE.md](CLAUDE.md), [docs/process/STACK.md](docs/process/STACK.md)
- Session history: [CHANGELOG.md](CHANGELOG.md)
- Current state: [docs/process/HANDOFF.md](docs/process/HANDOFF.md)
