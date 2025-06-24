package com.wynprice.secretrooms.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.wynprice.secretrooms.SecretRooms7;
import com.wynprice.secretrooms.network.ApplyTexturePacket;
import com.wynprice.secretrooms.network.SecretNetwork;
import com.wynprice.secretrooms.server.blocks.SecretBaseBlock;
import com.wynprice.secretrooms.server.data.SecretData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class TextureRemapperScreen extends Screen {
    // Use the standard GUI background texture instead of inventory
    private static final ResourceLocation BACKGROUND_TEXTURE = new ResourceLocation("textures/gui/container/dispenser.png");
    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 166;
    
    private final BlockState storedState;
    private final BlockState targetState;
    private final BlockPos targetPos;
    private int guiLeft;
    private int guiTop;
    
    private Button applyAllButton;
    private Button[] sideButtons;
    private Direction selectedSide = Direction.NORTH;

    public TextureRemapperScreen(BlockState storedState, BlockState targetState, BlockPos targetPos) {
        super(Component.translatable(SecretRooms7.MODID + ".gui.texture_remapper.title"));
        this.storedState = storedState;
        this.targetState = targetState;
        this.targetPos = targetPos;
    }

    @Override
    protected void init() {
        super.init();
        this.guiLeft = (this.width - GUI_WIDTH) / 2;
        this.guiTop = (this.height - GUI_HEIGHT) / 2;
        
        // Fixed direction mapping - correct the reversed mapping
        this.sideButtons = new Button[6];
        String[] sideNames = {"Down", "Up", "North", "South", "West", "East"}; // Fixed order
        Direction[] directions = {Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST}; // Fixed order
        
        for (int i = 0; i < 6; i++) {
            final Direction dir = directions[i];
            final String sideName = sideNames[i];
            int buttonX = this.guiLeft + 8 + (i % 2) * 80;
            int buttonY = this.guiTop + 25 + (i / 2) * 22;
            
            this.sideButtons[i] = this.addRenderableWidget(Button.builder(
                Component.literal(sideName), // Use literal instead of translatable for now
                (button) -> this.selectSide(dir))
                .pos(buttonX, buttonY)
                .size(75, 18)
                .build());
        }
        
        // Add apply buttons at the bottom
        this.addRenderableWidget(Button.builder(
            Component.literal("Apply Selected"),
            (button) -> this.applySelected())
            .pos(this.guiLeft + 8, this.guiTop + GUI_HEIGHT - 25)
            .size(75, 20)
            .build());
            
        this.applyAllButton = this.addRenderableWidget(Button.builder(
            Component.literal("Apply All"),
            (button) -> this.applyAll())
            .pos(this.guiLeft + GUI_WIDTH - 83, this.guiTop + GUI_HEIGHT - 25)
            .size(75, 20)
            .build());
        
        // Set initial selection
        this.selectSide(Direction.NORTH);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(graphics);
        
        // Draw the GUI background
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.blit(BACKGROUND_TEXTURE, this.guiLeft, this.guiTop, 0, 0, GUI_WIDTH, GUI_HEIGHT);
        
        // Draw title
        graphics.drawString(this.font, this.title, this.guiLeft + 8, this.guiTop + 6, 4210752, false);
        
        // Draw block previews in the right area
        this.renderBlockPreview(graphics, this.storedState, this.guiLeft + 90, this.guiTop + 100, "Stored Block:");
        this.renderBlockPreview(graphics, this.targetState, this.guiLeft + 90, this.guiTop + 120, "Target Block:");
        
        // Draw selected side indicator
        graphics.drawString(this.font, "Selected: " + this.selectedSide.name(), 
                           this.guiLeft + 8, this.guiTop + 95, 0x404040, false);
        
        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    private void renderBlockPreview(GuiGraphics graphics, BlockState state, int x, int y, String label) {
        // Draw label
        graphics.drawString(this.font, label, x, y - 8, 4210752, false);
        
        // Draw block preview
        if (state != null) {
            Block block = state.getBlock();
            TextureAtlasSprite sprite = Minecraft.getInstance().getBlockRenderer()
                .getBlockModelShaper()
                .getParticleIcon(state);
                
            // Properly bind the block atlas texture
            RenderSystem.setShaderTexture(0, InventoryMenu.BLOCK_ATLAS);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            
            // Render the sprite
            graphics.blit(x, y, 0, 16, 16, sprite);
            
            // Draw block name next to the sprite
            Component blockName = block.getName();
            graphics.drawString(this.font, blockName, x + 20, y + 4, 4210752, false);
        } else {
            graphics.drawString(this.font, "None", x + 20, y + 4, 0x808080, false);
        }
    }

    private void selectSide(Direction direction) {
        this.selectedSide = direction;
        // Update button states to show selection
        Direction[] directions = {Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
        for (int i = 0; i < 6; i++) {
            Direction buttonDir = directions[i];
            this.sideButtons[i].active = buttonDir != direction;
        }
    }

    private void applySelected() {
        if (this.targetState.getBlock() instanceof SecretBaseBlock) {
            SecretData data = new SecretData(null);
            data.setBlockState(this.storedState);
            System.out.println("DEBUG: Applying texture to side " + this.selectedSide + " at pos " + this.targetPos);
            SecretNetwork.INSTANCE.sendToServer(new ApplyTexturePacket(this.targetPos, this.selectedSide, false, data));
            this.onClose();
        }
    }

    private void applyAll() {
        if (this.targetState.getBlock() instanceof SecretBaseBlock) {
            SecretData data = new SecretData(null);
            data.setBlockState(this.storedState);
            System.out.println("DEBUG: Applying texture to all sides at pos " + this.targetPos);
            SecretNetwork.INSTANCE.sendToServer(new ApplyTexturePacket(this.targetPos, Direction.NORTH, true, data));
            this.onClose();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
} 