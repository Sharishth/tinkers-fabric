package slimeknights.mantle.registration.object;

import lombok.Getter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CeilingHangingSignBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallHangingSignBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.state.properties.WoodType;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static slimeknights.mantle.registration.RegistrationHelper.getCastedHolder;

/** Extension of the fence object with all other wood blocks */
@SuppressWarnings("unused")
public class WoodBlockObject extends FenceBuildingBlockObject {
  @Getter
  private final WoodType woodType;
  // basic
  private final Supplier<? extends Block> log;
  private final Supplier<? extends Block> strippedLog;
  private final Supplier<? extends Block> wood;
  private final Supplier<? extends Block> strippedWood;
  // doors
  private final Supplier<? extends FenceGateBlock> fenceGate;
  private final Supplier<? extends DoorBlock> door;
  private final Supplier<? extends TrapDoorBlock> trapdoor;
  // redstone
  private final Supplier<? extends PressurePlateBlock> pressurePlate;
  private final Supplier<? extends ButtonBlock> button;
  // signs
  private final Supplier<? extends StandingSignBlock> sign;
  private final Supplier<? extends WallSignBlock> wallSign;
  private final Supplier<? extends CeilingHangingSignBlock> hangingSign;
  private final Supplier<? extends WallHangingSignBlock> wallHangingSign;
  // tags
  /** Block tag holding the logs and wood of this type, {@code <name>_logs} */
  @Getter
  private final TagKey<Block> logBlockTag;
  /** Item tag holding the logs and wood of this type, {@code <name>_logs} */
  @Getter
  private final TagKey<Item> logItemTag;

  public WoodBlockObject(Identifier name, WoodType woodType, BuildingBlockObject planks,
                         Supplier<? extends Block> log, Supplier<? extends Block> strippedLog, Supplier<? extends Block> wood, Supplier<? extends Block> strippedWood,
                         Supplier<? extends FenceBlock> fence, Supplier<? extends FenceGateBlock> fenceGate, Supplier<? extends DoorBlock> door, Supplier<? extends TrapDoorBlock> trapdoor,
                         Supplier<? extends PressurePlateBlock> pressurePlate, Supplier<? extends ButtonBlock> button,
                         Supplier<? extends StandingSignBlock> sign, Supplier<? extends WallSignBlock> wallSign,
                         Supplier<? extends CeilingHangingSignBlock> hangingSign, Supplier<? extends WallHangingSignBlock> wallHangingSign) {
    super(planks, fence);
    this.woodType = woodType;
    this.log = log;
    this.strippedLog = strippedLog;
    this.wood = wood;
    this.strippedWood = strippedWood;
    this.fenceGate = fenceGate;
    this.door = door;
    this.trapdoor = trapdoor;
    this.pressurePlate = pressurePlate;
    this.button = button;
    this.sign = sign;
    this.wallSign = wallSign;
    this.hangingSign = hangingSign;
    this.wallHangingSign = wallHangingSign;
    Identifier tagName = name.withSuffix("_logs");
    this.logBlockTag = TagKey.create(Registries.BLOCK, tagName);
    this.logItemTag = TagKey.create(Registries.ITEM, tagName);
  }

  /** Creates a wood object from blocks that are already registered */
  public WoodBlockObject(Identifier name, WoodType woodType, BuildingBlockObject planks,
                         Block log, Block strippedLog, Block wood, Block strippedWood,
                         Block fence, Block fenceGate, Block door, Block trapdoor,
                         Block pressurePlate, Block button,
                         Block sign, Block wallSign, Block hangingSign, Block wallHangingSign) {
    this(name, woodType, planks,
         getCastedHolder(BuiltInRegistries.BLOCK, log), getCastedHolder(BuiltInRegistries.BLOCK, strippedLog),
         getCastedHolder(BuiltInRegistries.BLOCK, wood), getCastedHolder(BuiltInRegistries.BLOCK, strippedWood),
         getCastedHolder(BuiltInRegistries.BLOCK, fence), getCastedHolder(BuiltInRegistries.BLOCK, fenceGate),
         getCastedHolder(BuiltInRegistries.BLOCK, door), getCastedHolder(BuiltInRegistries.BLOCK, trapdoor),
         getCastedHolder(BuiltInRegistries.BLOCK, pressurePlate), getCastedHolder(BuiltInRegistries.BLOCK, button),
         getCastedHolder(BuiltInRegistries.BLOCK, sign), getCastedHolder(BuiltInRegistries.BLOCK, wallSign),
         getCastedHolder(BuiltInRegistries.BLOCK, hangingSign), getCastedHolder(BuiltInRegistries.BLOCK, wallHangingSign));
  }

  /** Gets the log for this wood type */
  public Block getLog() {
    return log.get();
  }

  /** Gets the stripped log for this wood type */
  public Block getStrippedLog() {
    return strippedLog.get();
  }

  /** Gets the wood for this wood type */
  public Block getWood() {
    return wood.get();
  }

  /** Gets the stripped wood for this wood type */
  public Block getStrippedWood() {
    return strippedWood.get();
  }

  /* Doors */

  /** Gets the fence gate for this wood type */
  public FenceGateBlock getFenceGate() {
    return fenceGate.get();
  }

  /** Gets the door for this wood type */
  public DoorBlock getDoor() {
    return door.get();
  }

  /** Gets the trapdoor for this wood type */
  public TrapDoorBlock getTrapdoor() {
    return trapdoor.get();
  }

  /* Redstone */

  /** Gets the pressure plate for this wood type */
  public PressurePlateBlock getPressurePlate() {
    return pressurePlate.get();
  }

  /** Gets the button for this wood type */
  public ButtonBlock getButton() {
    return button.get();
  }

  /* Signs */

  /** Gets the sign for this wood type, can also be used to get the item */
  public StandingSignBlock getSign() {
    return sign.get();
  }

  /** Gets the wall sign for this wood type */
  public WallSignBlock getWallSign() {
    return wallSign.get();
  }

  /** Gets the hanging sign for this wood type, can also be used to get the item */
  public CeilingHangingSignBlock getHangingSign() {
    return hangingSign.get();
  }

  /** Gets the wall hanging sign for this wood type */
  public WallHangingSignBlock getWallHangingSign() {
    return wallHangingSign.get();
  }

  @Override
  public List<Block> values() {
    return List.of(
      get(), getSlab(), getStairs(), getFence(),
      getLog(), getStrippedLog(), getWood(), getStrippedWood(),
      getFenceGate(), getDoor(), getTrapdoor(),
      getPressurePlate(), getButton(),
      getSign(), getWallSign(), getHangingSign(), getWallHangingSign());
  }

  /** Visits every block with an item form; wall signs share their standing sign's item, so they are skipped */
  @Override
  public void forEach(Consumer<? super Block> consumer) {
    super.forEach(consumer);
    consumer.accept(getFenceGate());
    consumer.accept(getLog());
    consumer.accept(getStrippedLog());
    consumer.accept(getWood());
    consumer.accept(getStrippedWood());
    consumer.accept(getDoor());
    consumer.accept(getTrapdoor());
    consumer.accept(getPressurePlate());
    consumer.accept(getButton());
    consumer.accept(getSign());
    consumer.accept(getHangingSign());
  }

  /** Variants of wood for the register function */
  public enum WoodVariant { LOG, WOOD, PLANKS }
}
