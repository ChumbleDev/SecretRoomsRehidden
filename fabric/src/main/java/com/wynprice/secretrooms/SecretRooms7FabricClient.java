package com.wynprice.secretrooms;

import com.wynprice.secretrooms.client.FabricSecretBlockModel;
import com.wynprice.secretrooms.client.SecretModelHandler;
import com.wynprice.secretrooms.client.model.OneWayGlassModel;
import com.wynprice.secretrooms.client.model.SecretBlockModel;
import com.wynprice.secretrooms.client.model.SecretMappedModel;
import com.wynprice.secretrooms.server.blocks.SecretBlocks;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.minecraft.resources.ResourceLocation;

public class SecretRooms7FabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        SecretRooms7.LOGGER.info("Secret Rooms Fabric Client initializing...");
        
        // Register block color providers for mimic coloring
        registerBlockColors();
        
        // Register custom model loaders for Secret Rooms blocks
        ModelLoadingPlugin.register(pluginContext -> {
            SecretRooms7.LOGGER.info("Registering Secret Rooms model loaders...");
            
            // Register the secret_block loader
            pluginContext.addModels(new ResourceLocation(SecretRooms7.MODID, "secret_block"));
            pluginContext.modifyModelAfterBake().register((model, context) -> {
                ResourceLocation id = context.id();
                if (id.getNamespace().equals(SecretRooms7.MODID)) {
                    if (id.getPath().contains("secret_block") || 
                        id.getPath().contains("ghost_block") ||
                        id.getPath().contains("secret_lever") ||
                        id.getPath().contains("secret_stairs") ||
                        id.getPath().contains("secret_redstone") ||
                        id.getPath().contains("secret_wooden_button") ||
                        id.getPath().contains("secret_stone_button") ||
                        id.getPath().contains("secret_pressure_plate") ||
                        id.getPath().contains("secret_player_pressure_plate") ||
                        id.getPath().contains("secret_chest") ||
                        id.getPath().contains("secret_trapped_chest") ||
                        id.getPath().contains("secret_gate") ||
                        id.getPath().contains("secret_dummy_block") ||
                        id.getPath().contains("secret_daylight_detector") ||
                        id.getPath().contains("secret_observer") ||
                        id.getPath().contains("secret_clamber")) {
                        return new FabricSecretBlockModel(model, SecretBlockModel::new);
                    }
                    
                    if (id.getPath().contains("secret_door") || 
                        id.getPath().contains("secret_iron_door") ||
                        id.getPath().contains("secret_trapdoor") ||
                        id.getPath().contains("secret_iron_trapdoor")) {
                        return new FabricSecretBlockModel(model, SecretMappedModel::new);
                    }
                    
                    if (id.getPath().contains("one_way_glass")) {
                        return new FabricSecretBlockModel(model, OneWayGlassModel::new);
                    }
                }
                return model;
            });
        });
        
        SecretRooms7.LOGGER.info("Secret Rooms Fabric Client initialized successfully");
    }
    
    private void registerBlockColors() {
        // Register block color handlers using Fabric's API
        ColorProviderRegistry.BLOCK.register((state, world, pos, index) -> {
            if (world != null && pos != null) {
                return com.wynprice.secretrooms.server.blocks.SecretBaseBlock.getMirrorState(world, pos)
                    .map(mirror -> {
                        com.wynprice.secretrooms.client.world.DelegateWorld delegateWorld = 
                            com.wynprice.secretrooms.client.world.DelegateWorld.getPooled(world);
                        try {
                            return net.minecraft.client.Minecraft.getInstance().getBlockColors()
                                .getColor(mirror, delegateWorld, pos, index);
                        } finally {
                            delegateWorld.release();
                        }
                    })
                    .orElse(-1);
            }
            return -1;
        }, 
        SecretBlocks.GHOST_BLOCK.get(),
        SecretBlocks.SECRET_STAIRS.get(),
        SecretBlocks.SECRET_LEVER.get(),
        SecretBlocks.SECRET_REDSTONE.get(),
        SecretBlocks.ONE_WAY_GLASS.get(),
        SecretBlocks.SECRET_WOODEN_BUTTON.get(),
        SecretBlocks.SECRET_STONE_BUTTON.get(),
        SecretBlocks.SECRET_PRESSURE_PLATE.get(),
        SecretBlocks.SECRET_PLAYER_PRESSURE_PLATE.get(),
        SecretBlocks.SECRET_DOOR.get(),
        SecretBlocks.SECRET_IRON_DOOR.get(),
        SecretBlocks.SECRET_CHEST.get(),
        SecretBlocks.SECRET_TRAPDOOR.get(),
        SecretBlocks.SECRET_IRON_TRAPDOOR.get(),
        SecretBlocks.SECRET_TRAPPED_CHEST.get(),
        SecretBlocks.SECRET_GATE.get(),
        SecretBlocks.SECRET_DUMMY_BLOCK.get(),
        SecretBlocks.SECRET_DAYLIGHT_DETECTOR.get(),
        SecretBlocks.SECRET_OBSERVER.get(),
        SecretBlocks.SECRET_CLAMBER.get()
        );
    }
} 