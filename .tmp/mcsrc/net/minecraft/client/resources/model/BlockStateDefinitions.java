/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.registries.BuiltInRegistries
 *  net.minecraft.resources.Identifier
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.StateDefinition
 *  net.minecraft.world.level.block.state.StateDefinition$Builder
 *  net.minecraft.world.level.block.state.properties.BlockStateProperties
 *  net.minecraft.world.level.block.state.properties.Property
 */
package net.minecraft.client.resources.model;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;

public class BlockStateDefinitions {
    private static final StateDefinition<Block, BlockState> ITEM_FRAME_FAKE_DEFINITION = BlockStateDefinitions.createItemFrameFakeState();
    private static final StateDefinition<Block, BlockState> GLOW_ITEM_FRAME_FAKE_DEFINITION = BlockStateDefinitions.createItemFrameFakeState();
    private static final Identifier GLOW_ITEM_FRAME_LOCATION = Identifier.withDefaultNamespace((String)"glow_item_frame");
    private static final Identifier ITEM_FRAME_LOCATION = Identifier.withDefaultNamespace((String)"item_frame");
    private static final Map<Identifier, StateDefinition<Block, BlockState>> STATIC_DEFINITIONS = Map.of(ITEM_FRAME_LOCATION, ITEM_FRAME_FAKE_DEFINITION, GLOW_ITEM_FRAME_LOCATION, GLOW_ITEM_FRAME_FAKE_DEFINITION);

    private static StateDefinition<Block, BlockState> createItemFrameFakeState() {
        return new StateDefinition.Builder((Object)Blocks.AIR).add(new Property[]{BlockStateProperties.MAP}).create(Block::defaultBlockState, BlockState::new);
    }

    public static BlockState getItemFrameFakeState(boolean isGlowing, boolean map) {
        return (BlockState)((BlockState)(isGlowing ? GLOW_ITEM_FRAME_FAKE_DEFINITION : ITEM_FRAME_FAKE_DEFINITION).any()).setValue((Property)BlockStateProperties.MAP, (Comparable)Boolean.valueOf(map));
    }

    public static Function<Identifier, StateDefinition<Block, BlockState>> definitionLocationToBlockStateMapper() {
        HashMap<Identifier, StateDefinition<Block, BlockState>> result = new HashMap<Identifier, StateDefinition<Block, BlockState>>(STATIC_DEFINITIONS);
        for (Block block : BuiltInRegistries.BLOCK) {
            result.put(block.builtInRegistryHolder().key().identifier(), (StateDefinition<Block, BlockState>)block.getStateDefinition());
        }
        return result::get;
    }
}

