package com.wynprice.secretrooms.network;

import com.wynprice.secretrooms.server.blocks.OneWayGlass;
import com.wynprice.secretrooms.server.blocks.SecretBaseBlock;
import com.wynprice.secretrooms.server.data.SecretData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

public class ApplyTexturePacket {
    private final BlockPos pos;
    private final Direction side;
    private final boolean applyAll;
    private final SecretData data;

    public ApplyTexturePacket(BlockPos pos, Direction side, boolean applyAll, SecretData data) {
        this.pos = pos;
        this.side = side;
        this.applyAll = applyAll;
        this.data = data;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeInt(side.ordinal());
        buf.writeBoolean(applyAll);
        buf.writeNbt(data.writeNBT(new CompoundTag()));
    }

    public static ApplyTexturePacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        Direction side = Direction.values()[buf.readInt()];
        boolean applyAll = buf.readBoolean();
        SecretData data = new SecretData(null);
        data.readNBT(buf.readNbt(), null);
        return new ApplyTexturePacket(pos, side, applyAll, data);
    }

    public void handle(ServerPlayer player) {
        if (player != null) {
            BlockState state = player.level().getBlockState(pos);
            if (state.getBlock() instanceof SecretBaseBlock) {
                Optional<SecretData> targetData = SecretBaseBlock.getMirrorData(player.level(), pos);
                if (targetData.isPresent()) {
                    if (applyAll) {
                        // Apply texture to all sides
                        targetData.get().setFrom(data);
                        
                        // For OneWayGlass, also set all direction properties to false (mimic texture)
                        if (state.getBlock() instanceof OneWayGlass) {
                            BlockState newState = state
                                .setValue(PipeBlock.NORTH, false)
                                .setValue(PipeBlock.EAST, false)
                                .setValue(PipeBlock.SOUTH, false)
                                .setValue(PipeBlock.WEST, false)
                                .setValue(PipeBlock.UP, false)
                                .setValue(PipeBlock.DOWN, false);
                            player.level().setBlock(pos, newState, 3);
                        }
                    } else {
                        // Apply texture to specific side only
                        if (state.getBlock() instanceof OneWayGlass) {
                            // For OneWayGlass, set the specific direction property to false (mimic texture)
                            // and update the mimic texture data
                            targetData.get().setFrom(data);
                            
                            BlockState newState = state.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(side), false);
                            player.level().setBlock(pos, newState, 3);
                        } else {
                            // For other secret blocks, just replace the entire texture data
                            targetData.get().setFrom(data);
                        }
                    }
                    SecretBaseBlock.requestModelRefresh(player.level(), pos);
                    
                    System.out.println("DEBUG: Successfully applied texture to " + 
                                     (applyAll ? "all sides" : "side " + side) + 
                                     " at pos " + pos);
                }
            }
        }
    }

    public BlockPos getPos() {
        return pos;
    }

    public Direction getSide() {
        return side;
    }

    public boolean isApplyAll() {
        return applyAll;
    }

    public SecretData getData() {
        return data;
    }
} 