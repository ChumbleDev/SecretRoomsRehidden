package com.wynprice.secretrooms.server.tileentity;

import com.wynprice.secretrooms.platform.SecretRoomsServices;
import com.wynprice.secretrooms.server.data.SecretData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import com.wynprice.secretrooms.SecretRooms7;

import javax.annotation.Nullable;

public class SecretTileEntity extends BlockEntity {

    private final SecretData data = new SecretData(this);
    boolean isSettingLevel = false; // Guard against recursion - package private for SecretData access

    public SecretTileEntity(BlockPos pos, BlockState state) {
        super(SecretTileEntities.SECRET_TILE_ENTITY.get(), pos, state);
    }

    public SecretTileEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        
        try {
            CompoundTag secretDataTag = new CompoundTag();
            this.data.writeNBT(secretDataTag);
            tag.put("secret_data", secretDataTag);
        } catch (Exception e) {
            SecretRooms7.LOGGER.error("Failed to save SecretTileEntity at {}: {}", this.worldPosition, e.getMessage());
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        
        try {
            if (tag.contains("secret_data")) {
                CompoundTag secretDataTag = tag.getCompound("secret_data");
                this.data.readNBT(secretDataTag, this.level);
                
                // Try to trigger model update after loading
                this.data.onLevelAvailable();
            }
        } catch (Exception e) {
            SecretRooms7.LOGGER.error("Failed to load SecretTileEntity at {}: {}", this.worldPosition, e.getMessage());
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag compound = super.getUpdateTag();
        this.saveAdditional(compound);
        return compound;
    }

    @Override
    @Nullable
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public SecretData getData() {
        return this.data;
    }

    public boolean isSettingLevel() {
        return this.isSettingLevel;
    }

    public void requestModelDataUpdateIfPossible() {
        try {
            if (this.level != null && this.getBlockPos() != null) {
                if (this.level.isClientSide) {
                    try {
                        // Use platform service to request model data update (Forge-specific)
                        SecretRoomsServices.PLATFORM.updateModelData(this);
                    } catch (Exception e) {
                        // Silently handle platform-specific errors
                    }
                    // Send block update to refresh visuals on client
                    this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 11);
                } else {
                    // On server, send update packet to clients
                    this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 2);
                }
            }
        } catch (Exception e) {
            SecretRooms7.LOGGER.warn("Failed to request model data update for SecretTileEntity at {}: {}", this.worldPosition, e.getMessage());
        }
    }

    @Override
    public void setLevel(Level level) {
        // Prevent infinite recursion when setting level
        if (this.isSettingLevel) {
            super.setLevel(level);
            return;
        }
        
        this.isSettingLevel = true;
        try {
            super.setLevel(level);
            
            // Attempt to load cached data when level becomes available
            if (level != null && this.data != null) {
                this.data.onLevelAvailable();
                
                // Do NOT call updateModelData here as it causes infinite recursion in Fabric
                // Visual updates will be handled by Minecraft's normal block update mechanisms
            }
        } catch (Exception e) {
            SecretRooms7.LOGGER.warn("Failed to handle level change for SecretTileEntity at {}: {}", 
                this.worldPosition != null ? this.worldPosition : "unknown", e.getMessage());
        } finally {
            this.isSettingLevel = false;
        }
    }
}
