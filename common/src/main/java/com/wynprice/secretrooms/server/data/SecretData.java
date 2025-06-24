package com.wynprice.secretrooms.server.data;

import com.wynprice.secretrooms.SecretRooms7;
import com.wynprice.secretrooms.platform.SecretRoomsServices;
import com.wynprice.secretrooms.server.blocks.SecretBaseBlock;
import com.wynprice.secretrooms.server.tileentity.SecretTileEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import javax.annotation.Nullable;

public class SecretData {

    @Nullable
    private final SecretTileEntity base;

    private BlockState blockState = null;
    private CompoundTag tileEntityNBT = null;
    
    // Cache for preserving NBT data when level is not available
    private CompoundTag cachedBlockStateNBT = null;
    private boolean hasValidData = false;
    private boolean isProcessingChange = false; // Flag to prevent infinite loops

    public SecretData(@Nullable SecretTileEntity base) {
        this.base = base;
    }

    public BlockState getBlockState() {
        // If we have a valid block state, return it (allow any valid block, not just non-secret ones)
        if (this.blockState != null && !this.blockState.isAir()) {
            return this.blockState;
        }
        
        // Try to load from cached NBT if available
        if (this.cachedBlockStateNBT != null && this.base != null && this.base.getLevel() != null) {
            try {
                this.loadFromCachedNBT();
                if (this.blockState != null && !this.blockState.isAir()) {
                    return this.blockState;
                }
            } catch (Exception e) {
                SecretRooms7.LOGGER.warn("Failed to load cached block state: " + e.getMessage());
            }
        }
        
        // Only use stone as absolute last resort when no data exists at all
        if (!this.hasValidData && this.cachedBlockStateNBT == null && this.blockState == null) {
            SecretRooms7.LOGGER.debug("No texture data available, using stone fallback");
            this.blockState = Blocks.STONE.defaultBlockState();
            this.hasValidData = true;
        }
        
        return this.blockState != null ? this.blockState : Blocks.STONE.defaultBlockState();
    }

    public void setBlockState(BlockState state) {
        if (state != null && !state.isAir()) {
            this.blockState = state;
            this.hasValidData = true;
            this.cachedBlockStateNBT = null; // Clear cache since we have direct data
            this.onChanged();
        }
    }

    @Nullable
    public CompoundTag getTileEntityNBT() {
        return tileEntityNBT;
    }

    public void setTileEntityNBT(@Nullable CompoundTag tileEntityNBT) {
        this.tileEntityNBT = tileEntityNBT;
        this.onChanged();
    }

    public void setFrom(SecretData other) {
        if (other.blockState != null) {
            this.setBlockState(other.blockState);
        }
        if (other.tileEntityNBT != null) {
            this.setTileEntityNBT(other.tileEntityNBT.copy());
        }
        if (other.cachedBlockStateNBT != null) {
            this.cachedBlockStateNBT = other.cachedBlockStateNBT.copy();
        }
        this.hasValidData = other.hasValidData;
        this.onChanged();
    }

    public CompoundTag writeNBT(CompoundTag tag) {
        try {
            // Always write the current block state if we have one
            if (this.blockState != null && !this.blockState.isAir() && 
                !(this.blockState.getBlock() instanceof SecretBaseBlock)) {
                
                // Try to write using level if available, otherwise use cached NBT
                if (this.base != null && this.base.getLevel() != null) {
                    try {
                        CompoundTag blockStateTag = NbtUtils.writeBlockState(this.blockState);
                        tag.put("blockstate", blockStateTag);
                    } catch (Exception e) {
                        // If writing with level fails, try manual approach
                        this.writeManualBlockState(tag);
                    }
                } else if (this.cachedBlockStateNBT != null) {
                    // Use cached NBT if no level available
                    tag.put("blockstate", this.cachedBlockStateNBT.copy());
                } else {
                    // Try manual approach as fallback
                    this.writeManualBlockState(tag);
                }
            } else if (this.cachedBlockStateNBT != null) {
                // If we don't have a direct block state but have cached NBT, use it
                tag.put("blockstate", this.cachedBlockStateNBT.copy());
            }
            
            tag.putBoolean("has_data", this.hasValidData);
            
        } catch (Exception e) {
            SecretRooms7.LOGGER.error("Failed to write SecretData NBT: {}", e.getMessage());
            // Don't let save failures crash the game
        }
        
        return tag;
    }

    public void readNBT(CompoundTag tag, @Nullable Level level) {
        try {
            this.hasValidData = tag.getBoolean("has_data");
            
            if (tag.contains("blockstate")) {
                CompoundTag blockStateTag = tag.getCompound("blockstate");
                
                if (level != null) {
                    try {
                        // Try to load the block state directly
                        BlockState loadedState = NbtUtils.readBlockState(level.holderLookup(Registries.BLOCK), blockStateTag);
                        if (loadedState != null && !loadedState.isAir()) {
                            // Accept any valid block state, not just non-secret ones
                            this.blockState = loadedState;
                            this.hasValidData = true;
                        }
                    } catch (Exception e) {
                        SecretRooms7.LOGGER.warn("Failed to read block state with level context: {}", e.getMessage());
                        // Cache the NBT for later loading
                        this.cachedBlockStateNBT = blockStateTag.copy();
                    }
                } else {
                    // Level not available, cache the NBT for later loading
                    this.cachedBlockStateNBT = blockStateTag.copy();
                }
            }
        } catch (Exception e) {
            SecretRooms7.LOGGER.error("Failed to read SecretData NBT: {}", e.getMessage());
        }
    }
    
    /**
     * Called when the level becomes available to try loading cached NBT
     */
    public void onLevelAvailable() {
        try {
            if (this.cachedBlockStateNBT != null && this.base != null && this.base.getLevel() != null) {
                this.loadFromCachedNBT();
            }
        } catch (Exception e) {
            SecretRooms7.LOGGER.error("Failed to load cached NBT when level became available: {}", e.getMessage());
        }
    }
    
    private void loadFromCachedNBT() {
        try {
            if (this.cachedBlockStateNBT != null && this.base != null && this.base.getLevel() != null) {
                BlockState loadedState = NbtUtils.readBlockState(
                    this.base.getLevel().holderLookup(Registries.BLOCK), 
                    this.cachedBlockStateNBT
                );
                if (loadedState != null && !loadedState.isAir()) {
                    this.blockState = loadedState;
                    this.hasValidData = true;
                    this.cachedBlockStateNBT = null; // Clear cache after successful loading
                    // Don't call onChanged during world loading to prevent hangs
                    // this.onChanged();
                }
            }
        } catch (Exception e) {
            SecretRooms7.LOGGER.warn("Failed to load cached block state: {}", e.getMessage());
        }
    }

    private boolean isValidBlockState(BlockState state) {
        return state != null && !state.isAir() && !(state.getBlock() instanceof SecretBaseBlock);
    }

    private void onChanged() {
        // Prevent infinite loops during world loading
        if (this.isProcessingChange) {
            return;
        }
        
        this.isProcessingChange = true;
        
        try {
            if (this.base != null && this.base.getLevel() != null && this.base.getBlockPos() != null) {
                this.base.setChanged();
                
                // Only request model refresh if we're on client side and not during level setting
                if (this.base.getLevel().isClientSide()) {
                    try {
                        // Use a more cautious approach to prevent recursion
                        if (this.base instanceof SecretTileEntity && !((SecretTileEntity) this.base).isSettingLevel()) {
                            SecretRoomsServices.PLATFORM.updateModelData(this.base);
                        }
                    } catch (Exception e) {
                        SecretRooms7.LOGGER.warn("Failed to update model data: {}", e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            SecretRooms7.LOGGER.error("Error in onChanged: {}", e.getMessage());
        } finally {
            this.isProcessingChange = false;
        }
    }

    public BlockEntity getTileEntityCache() {
        if (this.tileEntityNBT != null && this.base != null) {
            try {
                BlockState state = this.getBlockState();
                if (state != null && !state.isAir()) {
                    BlockEntity tileEntity = BlockEntity.loadStatic(this.base.getBlockPos(), state, this.tileEntityNBT);
                    if (tileEntity != null && this.base.getLevel() != null) {
                        tileEntity.setLevel(this.base.getLevel());
                        tileEntity.setBlockState(state);
                    }
                    return tileEntity;
                }
            } catch (Exception e) {
                SecretRooms7.LOGGER.warn("Failed to create tile entity cache: " + e.getMessage());
            }
        }
        return null;
    }

    private void writeManualBlockState(CompoundTag tag) {
        try {
            CompoundTag blockStateTag = new CompoundTag();
            String blockName = BuiltInRegistries.BLOCK.getKey(this.blockState.getBlock()).toString();
            blockStateTag.putString("Name", blockName);
            
            if (!this.blockState.getProperties().isEmpty()) {
                CompoundTag propertiesTag = new CompoundTag();
                for (var property : this.blockState.getProperties()) {
                    String propertyName = property.getName();
                    String propertyValue = this.blockState.getValue(property).toString();
                    propertiesTag.putString(propertyName, propertyValue);
                }
                blockStateTag.put("Properties", propertiesTag);
            }
            tag.put("blockstate", blockStateTag);
        } catch (Exception e) {
            SecretRooms7.LOGGER.warn("Failed to write block state manually: {}", e.getMessage());
        }
    }
}

