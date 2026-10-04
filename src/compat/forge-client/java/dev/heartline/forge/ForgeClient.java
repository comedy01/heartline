package dev.heartline.forge;

import dev.heartline.client.gui.HeartlineSettingsScreen;
import dev.heartline.hud.Canvas;
import dev.heartline.hud.HeartlineHud;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;

final class ForgeClient {
    private ForgeClient() {
    }

    static void register(ModLoadingContext context, IEventBus modBus, KeyMapping toggle, KeyMapping hide) {
        modBus.addListener((RegisterKeyMappingsEvent event) -> {
            event.register(toggle);
            event.register(hide);
        });
        modBus.addListener((RegisterGuiOverlaysEvent event) -> event.registerAboveAll("bars",
                (gui, graphics, partialTick, width, height) -> {
                    Minecraft mc = Minecraft.getInstance();
                    HeartlineHud.render(mc, new Canvas(graphics, mc.font), partialTick);
                }));

        context.registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                        (client, parent) -> new HeartlineSettingsScreen(parent, client.options)));
    }
}
