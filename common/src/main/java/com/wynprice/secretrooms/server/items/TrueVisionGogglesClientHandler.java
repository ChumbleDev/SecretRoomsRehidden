package com.wynprice.secretrooms.server.items;

import com.wynprice.secretrooms.server.tileentity.SecretTileEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;

public class TrueVisionGogglesClientHandler {

    private static boolean clientWearingItem;

    public static void onClientWorldLoad() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            clientWearingItem = isWearingGoggles(player);
        }
    }

    public static void onClientTick() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        
        boolean wearing = isWearingGoggles(player);
        if (wearing != clientWearingItem) {
            clientWearingItem = wearing;
            refreshSecretBlockRendering(player);
        }
    }
    
    private static void refreshSecretBlockRendering(LocalPlayer player) {
        LevelRenderer renderer = Minecraft.getInstance().levelRenderer;
        ClientLevel clientLevel = player.clientLevel;
        if (clientLevel == null) return;
        
        ClientChunkCache source = clientLevel.getChunkSource();
        ChunkPos chunkPos = new ChunkPos(player.blockPosition());
        int renderDistance = Minecraft.getInstance().options.renderDistance().get();
        
        for (int x = -renderDistance; x <= renderDistance; x++) {
            for (int z = -renderDistance; z <= renderDistance; z++) {
                if (source.hasChunk(chunkPos.x + x, chunkPos.z + z)) {
                    for (BlockEntity blockEntity : source.getChunk(chunkPos.x + x, chunkPos.z + z, false).getBlockEntities().values()) {
                        if (blockEntity instanceof SecretTileEntity) {
                            BlockPos pos = blockEntity.getBlockPos();
                            // Mark block for re-render to update outline visibility
                            renderer.setBlocksDirty(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY(), pos.getZ());
                        }
                    }
                }
            }
        }
    }

    public static boolean isWearingGoggles(Player player) {
        return player.getItemBySlot(EquipmentSlot.HEAD).getItem() == SecretItems.TRUE_VISION_GOGGLES.get();
    }
}
