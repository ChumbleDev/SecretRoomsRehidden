package com.wynprice.secretrooms.client;

import com.wynprice.secretrooms.SecretRooms7;
import com.wynprice.secretrooms.server.data.SecretData;
import com.wynprice.secretrooms.server.items.SwitchProbe;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.PushReaction;

import java.util.ArrayList;
import java.util.List;

public class SwitchProbeTooltipComponent implements TooltipComponent {
    private final List<FormattedCharSequence> lines;
    private final TextureAtlasSprite sprite;
    private final ItemStack itemRepresentation;
    private final int spriteColor;
    private final boolean hasBlockData;
    private final boolean isError;
    private final float hardness;
    private final float resistance;

    public SwitchProbeTooltipComponent(ItemStack probeStack) {
        List<Component> textLines = new ArrayList<>();
        TextureAtlasSprite blockSprite = null;
        ItemStack fallbackItem = ItemStack.EMPTY;
        int color = 0xFFFFFF;
        boolean foundBlockData = false;
        boolean hasError = false;
        float blockHardness = 0;
        float blockResistance = 0;
        
        if (probeStack.hasTag() && probeStack.getTag().contains(SwitchProbe.PROBE_DATA)) {
            try {
                CompoundTag probeData = probeStack.getTag().getCompound(SwitchProbe.PROBE_DATA);
                if (!probeData.isEmpty()) {
                    SecretData data = new SecretData(null);
                    // Use client-side level if available, otherwise read raw NBT
                    Level level = Minecraft.getInstance().level;
                    data.readNBT(probeData, level);
                    BlockState state = data.getBlockState();
                    
                    if (state != null && !state.isAir()) {
                        Block block = state.getBlock();
                        blockHardness = state.getDestroySpeed(null, BlockPos.ZERO);
                        blockResistance = block.getExplosionResistance();
                        
                        // Create title line with block name
                        ChatFormatting rarityFormat = getRarityFormat(blockHardness, blockResistance);
                        Component blockName = Component.literal("✦ ")
                            .withStyle(ChatFormatting.GOLD)
                            .append(Component.translatable(SecretRooms7.MODID + ".probe.stored")
                                .withStyle(ChatFormatting.GREEN))
                            .append(": ")
                            .append(block.getName().copy().withStyle(rarityFormat, ChatFormatting.BOLD));
                        textLines.add(blockName);
                        
                        // Add block stats
                        textLines.add(Component.literal("")); // Spacing
                        textLines.add(Component.literal("✦ ")
                            .withStyle(ChatFormatting.BLUE)
                            .append(Component.translatable(SecretRooms7.MODID + ".probe.stats")
                                .withStyle(ChatFormatting.BLUE, ChatFormatting.ITALIC)));
                        
                        // Add hardness and resistance
                        addStatistic(textLines, "hardness", String.format("%.1f", blockHardness), getHardnessColor(blockHardness));
                        addStatistic(textLines, "resistance", String.format("%.1f", blockResistance), getResistanceColor(blockResistance));
                        
                        // Add block properties if any
                        if (!state.getProperties().isEmpty()) {
                            textLines.add(Component.literal("")); // Spacing
                            textLines.add(Component.literal("✦ ")
                                .withStyle(ChatFormatting.AQUA)
                                .append(Component.translatable(SecretRooms7.MODID + ".probe.properties")
                                    .withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC)));
                            
                            for (Property<?> property : state.getProperties()) {
                                String value = getPropertyString(property, state);
                                textLines.add(Component.literal("  • ")
                                    .withStyle(ChatFormatting.GRAY)
                                    .append(Component.literal(property.getName())
                                        .withStyle(ChatFormatting.WHITE))
                                    .append(Component.literal(": ")
                                        .withStyle(ChatFormatting.GRAY))
                                    .append(Component.literal(value)
                                        .withStyle(ChatFormatting.GREEN)));
                            }
                        }
                        
                        // Add block behavior info
                        textLines.add(Component.literal("")); // Spacing
                        textLines.add(Component.literal("✦ ")
                            .withStyle(ChatFormatting.LIGHT_PURPLE)
                            .append(Component.translatable(SecretRooms7.MODID + ".probe.behavior")
                                .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC)));
                        
                        // Add block characteristics
                        addBlockCharacteristic(textLines, "solid", state.canOcclude());
                        addBlockCharacteristic(textLines, "transparent", !state.canOcclude());
                        addBlockCharacteristic(textLines, "redstone", state.isRedstoneConductor(null, BlockPos.ZERO));
                        addBlockCharacteristic(textLines, "pushable", state.getPistonPushReaction() == PushReaction.NORMAL);
                        addBlockCharacteristic(textLines, "replaceable", state.canBeReplaced());
                        
                        // Add usage instructions
                        textLines.add(Component.literal("")); // Spacing
                        textLines.add(Component.literal("✦ ")
                            .withStyle(ChatFormatting.GRAY)
                            .append(Component.translatable(SecretRooms7.MODID + ".probe.usage")
                                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC)));
                        textLines.add(Component.literal("  • ")
                            .withStyle(ChatFormatting.DARK_GRAY)
                            .append(Component.translatable(SecretRooms7.MODID + ".probe.usage.apply")
                                .withStyle(ChatFormatting.DARK_GRAY)));
                        textLines.add(Component.literal("  • ")
                            .withStyle(ChatFormatting.DARK_GRAY)
                            .append(Component.translatable(SecretRooms7.MODID + ".probe.usage.clear")
                                .withStyle(ChatFormatting.DARK_GRAY)));
                        
                        Item item = block.asItem();
                        
                        // Try to get item representation first
                        if (item != Items.AIR) {
                            fallbackItem = new ItemStack(item);
                        } 
                        // Otherwise use block texture
                        else {
                            blockSprite = Minecraft.getInstance().getBlockRenderer()
                                .getBlockModelShaper()
                                .getParticleIcon(state);
                                
                            // Try to get block color
                            LocalPlayer player = Minecraft.getInstance().player;
                            if (player != null && Minecraft.getInstance().level != null) {
                                BlockPos playerPos = player.blockPosition();
                                
                                // Try block color handler first
                                int blockColor = Minecraft.getInstance().getBlockColors().getColor(
                                    state,
                                    Minecraft.getInstance().level,
                                    playerPos,
                                    0
                                );
                                
                                if (blockColor != -1) {
                                    color = blockColor;
                                } else {
                                    // Try biome colors
                                    Biome biome = Minecraft.getInstance().level.getBiome(playerPos).value();
                                    int biomeColor = biome.getGrassColor(playerPos.getX(), playerPos.getZ());
                                    if (biomeColor != -1) {
                                        color = biomeColor;
                                    } else {
                                        biomeColor = biome.getFoliageColor();
                                        if (biomeColor != -1) {
                                            color = biomeColor;
                                        }
                                    }
                                }
                            }
                        }
                        foundBlockData = true;
                    }
                }
            } catch (Exception e) {
                hasError = true;
                SecretRooms7.LOGGER.error("Error creating tooltip: ", e);
                textLines.add(Component.literal("⚠ ")
                    .withStyle(ChatFormatting.RED)
                    .append(Component.translatable(SecretRooms7.MODID + ".probe.error")
                        .withStyle(ChatFormatting.RED, ChatFormatting.BOLD)));
                textLines.add(Component.literal(e.getMessage())
                    .withStyle(ChatFormatting.RED));
            }
        }
        
        if (!foundBlockData && !hasError) {
            textLines.add(Component.translatable(SecretRooms7.MODID + ".probe.noneset")
                .withStyle(ChatFormatting.GRAY));
            textLines.add(Component.literal("")); // Spacing
            textLines.add(Component.literal("✦ ")
                .withStyle(ChatFormatting.YELLOW)
                .append(Component.translatable(SecretRooms7.MODID + ".probe.help")
                    .withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC)));
            textLines.add(Component.literal("  • ")
                .withStyle(ChatFormatting.GOLD)
                .append(Component.translatable(SecretRooms7.MODID + ".probe.help.store")
                    .withStyle(ChatFormatting.GRAY)));
            textLines.add(Component.literal("  • ")
                .withStyle(ChatFormatting.GOLD)
                .append(Component.translatable(SecretRooms7.MODID + ".probe.help.secret")
                    .withStyle(ChatFormatting.GRAY)));
        }
        
        this.lines = textLines.stream()
            .map(Component::getVisualOrderText)
            .toList();
        this.sprite = blockSprite;
        this.itemRepresentation = fallbackItem;
        this.spriteColor = color;
        this.hasBlockData = foundBlockData;
        this.isError = hasError;
        this.hardness = blockHardness;
        this.resistance = blockResistance;
    }

    private void addBlockCharacteristic(List<Component> lines, String key, boolean value) {
        lines.add(Component.literal("  • ")
            .withStyle(ChatFormatting.GRAY)
            .append(Component.translatable(SecretRooms7.MODID + ".probe." + key)
                .withStyle(ChatFormatting.WHITE))
            .append(": ")
            .append(Component.literal(String.valueOf(value))
                .withStyle(value ? ChatFormatting.GREEN : ChatFormatting.RED)));
    }

    private void addStatistic(List<Component> lines, String key, String value, ChatFormatting valueColor) {
        lines.add(Component.literal("  • ")
            .withStyle(ChatFormatting.GRAY)
            .append(Component.translatable(SecretRooms7.MODID + ".probe." + key)
                .withStyle(ChatFormatting.WHITE))
            .append(": ")
            .append(Component.literal(value)
                .withStyle(valueColor)));
    }

    private ChatFormatting getHardnessColor(float hardness) {
        if (hardness < 0) return ChatFormatting.LIGHT_PURPLE; // Unbreakable
        if (hardness == 0) return ChatFormatting.RED; // Instant break
        if (hardness < 1) return ChatFormatting.YELLOW;
        if (hardness < 3) return ChatFormatting.GREEN;
        if (hardness < 5) return ChatFormatting.BLUE;
        return ChatFormatting.DARK_PURPLE;
    }

    private ChatFormatting getResistanceColor(float resistance) {
        if (resistance <= 0) return ChatFormatting.RED;
        if (resistance < 3) return ChatFormatting.YELLOW;
        if (resistance < 6) return ChatFormatting.GREEN;
        if (resistance < 12) return ChatFormatting.BLUE;
        return ChatFormatting.LIGHT_PURPLE;
    }

    private ChatFormatting getRarityFormat(float hardness, float resistance) {
        if (hardness < 0 || resistance >= 12) return ChatFormatting.LIGHT_PURPLE;
        if (hardness >= 5 || resistance >= 6) return ChatFormatting.AQUA;
        if (hardness >= 3 || resistance >= 3) return ChatFormatting.YELLOW;
        return ChatFormatting.WHITE;
    }

    private <T extends Comparable<T>> String getPropertyString(Property<T> property, BlockState state) {
        return property.getName(state.getValue(property));
    }

    public List<FormattedCharSequence> getLines() {
        return this.lines;
    }

    public TextureAtlasSprite getSprite() {
        return sprite;
    }

    public ItemStack getItemRepresentation() {
        return itemRepresentation;
    }

    public int getSpriteColor() {
        return spriteColor;
    }

    public boolean hasBlockData() {
        return hasBlockData;
    }

    public boolean isError() {
        return isError;
    }

    public float getHardness() {
        return hardness;
    }

    public float getResistance() {
        return resistance;
    }
}

