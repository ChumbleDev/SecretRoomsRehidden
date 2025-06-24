package com.wynprice.secretrooms;

import com.mojang.datafixers.util.Either;
import com.wynprice.secretrooms.client.SecretModelHandler;

import com.wynprice.secretrooms.client.SimpleUnbakedGeometryLoader;
import com.wynprice.secretrooms.client.SwitchProbeTooltip;
import com.wynprice.secretrooms.client.SwitchProbeTooltipComponent;
import com.wynprice.secretrooms.client.model.OneWayGlassModel;
import com.wynprice.secretrooms.client.model.SecretBlockModel;
import com.wynprice.secretrooms.client.model.SecretMappedModel;
import com.wynprice.secretrooms.server.SecretCreativeTab;
import com.wynprice.secretrooms.server.blocks.SecretBlocks;
import com.wynprice.secretrooms.server.data.SecretBlockLootTableProvider;
import com.wynprice.secretrooms.server.data.SecretBlockTagsProvider;
import com.wynprice.secretrooms.server.data.SecretItemTagsProvider;
import com.wynprice.secretrooms.server.data.SecretRecipeProvider;
import com.wynprice.secretrooms.server.items.SecretItems;
import com.wynprice.secretrooms.server.items.SwitchProbe;
import com.wynprice.secretrooms.server.items.TrueVisionGogglesClientHandler;
import com.wynprice.secretrooms.server.items.TrueVisionGogglesHandler;
import com.wynprice.secretrooms.server.registry.ForgeRegistryHolder;
import com.wynprice.secretrooms.server.tileentity.SecretTileEntities;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraft.client.Minecraft;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

@Mod(SecretRooms7.MODID)
public class SecretRooms7Forge {

    public SecretRooms7Forge() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        IEventBus forgeBus = MinecraftForge.EVENT_BUS;

        ((ForgeRegistryHolder<?>) SecretBlocks.REGISTRY).register(bus);
        ((ForgeRegistryHolder<?>) SecretItems.REGISTRY).register(bus);
        ((ForgeRegistryHolder<?>) SecretTileEntities.REGISTRY).register(bus);
        ((ForgeRegistryHolder<?>) SecretCreativeTab.REGISTRY).register(bus);

        bus.addListener(this::gatherData);
        forgeBus.addListener(this::modifyBreakSpeed);

        DistExecutor.runWhenOn(Dist.CLIENT, () -> () -> {
            // Register color handlers for proper block coloring
            bus.addListener((RegisterColorHandlersEvent.Block event) -> SecretModelHandler.onBlockColors(event::register));
            // bus.addListener((EntityRenderersEvent.RegisterLayerDefinitions event) -> SecretModelHandler.onEntityModelRegistered(event::registerLayerDefinition));
            // Resource reload listener removed with connected textures

            bus.addListener(SecretRooms7Forge::onClientSetup);
            bus.addListener(this::registerClientTooltipComponentFactory);
            bus.addListener((ModelEvent.RegisterGeometryLoaders event) -> {
                event.register("secret_block", SimpleUnbakedGeometryLoader.create(SecretBlockModel::new));
                event.register("secret_mapped_model", SimpleUnbakedGeometryLoader.create(SecretMappedModel::new));
                event.register("one_way_glass", SimpleUnbakedGeometryLoader.create(OneWayGlassModel::new));
            });
            
            // Refresh OneWayGlass model when models reload
            bus.addListener((ModelEvent.BakingCompleted event) -> OneWayGlassModel.refreshModel());

            // Register client tick event for True Vision Goggles
            forgeBus.addListener((net.minecraftforge.event.TickEvent.ClientTickEvent event) -> {
                if (event.phase == net.minecraftforge.event.TickEvent.Phase.END) {
                    TrueVisionGogglesClientHandler.onClientTick();
                }
            });

            // Initialize goggles state when joining world
            forgeBus.addListener((net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingIn event) -> {
                TrueVisionGogglesClientHandler.onClientWorldLoad();
            });

            forgeBus.addListener((RenderTooltipEvent.GatherComponents event) -> {
                ItemStack stack = event.getItemStack();
                if (stack.getItem() instanceof SwitchProbe) {
                    @SuppressWarnings("unchecked")
                    List<Either<Component, TooltipComponent>> tooltipElements = 
                        (List<Either<Component, TooltipComponent>>) (Object) event.getTooltipElements();
                    SwitchProbe.appendHover(stack, tooltipElements);
                }
            });
        });
    }

    public void modifyBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        Optional<BlockPos> position = event.getPosition();
        if (position.isEmpty()) {
            return;
        }
        SecretRooms7.modifyBreakSpeed(player, position.get()).ifPresent(event::setNewSpeed);
    }

    public void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();

        generator.addProvider(event.includeServer(), new SecretBlockLootTableProvider(output));
        generator.addProvider(event.includeServer(), new SecretRecipeProvider(output));

        SecretBlockTagsProvider blockTags = new SecretBlockTagsProvider(output, lookupProvider, existingFileHelper);
        generator.addProvider(event.includeServer(), blockTags);
        generator.addProvider(event.includeServer(), new SecretItemTagsProvider(output, lookupProvider, blockTags.contentsGetter(), existingFileHelper));
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            // Setup render layers for all blocks
            for (Supplier<Block> blockSupplier : SecretBlocks.REGISTRY.listAll()) {
                Block block = blockSupplier.get();
                
                if (block == SecretBlocks.ONE_WAY_GLASS.get()) {
                    // OneWayGlass needs cutout for transparency
                    ItemBlockRenderTypes.setRenderLayer(block, RenderType.cutout());
                } else if (block == SecretBlocks.TORCH_LEVER.get() || block == SecretBlocks.WALL_TORCH_LEVER.get()) {
                    // Torch levers need cutout for proper transparency (no black background)
                    ItemBlockRenderTypes.setRenderLayer(block, RenderType.cutout());
                } else {
                    // Normal blocks can render in any layer
                    ItemBlockRenderTypes.setRenderLayer(block, type -> true);
                }
            }
        });
    }

    public void registerClientTooltipComponentFactory(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(SwitchProbeTooltipComponent.class, SwitchProbeTooltip::new);
    }
}
