package com.wynprice.secretrooms.server.items;

import com.wynprice.secretrooms.SecretRooms7;
import com.wynprice.secretrooms.client.gui.TextureRemapperScreen;
import com.wynprice.secretrooms.server.blocks.SecretBaseBlock;
import com.wynprice.secretrooms.server.data.SecretData;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.List;

public class TextureRemapper extends Item {
    public static final String STORED_DATA = "stored_data";
    public static final String STORED_FACE = "stored_face";

    public TextureRemapper(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        BlockState clickedState = level.getBlockState(pos);
        
        try {
            // If clicked on a Secret Block and we have stored data, open the menu
            if (clickedState.getBlock() instanceof SecretBaseBlock) {
                if (stack.hasTag() && stack.getTag().contains(STORED_DATA)) {
                    if (level.isClientSide && player != null) {
                        SecretData data = new SecretData(null);
                        data.readNBT(stack.getTag().getCompound(STORED_DATA), level);
                        BlockState storedState = data.getBlockState();
                        Minecraft.getInstance().setScreen(new TextureRemapperScreen(storedState, clickedState, pos));
                        return InteractionResult.SUCCESS;
                    }
                    return InteractionResult.SUCCESS;
                } else {
                    if (!level.isClientSide && player != null) {
                        player.displayClientMessage(
                            Component.translatable(SecretRooms7.MODID + ".remapper.no_data")
                                .withStyle(ChatFormatting.RED), true);
                    }
                    return InteractionResult.FAIL;
                }
            }
            // Store data from non-Secret blocks
            else {
                // Don't store air or invalid blocks
                if (clickedState.isAir() || clickedState.getBlock() == Blocks.VOID_AIR) {
                    if (!level.isClientSide && player != null) {
                        player.displayClientMessage(
                            Component.translatable(SecretRooms7.MODID + ".remapper.invalid_block")
                                .withStyle(ChatFormatting.RED), true);
                    }
                    return InteractionResult.FAIL;
                }
                
                SecretData newData = new SecretData(null);
                newData.setBlockState(clickedState);
                
                // Store tile entity data if present
                BlockEntity blockEntity = level.getBlockEntity(pos);
                if (blockEntity != null) {
                    newData.setTileEntityNBT(blockEntity.saveWithFullMetadata());
                }
                
                // Save data to item
                CompoundTag tag = stack.getOrCreateTag();
                tag.put(STORED_DATA, newData.writeNBT(new CompoundTag()));
                
                if (!level.isClientSide && player != null) {
                    Block block = clickedState.getBlock();
                    player.displayClientMessage(
                        Component.translatable(SecretRooms7.MODID + ".remapper.stored")
                            .append(": ")
                            .append(block.getName())
                            .withStyle(ChatFormatting.GREEN), true);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        } catch (Exception e) {
            SecretRooms7.LOGGER.error("Error using Texture Remapper: ", e);
            if (!level.isClientSide && player != null) {
                player.displayClientMessage(
                    Component.translatable(SecretRooms7.MODID + ".remapper.error")
                        .withStyle(ChatFormatting.RED), true);
            }
            return InteractionResult.FAIL;
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        
        tooltip.add(Component.translatable(SecretRooms7.MODID + ".remapper.usage1")
            .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(SecretRooms7.MODID + ".remapper.usage2")
            .withStyle(ChatFormatting.GRAY));
        
        if (!stack.hasTag() || !stack.getTag().contains(STORED_DATA)) {
            tooltip.add(Component.translatable(SecretRooms7.MODID + ".remapper.noneset")
                .withStyle(ChatFormatting.YELLOW));
            return;
        }
        
        try {
            SecretData data = new SecretData(null);
            data.readNBT(stack.getTag().getCompound(STORED_DATA), level);
            BlockState state = data.getBlockState();
            Block block = state.getBlock();
            
            tooltip.add(Component.translatable(SecretRooms7.MODID + ".remapper.containedblock")
                .append(block.getName())
                .withStyle(ChatFormatting.GREEN));
        } catch (Exception e) {
            SecretRooms7.LOGGER.error("Error showing Texture Remapper tooltip: ", e);
            tooltip.add(Component.translatable(SecretRooms7.MODID + ".remapper.error")
                .withStyle(ChatFormatting.RED));
        }
    }
} 