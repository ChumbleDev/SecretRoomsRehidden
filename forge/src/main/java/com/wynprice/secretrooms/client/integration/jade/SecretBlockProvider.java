package com.wynprice.secretrooms.client.integration.jade;

import com.wynprice.secretrooms.SecretRooms7;
import com.wynprice.secretrooms.server.blocks.SecretBaseBlock;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import java.util.Optional;

public enum SecretBlockProvider implements IBlockComponentProvider {
    INSTANCE;

    @Override
    public ResourceLocation getUid() {
        return new ResourceLocation(SecretRooms7.MODID, "secret_block");
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        Optional.ofNullable(accessor.getLevel().getBlockEntity(accessor.getPosition()))
                .flatMap(entity -> SecretBaseBlock.getMirrorState(accessor.getLevel(), accessor.getPosition()))
                .ifPresent(state -> tooltip.add(Component.literal("Disguised as: " + state.getBlock().getName().getString())));
    }
} 