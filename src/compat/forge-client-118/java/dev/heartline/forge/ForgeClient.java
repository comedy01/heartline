package dev.heartline.forge;

import dev.heartline.client.gui.HeartlineSettingsScreen;
import dev.heartline.hud.Canvas;
import dev.heartline.hud.HeartlineHud;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.ClientRegistry;
import net.minecraftforge.client.ConfigGuiHandler;
import net.minecraftforge.client.gui.OverlayRegistry;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

final class ForgeClient {
    private ForgeClient() {
    }

    static void register(ModLoadingContext context, IEventBus modBus, KeyMapping toggle, KeyMapping hide) {
        modBus.addListener((FMLClientSetupEvent event) -> event.enqueueWork(() -> {
            ClientRegistry.registerKeyBinding(toggle);
            ClientRegistry.registerKeyBinding(hide);
            OverlayRegistry.registerOverlayTop("Heartline Bars", (gui, poseStack, partialTick, width, height) -> {
                Minecraft mc = Minecraft.getInstance();
                HeartlineHud.render(mc, new Canvas(poseStack, mc.font), partialTick);
            });
        }));

        context.registerExtensionPoint(
                ConfigGuiHandler.ConfigGuiFactory.class,
                () -> new ConfigGuiHandler.ConfigGuiFactory(
                        (client, parent) -> new HeartlineSettingsScreen(parent, client.options)));
    }
}
