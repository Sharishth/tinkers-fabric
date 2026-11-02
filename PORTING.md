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

## Behaviour differences from upstream

| Area | Upstream (1.20.1) | This port | Why |
|---|---|---|---|
| Common tag namespace | `forge:` (for example `forge:ingots/iron`) | `c:` (for example `c:ingots/iron`) | Fabric and 26.x use `c`; upstream planned the same move for 1.21+ |
| Spawn eggs | Two egg colours passed at registration | No colours: 26.x spawn eggs use an item model and texture | Vanilla 26.x removed spawn egg colours |
| Potion ids from Mantle's potion helper | Normal potion registered as `_name` | Normal potion is `name`; `long_name`, `strong_name` unchanged | Upstream's own TODO planned this for 1.21 |
| `MetalItemObject.values()` | Listed the ingot twice and skipped the nugget | Block, ingot and nugget | Upstream bug; `forEach` already visited all three |
| Mantle menu types (for addon code) | Forge `IContainerFactory` reading a raw buffer | Fabric `ExtendedMenuType` with a `StreamCodec` for the opening data | Fabric has no Forge menu extension |
| Mantle registration API (for addon code) | Suppliers, e.g. `ITEMS.register("x", () -> new Item(props))`; registers attach to the Forge event bus | Factories that receive properties already carrying the entry id, e.g. `ITEMS.register("x", p -> new Item(p.stacksTo(16)))`; `register()` then `RegistrationQueue.flush()` | 26.x requires the id inside item and block properties before construction; Fabric has no register events |
| Wood signs (Tinkers slimewood signs) | Mantle's own block entity types `mantle:sign` and `mantle:hanging_sign` | Vanilla `minecraft:sign` and `minecraft:hanging_sign`, opened to the new blocks with Fabric's `addValidBlock` | Vanilla renderers then work unchanged. Signs placed in worlds from the Forge version would lose their text (old worlds are not supported, see I-009) |
| Wood burn times | `Burnable*Item` classes with tick counts (300, 150, 100, 200) | Vanilla's data-driven fuel keys on the item (`cookingFuel`), the same keys oak uses | 26.3 made burn times data; values come from the datapack |
| Log stripping | `StrippableLogBlock` answering Forge's tool action hook | Plain `RotatedPillarBlock` logs; stripping registered with Fabric's `BlockTransformerHelper` | 26.3 axes strip through a block transformer data component |

## Not ported (yet or on purpose)

Every feature left out is listed here with its reason. Nothing is dropped silently.

| Feature | Upstream location | Reason | Tracking |
|---|---|---|---|
| Mantle registry adapters (`RegistryAdapter`, `BlockRegistryAdapter`, `ItemRegistryAdapter`, `FluidRegistryAdapter`, `EntityTypeRegistryAdapter`, `ContainerTypeRegistryAdapter`, `EnumRegistryAdapter`, `BlockEntityTypeRegistryAdapter`) | Mantle `registration/adapter` | Built for Forge's register event (construct first, register later). 26.x needs ids before construction, and Tinkers uses none of them; the deferred registers cover the same cases. Mantle's own sign block entities are replaced by vanilla's (see Behaviour differences) | I-008 |
| `MantleSignBlockEntity`, `MantleHangingSignBlockEntity`, `Mantle*SignBlock`, `MantleRegistrations`, `StrippableLogBlock`, `RegistrationHelper.registerWoodType`/`forEachWoodType` | Mantle `block`, `block/entity`, `registration` | Only existed to give signs a block entity type Forge could extend and to add wood types to the sign atlas. Vanilla sign types plus Fabric `addValidBlock` cover the first; 26.3 sign boards are block models, so no atlas list is needed | none needed |
| `RegistrationHelper.injected()` and `@ObjectHolder` fields | Mantle `registration` | Forge-only field injection; Fabric code holds `RegistryObject` handles instead | none needed |
| `RegistrationHelper.handleMissingMappings` (remap renamed ids in old worlds) | Mantle `registration` | No Forge MissingMappingsEvent and no registry alias API in 26.3; only matters for worlds made with older Tinkers versions | I-009 |
| Block entity data fixer types | Mantle `BlockEntityTypeDeferredRegister` | Removed from vanilla `BlockEntityType` in 26.x | none needed |

