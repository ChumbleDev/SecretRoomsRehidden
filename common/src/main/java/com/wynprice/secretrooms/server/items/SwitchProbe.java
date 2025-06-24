package com.wynprice.secretrooms.server.items;

import com.wynprice.secretrooms.SecretRooms7;
import com.wynprice.secretrooms.client.SwitchProbeTooltipComponent;
import com.wynprice.secretrooms.server.blocks.SecretBaseBlock;
import com.wynprice.secretrooms.server.data.SecretData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import com.mojang.datafixers.util.Either;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

/**
 * <br>
 * <p>Used to change the appearance of SecretRoomsMod blocks</p>
 * <a href="https://hexxit.fandom.com/wiki/Programmable_Switch_Probe">Wiki of the probe</a>
 *
 * <br><br>
 * Current Issues:
 * <ul>
 *     <li>Crashes upon use.</li>
 * </ul>
 */
public class SwitchProbe extends Item {

    public static final String PROBE_DATA = "probe_data";

    public SwitchProbe(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        
        // Clear data on shift-right click in air
        if (player.isShiftKeyDown()) {
            if (stack.hasTag() && stack.getTag().contains(PROBE_DATA)) {
                stack.removeTagKey(PROBE_DATA);
                if (!level.isClientSide) {
                    player.displayClientMessage(
                        Component.translatable(SecretRooms7.MODID + ".probe.cleared")
                            .withStyle(ChatFormatting.YELLOW), true);
                }
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
            }
        }
        
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        BlockState clickedState = level.getBlockState(pos);
        
        try {
            // If we have stored data and clicked on a Secret Block, apply the data
            if (stack.hasTag() && stack.getTag().contains(PROBE_DATA) && clickedState.getBlock() instanceof SecretBaseBlock) {
                CompoundTag probeData = stack.getTag().getCompound(PROBE_DATA);
                if (!probeData.isEmpty()) {
                    Optional<SecretData> targetData = SecretBaseBlock.getMirrorData(level, pos);
                    if (targetData.isPresent()) {
                        SecretData storedData = new SecretData(null);
                        try {
                            storedData.readNBT(probeData, level);
                            targetData.get().setFrom(storedData);
                            
                            // Request model refresh and notify player
                            SecretBaseBlock.requestModelRefresh(level, pos);
                            if (!level.isClientSide && player != null) {
                                Block storedBlock = storedData.getBlockState().getBlock();
                                player.displayClientMessage(
                                    Component.translatable(SecretRooms7.MODID + ".probe.applied")
                                        .append(": ")
                                        .append(storedBlock.getName())
                                        .withStyle(ChatFormatting.GREEN), true);
                            }
                            return InteractionResult.sidedSuccess(level.isClientSide);
                        } catch (Exception e) {
                            SecretRooms7.LOGGER.error("Failed to apply probe data: ", e);
                            if (!level.isClientSide && player != null) {
                                player.displayClientMessage(
                                    Component.translatable(SecretRooms7.MODID + ".probe.error")
                                        .withStyle(ChatFormatting.RED), true);
                            }
                            return InteractionResult.FAIL;
                        }
                    }
                }
            }
            // Store data from non-Secret blocks
            else if (!(clickedState.getBlock() instanceof SecretBaseBlock) && !clickedState.isAir()) {
                try {
                    SecretData newData = new SecretData(null);
                    newData.setBlockState(clickedState);
                    
                    // Store tile entity data if present
                    BlockEntity blockEntity = level.getBlockEntity(pos);
                    if (blockEntity != null) {
                        CompoundTag tileNBT = blockEntity.saveWithFullMetadata();
                        if (tileNBT != null && !tileNBT.isEmpty()) {
                            newData.setTileEntityNBT(tileNBT);
                        }
                    }
                    
                    // Save data to item
                    CompoundTag tag = stack.getOrCreateTag();
                    CompoundTag dataTag = newData.writeNBT(new CompoundTag());
                    if (!dataTag.isEmpty()) {
                        tag.put(PROBE_DATA, dataTag);
                        
                        if (!level.isClientSide && player != null) {
                            Block block = clickedState.getBlock();
                            player.displayClientMessage(
                                Component.translatable(SecretRooms7.MODID + ".probe.stored")
                                    .append(": ")
                                    .append(block.getName())
                                    .withStyle(ChatFormatting.GREEN), true);
                        }
                        return InteractionResult.sidedSuccess(level.isClientSide);
                    } else {
                        if (!level.isClientSide && player != null) {
                            player.displayClientMessage(
                                Component.translatable(SecretRooms7.MODID + ".probe.error")
                                    .append(": No data to store")
                                    .withStyle(ChatFormatting.RED), true);
                        }
                        return InteractionResult.FAIL;
                    }
                } catch (Exception e) {
                    SecretRooms7.LOGGER.error("Failed to store block data in probe: ", e);
                    if (!level.isClientSide && player != null) {
                        player.displayClientMessage(
                            Component.translatable(SecretRooms7.MODID + ".probe.error")
                                .withStyle(ChatFormatting.RED), true);
                    }
                    return InteractionResult.FAIL;
                }
            }
            
            return InteractionResult.PASS;
        } catch (Exception e) {
            SecretRooms7.LOGGER.error("Error using Switch Probe: ", e);
            if (!level.isClientSide && player != null) {
                player.displayClientMessage(
                    Component.translatable(SecretRooms7.MODID + ".probe.error")
                        .withStyle(ChatFormatting.RED), true);
            }
            return InteractionResult.FAIL;
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        
        if (!stack.hasTag() || !stack.getTag().contains(PROBE_DATA)) {
            tooltip.add(Component.translatable(SecretRooms7.MODID + ".probe.noneset")
                .withStyle(ChatFormatting.GRAY));
            return;
        }
        
        try {
            CompoundTag probeData = stack.getTag().getCompound(PROBE_DATA);
            if (probeData.isEmpty()) {
                tooltip.add(Component.translatable(SecretRooms7.MODID + ".probe.noneset")
                    .withStyle(ChatFormatting.GRAY));
                return;
            }
            
            SecretData data = new SecretData(null);
            data.readNBT(probeData, level);
            BlockState state = data.getBlockState();
            
            if (state != null && !state.isAir()) {
                Block block = state.getBlock();
                tooltip.add(Component.translatable(SecretRooms7.MODID + ".probe.containedblock")
                    .append(" ")
                    .append(block.getName())
                    .withStyle(ChatFormatting.GREEN));
                
                // Add usage instructions
                tooltip.add(Component.translatable(SecretRooms7.MODID + ".probe.usage")
                    .withStyle(ChatFormatting.GRAY));
            } else {
                tooltip.add(Component.translatable(SecretRooms7.MODID + ".probe.error")
                    .withStyle(ChatFormatting.RED));
            }
        } catch (Exception e) {
            SecretRooms7.LOGGER.error("Error displaying probe tooltip: ", e);
            tooltip.add(Component.translatable(SecretRooms7.MODID + ".probe.error")
                .withStyle(ChatFormatting.RED));
        }
    }

    public static void appendHover(ItemStack stack, List<Either<Component,TooltipComponent>> tooltipElements) {
        if (stack.getItem() instanceof SwitchProbe && stack.hasTag() && stack.getTag().contains(PROBE_DATA)) {
            tooltipElements.add(Either.right(new SwitchProbeTooltipComponent(stack)));
        }
    }
}
