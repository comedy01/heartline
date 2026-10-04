package dev.heartline.forge;

import dev.heartline.client.gui.HeartlineSettingsScreen;
import dev.heartline.hud.Canvas;
import dev.heartline.hud.HeartlineHud;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

final class ForgeClient {
    private ForgeClient() {
    }

    static void register(ModLoadingContext context, IEventBus modBus, KeyMapping toggle, KeyMapping hide) {
        modBus.addListener((FMLClientSetupEvent event) -> event.enqueueWork(() -> {
            ClientRegistry.registerKeyBinding(toggle);
            ClientRegistry.registerKeyBinding(hide);
        }));
        MinecraftForge.EVENT_BUS.addListener(ForgeClient::onOverlay);

        context.registerExtensionPoint(
                ExtensionPoint.CONFIGGUIFACTORY,
                () -> (client, parent) -> new HeartlineSettingsScreen(parent, client.options));
    }

    private static void onOverlay(RenderGameOverlayEvent.Post event) {
        if (event.getType() == RenderGameOverlayEvent.ElementType.ALL) {
            Minecraft mc = Minecraft.getInstance();
            HeartlineHud.render(mc, new Canvas(event.getMatrixStack(), mc.font), event.getPartialTicks());
        }
    }
}
