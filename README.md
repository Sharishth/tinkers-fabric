# Tinkers Fabric

An **unofficial**, community-made port of [Tinkers' Construct](https://github.com/SlimeKnights/TinkersConstruct) and its library [Mantle](https://github.com/SlimeKnights/Mantle) to the **Fabric** mod loader for **Minecraft 26.3**.

> [!IMPORTANT]
> This project is **not affiliated with, endorsed by or supported by SlimeKnights**. Please do not report bugs from this port to the official Tinkers' Construct issue tracker.

> [!NOTE]
> **About this project.** This is a personal project, made for fun. It is built mostly by AI ([Claude](https://claude.com/claude-code), by Anthropic): apart from basic testing, the code, documentation and porting decisions are mainly handled by the AI. It is not made for personal or financial gain: there are no donations, ads or paid builds. The only goal is to experience Tinkers' Construct on a modern Minecraft version with Fabric. Expect rough edges, and use it at your own risk.

> [!WARNING]
> Pre-alpha. There is no playable build yet. The port is being built milestone by milestone (see [PORTING.md](PORTING.md)).

## Features

The goal is feature parity with Tinkers' Construct 3.x (Forge 1.20.1), rewritten for Fabric:

- Mantle library: registries, helpers, config, networking, book system
- Modular tools and armor: materials, parts, stats, modifiers
- Part builder, tinker station, patterns and casts
- Smeltery, foundry, melter, casting and tanks (Fabric Transfer API)
- World generation, guide books, advancements and recipe-viewer compatibility

Anything not yet ported, or left out on purpose, is listed with its reason in [PORTING.md](PORTING.md).

## Requirements

| Component | Version |
|---|---|
| Minecraft | 26.3 |
| Fabric Loader | 0.19.5 or newer |
| Fabric API | 0.161.0+26.3 or newer |
| Java | 25 |

Versions are confirmed against fabricmc.net during setup; see PORTING.md for the current pins.

## Install

No release exists yet. To build from source on Windows:

```powershell
git clone https://github.com/Sharishth/tinkers-fabric.git
cd tinkers-fabric
.\gradlew.bat build
```

The jars land in `tconstruct\build\libs\` and `mantle\build\libs\` once those modules exist.

## Development

- Build: `.\gradlew.bat build`
- Run a dev client: `.\gradlew.bat :tconstruct:runClient`
- Agents and contributors: start with [AGENTS.md](AGENTS.md); porting status and rules are in [PORTING.md](PORTING.md).

## Mod id

The mod keeps the id `tconstruct` so data packs, recipes and tags written for Tinkers' Construct keep working. Fabric and NeoForge jars never load in the same game, so the ids cannot clash.

## Credits

- **SlimeKnights** ([Tinkers' Construct](https://github.com/SlimeKnights/TinkersConstruct), [Mantle](https://github.com/SlimeKnights/Mantle), MIT): the original mod, its design, art and behaviour. This port follows their code as the reference.
- **justduck25** (Tinker's Continuum and Continuum-Mantle, MIT): their NeoForge 26.1 ports were used to learn how the code moved to the 26.x Minecraft APIs. Approaches taken from them are credited where used.

## License

MIT. See [LICENSE](LICENSE). The original SlimeKnights copyright notice is kept.
