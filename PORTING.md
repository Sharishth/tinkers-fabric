# Porting status

Baseline: Tinkers' Construct `v3.12.1.231` and Mantle for Forge 1.20.1 (SlimeKnights).
Target: Minecraft 26.3, Fabric Loader, Fabric API, Java 25.

## How the port is done

- Read one subsystem in the 1.20.1 source, then rewrite it against Fabric and the 26.3 Minecraft API. No bulk copying of Forge or NeoForge code.
- The NeoForge 26.1 community ports by justduck25 are used only to see how vanilla APIs changed between 1.20.1 and 26.x. Any approach taken from them is credited in the commit and here.
- Mod ids stay `mantle` and `tconstruct` (decision D-01) so existing data packs and tags keep working.
- Each milestone lives on its own branch and only merges when it builds, the dev client boots and tests pass.

## Milestones

| ID | Scope | Status |
|---|---|---|
| M1 | Mantle core: registries, helpers, config, networking | planned |
| M2 | Tinkers skeleton: registries, items, blocks, tags, creative tab | planned |
| M3 | Tool data model: materials, parts, stats, modifiers, tool stack data, tooltips | planned |
| M4 | Crafting: part builder, tinker station, patterns, recipes, loot | planned |
| M5 | Fluids and smeltery: melter, smeltery, foundry, casting, tanks | planned |
| M6 | Combat, mining and armor logic: modifier hooks, events, damage, durability | planned |
| M7 | Client: rendering, models, GUIs, books, recipe-viewer compat | planned |
| M8 | World gen, data gen, advancements, final compat pass | planned |

## Not ported (yet or on purpose)

Every feature left out is listed here with its reason. Nothing is dropped silently.

| Feature | Upstream location | Reason | Tracking |
|---|---|---|---|
| _none yet_ | | | |
