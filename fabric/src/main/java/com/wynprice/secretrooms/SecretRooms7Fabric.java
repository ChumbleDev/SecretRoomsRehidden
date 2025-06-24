package com.wynprice.secretrooms;

import com.wynprice.secretrooms.server.SecretCreativeTab;
import com.wynprice.secretrooms.server.blocks.SecretBlocks;
import com.wynprice.secretrooms.server.items.SecretItems;
import com.wynprice.secretrooms.server.registry.FabricRegistryHolder;
import com.wynprice.secretrooms.server.tileentity.SecretTileEntities;
import net.fabricmc.api.ModInitializer;

public class SecretRooms7Fabric implements ModInitializer {
    @Override
    public void onInitialize() {
        // Register all the registries
        registerRegistries();
    }
    
    private void registerRegistries() {
        // Register blocks
        if (SecretBlocks.REGISTRY instanceof FabricRegistryHolder) {
            ((FabricRegistryHolder<?>) SecretBlocks.REGISTRY).register();
        }
        
        // Register items
        if (SecretItems.REGISTRY instanceof FabricRegistryHolder) {
            ((FabricRegistryHolder<?>) SecretItems.REGISTRY).register();
        }
        
        // Register tile entities
        if (SecretTileEntities.REGISTRY instanceof FabricRegistryHolder) {
            ((FabricRegistryHolder<?>) SecretTileEntities.REGISTRY).register();
        }
        
        // Register creative tab
        if (SecretCreativeTab.REGISTRY instanceof FabricRegistryHolder) {
            ((FabricRegistryHolder<?>) SecretCreativeTab.REGISTRY).register();
        }
    }
}
