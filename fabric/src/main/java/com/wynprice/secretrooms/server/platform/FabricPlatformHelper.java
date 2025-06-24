package com.wynprice.secretrooms.server.platform;

import com.wynprice.secretrooms.network.ApplyTexturePacket;
import com.wynprice.secretrooms.platform.services.ISecretRoomsPlatformHelper;
import com.wynprice.secretrooms.server.registry.RegistryHolder;
import com.wynprice.secretrooms.server.registry.FabricRegistryHolder;
import com.wynprice.secretrooms.server.tileentity.SecretTileEntity;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.tags.ItemTags;
import net.minecraft.resources.ResourceLocation;

import java.util.function.BiFunction;
import java.util.function.Supplier;

public class FabricPlatformHelper implements ISecretRoomsPlatformHelper {

    // Use a set to track pending updates and prevent duplicates
    private static final java.util.Set<BlockPos> pendingUpdates = java.util.concurrent.ConcurrentHashMap.newKeySet();

    @Override
    public RegistryHolder<Item> createItemRegistryHolder() {
        return new FabricRegistryHolder<>(BuiltInRegistries.ITEM);
    }

    @Override
    public RegistryHolder<Block> createBlockRegistryHolder() {
        return new FabricRegistryHolder<>(BuiltInRegistries.BLOCK);
    }

    @Override
    public RegistryHolder<BlockEntityType<?>> createBlockEntityRegistryHolder() {
        return new FabricRegistryHolder<>(BuiltInRegistries.BLOCK_ENTITY_TYPE);
    }

    @Override
    public RegistryHolder<CreativeModeTab> createCreativeTabRegistryHolder() {
        return new FabricRegistryHolder<>(BuiltInRegistries.CREATIVE_MODE_TAB);
    }

    @Override
    public CreativeModeTab.Builder createTabBuilder() {
        return FabricItemGroup.builder();
    }

    @Override
    public <T extends BlockEntity> BlockEntityType<T> createBlockEntityType(BiFunction<BlockPos, BlockState, T> creator, Supplier<Block>... blockSupplier) {
        Block[] blocks = new Block[blockSupplier.length];
        for (int i = 0; i < blockSupplier.length; i++) {
            blocks[i] = blockSupplier[i].get();
        }
        return FabricBlockEntityTypeBuilder.create(creator::apply, blocks).build();
    }

    @Override
    public TagKey<Item> getDyesItemTag() {
        // Use conventional fabric dyes tag
        return TagKey.create(Registries.ITEM, new ResourceLocation("c", "dyes"));
    }

    @Override
    public void updateModelData(SecretTileEntity tileEntity) {
        // Fabric-specific deferred model update to prevent infinite recursion
        if (tileEntity == null || tileEntity.getLevel() == null || tileEntity.getBlockPos() == null) {
            return;
        }
        
        BlockPos pos = tileEntity.getBlockPos();
        
        // Only schedule if not already pending
        if (pendingUpdates.add(pos)) {
            // Schedule the update for next tick to break recursion cycle
            if (tileEntity.getLevel() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                serverLevel.scheduleTick(pos, tileEntity.getBlockState().getBlock(), 1);
            } else if (tileEntity.getLevel().isClientSide) {
                // For client side, use a minimal approach
                try {
                    // Request chunk section update instead of full block update
                    tileEntity.getLevel().getChunkSource().getLightEngine().checkBlock(pos);
                } catch (Exception e) {
                    // Fallback to simple setChanged if the above fails
                    tileEntity.setChanged();
                }
            }
        }
    }

    @Override
    public void clearPendingUpdate(BlockPos pos) {
        pendingUpdates.remove(pos);
    }

    @Override
    public void sendPacketToServer(ApplyTexturePacket packet) {
        // Fabric networking - this would need to be implemented with Fabric's networking API
        // For now, leaving a placeholder - would need proper packet registration
        // ClientPlayNetworking.send(packet_id, packet_buf);
    }
}
