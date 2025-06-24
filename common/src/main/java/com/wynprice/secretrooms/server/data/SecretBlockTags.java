package com.wynprice.secretrooms.server.data;

import com.wynprice.secretrooms.SecretRooms7;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public class SecretBlockTags {

    private static TagKey<Block> tag(String id) {
        return TagKey.create(Registries.BLOCK, new ResourceLocation(SecretRooms7.MODID, id));
    }
}
