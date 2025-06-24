package com.wynprice.secretrooms.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

public class SwitchProbeTooltip implements ClientTooltipComponent {
    private final SwitchProbeTooltipComponent component;
    private static final int ICON_SIZE = 16;
    private static final int PADDING = 6;
    private static final int ICON_TEXT_GAP = 8;
    private static final int MIN_WIDTH = 200;  // Minimum width for better readability
    private static final float ANIMATION_SPEED = 0.03f;
    private static final float HOVER_SCALE = 0.05f;
    private static final float GLOW_INTENSITY = 0.3f;
    private static final float SPARKLE_SPEED = 0.1f;
    private static final int SPARKLE_COUNT = 3;
    
    private float animationTicks = 0;
    private float[] sparkleOffsets = new float[SPARKLE_COUNT];
    
    public SwitchProbeTooltip(SwitchProbeTooltipComponent component) {
        this.component = component;
        // Initialize sparkle offsets
        for (int i = 0; i < SPARKLE_COUNT; i++) {
            sparkleOffsets[i] = (float) (i * Math.PI * 2 / SPARKLE_COUNT);
        }
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics graphics) {
        // Update animations
        animationTicks += ANIMATION_SPEED;
        if (animationTicks > 2 * Math.PI) {
            animationTicks -= 2 * Math.PI;
        }

        // Calculate effects
        float hoverScale = 1.0f + Mth.sin(animationTicks) * HOVER_SCALE;
        float glowAlpha = (Mth.sin(animationTicks) + 1.0f) * 0.5f * GLOW_INTENSITY;
        
        int width = getWidth(font);
        int height = getHeight();
        
        // Draw background with fancy gradient based on block properties
        int bgColor = getBgColor();
        int borderColor = getBorderColor();
        int gradientEnd = (bgColor & 0xFEFEFE) >> 1; // Darker version of bgColor
        
        // Main background with gradient
        graphics.fillGradient(x, y, x + width, y + height, 
            bgColor | 0xFF000000,  // Top color
            gradientEnd | 0xDD000000); // Bottom color - slightly transparent
        
        // Animated border glow
        float borderGlow = (Mth.sin(animationTicks * 2) + 1.0f) * 0.5f;
        int glowBorder = interpolateColors(borderColor, 0xFFFFFFFF, borderGlow * 0.3f);
        
        // Border with glow effect
        graphics.fill(x, y, x + width, y + 1, glowBorder);  // Top
        graphics.fill(x, y + height - 1, x + width, y + height, glowBorder);  // Bottom
        graphics.fill(x, y, x + 1, y + height, glowBorder);  // Left
        graphics.fill(x + width - 1, y, x + width, y + height, glowBorder);  // Right
        
        // Render block/item preview if we have data
        if (component.hasBlockData()) {
            int iconX = x + PADDING;
            int iconY = y + (height - ICON_SIZE) / 2;
            
            // Draw sparkles
            if (!component.isError() && (component.getHardness() < 0 || component.getResistance() >= 12)) {
                for (int i = 0; i < SPARKLE_COUNT; i++) {
                    float sparkleAngle = animationTicks * SPARKLE_SPEED + sparkleOffsets[i];
                    float sparkleX = iconX + ICON_SIZE/2f + Mth.cos(sparkleAngle) * (ICON_SIZE * 0.8f);
                    float sparkleY = iconY + ICON_SIZE/2f + Mth.sin(sparkleAngle) * (ICON_SIZE * 0.8f);
                    float sparkleAlpha = (Mth.sin(sparkleAngle * 2) + 1.0f) * 0.5f;
                    
                    graphics.fill(
                        (int)sparkleX - 1, (int)sparkleY - 1,
                        (int)sparkleX + 1, (int)sparkleY + 1,
                        0xFFFFFF | ((int)(sparkleAlpha * 255) << 24)
                    );
                }
            }
            
            // Draw glow effect
            if (!component.isError()) {
                int glowColor = component.getSpriteColor();
                float gr = ((glowColor >> 16) & 0xFF) / 255F;
                float gg = ((glowColor >> 8) & 0xFF) / 255F;
                float gb = (glowColor & 0xFF) / 255F;
                
                // Outer glow
                graphics.setColor(gr, gg, gb, glowAlpha * 0.5f);
                graphics.fillGradient(
                    iconX - 4, iconY - 4,
                    iconX + ICON_SIZE + 4, iconY + ICON_SIZE + 4,
                    glowColor & 0x22FFFFFF,
                    glowColor & 0x00FFFFFF
                );
                
                // Inner glow
                graphics.setColor(gr, gg, gb, glowAlpha);
                graphics.fillGradient(
                    iconX - 2, iconY - 2,
                    iconX + ICON_SIZE + 2, iconY + ICON_SIZE + 2,
                    glowColor & 0x44FFFFFF,
                    glowColor & 0x00FFFFFF
                );
                
                graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            }
            
            // Try to render item first
            if(this.component.getItemRepresentation() != null) {
                graphics.pose().pushPose();
                graphics.pose().translate(iconX + ICON_SIZE/2f, iconY + ICON_SIZE/2f, 100);
                graphics.pose().scale(hoverScale, hoverScale, 1.0f);
                graphics.pose().translate(-(iconX + ICON_SIZE/2f), -(iconY + ICON_SIZE/2f), -100);
                
                graphics.renderItem(this.component.getItemRepresentation(), iconX, iconY, 0);
                graphics.renderItemDecorations(font, this.component.getItemRepresentation(), iconX, iconY);
                
                graphics.pose().popPose();
            } 
            // Otherwise render block sprite with color
            else if(this.component.getSprite() != null) {
                int color = this.component.getSpriteColor();
                float r = ((color >> 16) & 0xFF) / 255F;
                float g = ((color >> 8) & 0xFF) / 255F;
                float b = (color & 0xFF) / 255F;
                
                graphics.pose().pushPose();
                graphics.pose().translate(iconX + ICON_SIZE/2f, iconY + ICON_SIZE/2f, 0);
                graphics.pose().scale(hoverScale, hoverScale, 1.0f);
                graphics.pose().translate(-(iconX + ICON_SIZE/2f), -(iconY + ICON_SIZE/2f), 0);
                
                graphics.setColor(r, g, b, 1.0F);
                graphics.blit(iconX, iconY, 0, ICON_SIZE, ICON_SIZE, this.component.getSprite());
                graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
                
                graphics.pose().popPose();
            }
        }

        // Render text lines with shadow and highlight effect
        int textX = x + (component.hasBlockData() ? PADDING + ICON_SIZE + ICON_TEXT_GAP : PADDING);
        int textY = y + PADDING;
        
        for (FormattedCharSequence line : component.getLines()) {
            // Draw text shadow
            graphics.drawString(font, line, textX + 1, textY + 1, 0x4D000000, false);
            
            // Draw main text with slight vertical offset based on animation
            float textOffset = component.hasBlockData() ? Mth.sin(animationTicks + textY * 0.1f) * 0.5f : 0;
            graphics.drawString(font, line, textX, textY + (int)textOffset, 0xFFFFFF, false);
            
            textY += font.lineHeight;
        }
    }

    private int getBgColor() {
        if (component.isError()) {
            return 0x22FF0000;  // Red tint for errors
        }
        if (!component.hasBlockData()) {
            return 0x22000000;  // Dark for empty
        }
        // Color based on block properties
        if (component.getHardness() < 0) {
            return 0x22FF00FF;  // Purple tint for unbreakable
        }
        if (component.getResistance() >= 12) {
            return 0x2200FFFF;  // Cyan tint for high resistance
        }
        return 0x22000000;  // Default dark
    }

    private int getBorderColor() {
        if (component.isError()) {
            return 0x44FF0000;  // Red for errors
        }
        if (!component.hasBlockData()) {
            return 0x44000000;  // Dark for empty
        }
        // Color based on block properties
        if (component.getHardness() < 0 || component.getResistance() >= 12) {
            return 0x44FFFFFF;  // Bright for special blocks
        }
        return 0x44808080;  // Gray for normal blocks
    }

    private int interpolateColors(int color1, int color2, float factor) {
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;
        int a1 = (color1 >> 24) & 0xFF;
        
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;
        int a2 = (color2 >> 24) & 0xFF;
        
        int r = (int) (r1 + (r2 - r1) * factor);
        int g = (int) (g1 + (g2 - g1) * factor);
        int b = (int) (b1 + (b2 - b1) * factor);
        int a = (int) (a1 + (a2 - a1) * factor);
        
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    @Override
    public int getHeight() {
        int textHeight = component.getLines().size() * Minecraft.getInstance().font.lineHeight;
        return Math.max(ICON_SIZE + PADDING * 2, textHeight + PADDING * 2);
    }

    @Override
    public int getWidth(Font font) {
        int maxTextWidth = 0;
        for (FormattedCharSequence line : component.getLines()) {
            maxTextWidth = Math.max(maxTextWidth, font.width(line));
        }
        
        int iconWidth = component.hasBlockData() ? ICON_SIZE + ICON_TEXT_GAP : 0;
        return Math.max(MIN_WIDTH, PADDING * 2 + iconWidth + maxTextWidth);
    }
}
