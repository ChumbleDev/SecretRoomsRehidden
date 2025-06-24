package com.wynprice.secretrooms.server.blocks.states;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.MapCodec;
import com.wynprice.secretrooms.client.world.DelegateWorld;
import com.wynprice.secretrooms.server.blocks.SecretBaseBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.SupportType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Optional;

public class OneWayGlassState extends SecretBaseState {

    public OneWayGlassState(Block block, ImmutableMap<Property<?>, Comparable<?>> propertiesToValueMap, MapCodec<BlockState> codec) {
        super(block, propertiesToValueMap, codec);
    }

    @Override
    public boolean canOcclude() {
        // OCCLUSION X-RAY FIX: OneWayGlass should NEVER occlude adjacent blocks
        // This prevents face culling that causes x-ray vision when blocks are placed next to glass
        // The previous stack trace hack was fragile and likely broken in current Forge versions
        return false;
    }

    @Override
    public VoxelShape getFaceOcclusionShape(BlockGetter worldIn, BlockPos pos, Direction directionIn) {
        Optional<BlockState> mirror = SecretBaseBlock.getMirrorState(worldIn, pos);
        DelegateWorld world = DelegateWorld.getPooled(worldIn);
        VoxelShape shape = this.getValue(SecretBaseBlock.SOLID) &&
            mirror.isPresent() &&
            !this.getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(directionIn)) &&
            mirror.get().isFaceSturdy(world, pos, directionIn, SupportType.CENTER) ?
            Shapes.block() : Shapes.empty();
        world.release();
        return shape;
    }



}
