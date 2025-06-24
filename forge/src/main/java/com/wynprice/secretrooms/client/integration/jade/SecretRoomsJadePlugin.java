package com.wynprice.secretrooms.client.integration.jade;

import com.wynprice.secretrooms.server.blocks.SecretBaseBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class SecretRoomsJadePlugin implements IWailaPlugin {

    @Override
    public void register(IWailaCommonRegistration registration) {
        // Common registration if needed
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(SecretBlockProvider.INSTANCE, SecretBaseBlock.class);
    }
} 