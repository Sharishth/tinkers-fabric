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
| Mantle loadables, network side (for addon code) | `encode`/`decode` take a `FriendlyByteBuf` | They take a `RegistryFriendlyByteBuf`, and every loadable is a vanilla `StreamCodec`, usable directly in payload registration (D-20) | 26.x item, ingredient and fluid stream codecs need registry access; a wrong buffer is now a compile error |
| Mantle loadable API names (for addon code) | `xmap`, `comapFlatMap`, `flatComap`, `flatXmap` (deprecated); `ResourceLocationLoadable`; `list()` typed as `Loadable<List<T>>`; short/byte arrays take a fastutil mapper | Only `map` and `mapChecked`; `IdentifierLoadable`; `list()` returns `ListLoadable`; short/byte arrays take an `IntFunction` | Upstream marked the four methods for removal; 26.x renamed ResourceLocation to Identifier; upstream's own TODO planned the list type |
| Item stacks in JSON (datapacks) | `{"item": id, "count": n, "nbt": {...}}`; Mantle `ItemStackLoadable.*_NBT` | `{"item": id, "count": n, "components": {...}}` with vanilla's component patch; `*_COMPONENTS` constants (D-21) | 26.x moved item data from NBT to data components |
| Ingredients in JSON (datapacks) | Forge format (`{"item": id}`, `{"tag": id}`); `IngredientLoadable.ALLOW_EMPTY` | 26.x vanilla format (`"id"`, `"#tag"`, or a list); `IngredientLoadable.OPTIONAL` (`Optional<Ingredient>`, empty array = none) | 26.x ingredients use vanilla's codec and cannot be empty |
| Enchantments in Mantle loadables (for addon code) | `Loadables.ENCHANTMENT` gave an `Enchantment` | Gives a `Holder<Enchantment>`; parsing needs world registries in the context (`ContextKey.REGISTRIES`), see D-21 | 26.x enchantments are a datapack registry and exist only per world |
| Other Mantle loadable names (for addon code) | `Loadables.RESOURCE_LOCATION`; loot entries through Forge's loot modifier Gson | `Loadables.IDENTIFIER`; loot entries through vanilla's `LootPoolEntries.CODEC` | 26.x names; Forge's Gson is gone |
| `ItemStackList.of(Container)` and `copy(false)` | Always threw (add on a fixed-size list; set on an empty list) | Work as documented | Upstream bugs |
| `TranslationHelper` | Forge's `ForgeI18n` | Vanilla `Language` (English only on a dedicated server, as before) | Forge-only API |
| `mantle:mob_type` entity predicate (datapacks) | Compared the entity's `MobType` | Same JSON names (`undead`, `arthropod`, `illager`, `water`, `undefined`), matched through the entity type tags `undead`, `arthropod`, `illager`, `aquatic`; `undefined` = in none of them | 26.x removed `MobType` in favour of these tags |
| `mantle:has_enchantment` entity predicate | Read an `Enchantment` value | Reads a `Holder<Enchantment>`; needs world registries while parsing (D-21) | 26.x enchantments are a datapack registry |
| Mantle predicate registry constructors (for addon code) | Deprecated overloads without the "none" predicate | Removed | Upstream marked them for removal |
| Mantle id Gson serializer (for addon code) | `ResourceLocationSerializer<T extends ResourceLocation>` | `IdentifierSerializer<T>` built from a `Function<Identifier, T>` | 26.x `Identifier` is final, so id types wrap it instead of extending it |
| Compact loader registry parsing | One shared static JSON object was reused for every compact value | A new object per value | Upstream thread-safety bug; reload listeners can parse in parallel |
| Merged registry fields (`directField(typeKey, ...)`) | Rewrote the parent JSON object while reading it | Reads from a copy | Upstream side effect on input data |
| `Vector3fLoadable` object form | Wrote only non-zero axes but required all three when reading, so its own output failed | Missing axes read as 0 | Upstream bug |
| `NBTLoadable.ALLOW_STRING` | Passed the string through Gson first, which added quotes, so SNBT strings failed to parse | Parses the string's text as SNBT | Upstream bug |
| Array loadable minimum size | A minimum above 0 (other than the compact markers) was treated as 0, so too-short arrays loaded | The minimum is enforced | Upstream bug in `ArrayLoadable.SizeRange.checkSize` |
| `TypedMap.EMPTY.getOrDefault` | Returned null, ignoring the default | Returns the default | Upstream bug; broke its own non-null contract |
| Config files | Forge's `ForgeConfigSpec`; `mantle-client.toml` and `mantle-server.toml` registered in the mod constructor | Same file names, keys, defaults and comments, built on NeoForge's `ModConfigSpec` as shipped by the [Forge Config API Port](https://modrinth.com/mod/forge-config-api-port) mod, which is now a required mod (D-07). The client file is registered by the client entrypoint, so dedicated servers no longer get one. The server file lives in `config/`, with an optional per-world override in `<world>/serverconfig/` | Fabric has no config system of its own; the port keeps the TOML format and server-to-client sync |
| Heart renderer config comment | Said `DISABLE` falls back to "the Forge heart renderer" and told mod authors to cancel a Forge render event | Says `DISABLE` uses the vanilla heart renderer; the note for mod authors returns with the renderer | No Forge on Fabric; the renderer and its hook are not ported yet (T-72) |
| Mantle reload listeners (for addon code) | `MergingJsonDataLoader(gson, folder, builder)`, `RegistryDataMapLoader(name, folder, ...)`, `BlockStateDataMapLoader(name, folder, loadable)`; `parse`, `finishLoad` and `apply` had no parse context; added through Forge's `AddReloadListenerEvent` | Constructors also take the `PackType` the listener is registered for, and no Gson; `parse`, `finishLoad` and `apply` receive the parse context, which holds the world registries for server data (D-22); register with Fabric's `ResourceLoader.get(type).registerReloadListener(id, listener)` | 26.x enchantments, tags and item components need world registries to parse (D-21), and Fabric shares them only with server data listeners |
| `BlockStateDataMapLoader` after a broken file | States parsed before the error stayed in a shared buffer and were added to the next block that loaded | Each block is parsed on its own; a broken file adds nothing | Upstream bug |
| Wood signs (Tinkers slimewood signs) | Mantle's own block entity types `mantle:sign` and `mantle:hanging_sign` | Vanilla `minecraft:sign` and `minecraft:hanging_sign`, opened to the new blocks with Fabric's `addValidBlock` | Vanilla renderers then work unchanged. Signs placed in worlds from the Forge version would lose their text (old worlds are not supported, see I-009) |
| Wood burn times | `Burnable*Item` classes with tick counts (300, 150, 100, 200) | Vanilla's data-driven fuel keys on the item (`cookingFuel`), the same keys oak uses | 26.3 made burn times data; values come from the datapack |
| Log stripping | `StrippableLogBlock` answering Forge's tool action hook | Plain `RotatedPillarBlock` logs; stripping registered with Fabric's `BlockTransformerHelper` | 26.3 axes strip through a block transformer data component |
| Mantle networking (for addon code) | `NetworkWrapper(channel, version)` over a Forge `SimpleChannel`; packets implement `ISimplePacket`/`IThreadsafePacket` with `encode(buf)`, a buffer constructor and `handle(context)`; registered with a `NetworkDirection`; `sendToServer` on the wrapper | `NetworkWrapper(modId)` over vanilla `CustomPacketPayload` and Fabric's `PayloadTypeRegistry`; payloads are records with a `TYPE` and a `STREAM_CODEC` (a Mantle loadable can be the codec); `registerClientbound` / `registerServerbound` from the common entrypoint; serverbound payloads implement `IServerboundPacket.handle(ServerPlayer)`; clientbound receivers are registered from client code with Fabric's `ClientPlayNetworking`, which also sends to the server. The payload-record approach follows justduck25's Continuum-Mantle 26.1 port (D-15, D-23) | Forge's network API does not exist on Fabric; vanilla 26.x has typed payloads, and Loom keeps client receivers out of server code at compile time |
| `BlockEntityPacket` (for addon code) | `type()` gives the block entity class; `handleBlockEntity(context, be)` | `blockEntityClass()`; `handleBlockEntity(be)`; client code registers the receiver with `BlockEntityPacketHandler.register(type)`; a failed lookup logs one line instead of two | `type()` now clashes with `CustomPacketPayload.type()`; no Tinkers packet read the context |
| Datapack sync payloads (`JsonHelper.syncPackets`) | Called from Forge's `OnDatapackSyncEvent`, for one player or the whole list | Called per player from Fabric's `ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS`: `syncPackets(player, network, payloads...)`; still skipped for the single player host | Fabric fires the event once per player |
| Taking a book off a lectern through Mantle's book screen | Any client could take the book from any loaded lectern | Needs reach and build rights, like vanilla's own take-book button; the lectern is reset through vanilla's own code | Upstream security bug (A-003) |

## Not ported (yet or on purpose)

Every feature left out is listed here with its reason. Nothing is dropped silently.

| Feature | Upstream location | Reason | Tracking |
|---|---|---|---|
| Mantle registry adapters (`RegistryAdapter`, `BlockRegistryAdapter`, `ItemRegistryAdapter`, `FluidRegistryAdapter`, `EntityTypeRegistryAdapter`, `ContainerTypeRegistryAdapter`, `EnumRegistryAdapter`, `BlockEntityTypeRegistryAdapter`) | Mantle `registration/adapter` | Built for Forge's register event (construct first, register later). 26.x needs ids before construction, and Tinkers uses none of them; the deferred registers cover the same cases. Mantle's own sign block entities are replaced by vanilla's (see Behaviour differences) | I-008 |
| `MantleSignBlockEntity`, `MantleHangingSignBlockEntity`, `Mantle*SignBlock`, `MantleRegistrations`, `StrippableLogBlock`, `RegistrationHelper.registerWoodType`/`forEachWoodType` | Mantle `block`, `block/entity`, `registration` | Only existed to give signs a block entity type Forge could extend and to add wood types to the sign atlas. Vanilla sign types plus Fabric `addValidBlock` cover the first; 26.3 sign boards are block models, so no atlas list is needed | none needed |
| Load conditions: `ConditionalLoadable`, `GenericLoaderRegistry`'s `mantle:load_condition` loader and `getConditionalLoader`, `ConditionSerializer` | Mantle `data/loadable/mapping`, `data/registry`, `data/gson` | Built on Forge's `ICondition`; moves with recipe conditions onto Fabric resource conditions | T-44 |
| Mantle utils `TagHelper`, `IdExtender`, `SingleKeyMultimap`, `NonnullBiConsumer`, `WeakConsumerWrapper`, `LogicHelper.orElseNull` | Mantle `util` | Unused by Tinkers and Mantle, or only for Forge capabilities (`LazyOptional`), which Fabric does not have | none needed |
| Mantle utils `CombatHelper`, `RetexturedHelper`, `ItemLayerPixels`, `DataLoadedConditionContext` | Mantle `util` | Ported with the systems they belong to: combat events, models, conditions | T-60, T-70, T-44 |
| `SwingArmPacket` | Mantle `network/packet` | Only existed because the vanilla arm swing packet reset the attack cooldown. 26.3's `LivingEntity.swing(hand, animation, updateSelf)` sends its own swing packet without touching the cooldown, so `OffhandCooldownTracker.swingHand` will call that | none needed (T-18) |
| Book payloads `OpenLecternBookPacket`, `OpenNamedBookPacket`, `UpdateHeldPagePacket`, `UpdateInventoryPagePacket`, `UpdateLecternPagePacket` | Mantle `network/packet` | Their handlers need the book screen, `ILecternBookItem` and the saved page, which moves from NBT to a data component; ported with the book system | T-73 |
| `FluidContainerTransferPacket` | Mantle `fluid/transfer` | Needs Fabric fluids | T-50 |
| Network channel version check | Mantle `NetworkWrapper` | Forge refused to connect when the two sides' channel versions differed. Fabric has no per-channel versions; a mismatch shows up as a decode error instead. Registry sync still stops clients with different items or blocks | none planned |
| `ISafeManagerReloadListener`, `IEarlySafeManagerReloadListener` | Mantle `data/listener` | Only skipped a reload after Forge mod loading had failed, to avoid a misleading crash report. Fabric stops the game on a mod loading error, so use vanilla `ResourceManagerReloadListener` and Mantle `IEarlyReloadListener` | none needed |
| `MantleCodecs`; `GenericDataProvider` constructors taking a `DataGenerator` | Mantle `data` | The codecs wrapped Forge's loot Gson and Forge ingredients; 26.x has vanilla codecs (`LootPoolEntries.CODEC`, `LootItemFunctions`, `Ingredient.CODEC`). Fabric data generators hand out a `PackOutput`, which the remaining constructors take | none needed |
| `BuiltinRegistryTagProvider` | Mantle `data` | Extends vanilla's `IntrinsicHolderTagsProvider`, which 26.3 removed; decided together with the other tag providers | T-22 |
| Predicates `mantle:may_have_transfer` (items), `mantle:fluid_type` (fluids), and conditional predicates | Mantle `data/predicate` | Need Mantle's fluid transfer manager and Fabric fluids (T-50), or Forge conditions (T-44) | T-44, T-50 |
| `Loadables.TOOL_ACTION`, `Loadables.FLUID_TYPE`, `FluidStackLoadable` | Mantle `data/loadable` | Need Mantle's ToolAction (D-09) and Fabric fluids, which do not exist yet | T-37, T-50 |
| `RegistrationHelper.injected()` and `@ObjectHolder` fields | Mantle `registration` | Forge-only field injection; Fabric code holds `RegistryObject` handles instead | none needed |
| `RegistrationHelper.handleMissingMappings` (remap renamed ids in old worlds) | Mantle `registration` | No Forge MissingMappingsEvent and no registry alias API in 26.3; only matters for worlds made with older Tinkers versions | I-009 |
| Block entity data fixer types | Mantle `BlockEntityTypeDeferredRegister` | Removed from vanilla `BlockEntityType` in 26.x | none needed |

