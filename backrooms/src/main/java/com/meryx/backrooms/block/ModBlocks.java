package com.meryx.backrooms.block;

import com.meryx.backrooms.Backrooms;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/** Level 0 is built from unbreakable blocks: the only way out is an exit door. */
public final class ModBlocks {
    private static final List<Item> ITEMS = new ArrayList<>();

    public static final Block WALLPAPER = register("wallpaper", Block::new,
            unbreakable().mapColor(MapColor.COLOR_YELLOW).sound(SoundType.WOOL));
    public static final Block MOIST_CARPET = register("moist_carpet", Block::new,
            unbreakable().mapColor(MapColor.TERRACOTTA_YELLOW).sound(SoundType.WOOL));
    public static final Block CEILING_TILE = register("ceiling_tile", Block::new,
            unbreakable().mapColor(MapColor.SAND).sound(SoundType.STONE));
    public static final Block FLUORESCENT_LIGHT = register("fluorescent_light", FluorescentLightBlock::new,
            unbreakable().mapColor(MapColor.SNOW).sound(SoundType.GLASS).randomTicks()
                    .lightLevel(state -> state.getValue(FluorescentLightBlock.LIT) ? 15 : 0));
    public static final Block EXIT_DOOR = register("exit_door", ExitDoorBlock::new,
            unbreakable().mapColor(MapColor.METAL).sound(SoundType.METAL).lightLevel(state -> 4));

    // Level 1, the Habitable Zone
    public static final Block CONCRETE = register("concrete", Block::new,
            unbreakable().mapColor(MapColor.STONE).sound(SoundType.STONE));
    public static final Block DAMP_CONCRETE = register("damp_concrete", Block::new,
            unbreakable().mapColor(MapColor.STONE).sound(SoundType.STONE));
    public static final Block CRATE = register("crate", Block::new,
            unbreakable().mapColor(MapColor.WOOD).sound(SoundType.WOOD));

    public static final Block HAZARD_CONCRETE = register("hazard_concrete", Block::new,
            unbreakable().mapColor(MapColor.STONE).sound(SoundType.STONE));

    // Level 2, Pipe Dreams: basements full of pipes
    public static final Block CINDERBLOCK = register("cinderblock", Block::new,
            unbreakable().mapColor(MapColor.COLOR_LIGHT_GRAY).sound(SoundType.STONE));
    public static final Block BASEMENT_FLOOR = register("basement_floor", Block::new,
            unbreakable().mapColor(MapColor.STONE).sound(SoundType.STONE));
    public static final Block FLOOR_DRAIN = register("floor_drain", Block::new,
            unbreakable().mapColor(MapColor.METAL).sound(SoundType.METAL));
    public static final Block PIPE = register("pipe", properties -> new PipeBlock(properties, 8.0),
            unbreakable().mapColor(MapColor.COLOR_BROWN).sound(SoundType.METAL).noOcclusion());
    public static final Block SMALL_PIPE = register("small_pipe", properties -> new PipeBlock(properties, 4.0),
            unbreakable().mapColor(MapColor.TERRACOTTA_ORANGE).sound(SoundType.COPPER).noOcclusion());
    public static final Block WARNING_LIGHT = register("warning_light", Block::new,
            unbreakable().mapColor(MapColor.COLOR_RED).sound(SoundType.GLASS).lightLevel(state -> 9));

    /**
     * The animated, see-through static that {@code Glitches} lays over blocks. As a block it has no collision: stack two
     * and step in to test the way into Level 0.
     */
    public static final Block GLITCH = register("glitch", Block::new,
            unbreakable().mapColor(MapColor.COLOR_BLACK).sound(SoundType.AMETHYST).noCollision().noOcclusion()
                    .lightLevel(state -> 3)
                    .isSuffocating((state, level, pos) -> false)
                    .isViewBlocking((state, level, pos) -> false));

    private ModBlocks() {
    }

    private static BlockBehaviour.Properties unbreakable() {
        return BlockBehaviour.Properties.of().strength(-1.0F, 3600000.0F).noLootTable();
    }

    private static Block register(String name, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties properties) {
        ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, Backrooms.id(name));
        Block block = Registry.register(BuiltInRegistries.BLOCK, blockKey, factory.apply(properties.setId(blockKey)));
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Backrooms.id(name));
        Item item = Registry.register(BuiltInRegistries.ITEM, itemKey,
                new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
        ITEMS.add(item);
        return block;
    }

    public static void init() {
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.BUILDING_BLOCKS).register(entries -> ITEMS.forEach(entries::accept));
    }
}
