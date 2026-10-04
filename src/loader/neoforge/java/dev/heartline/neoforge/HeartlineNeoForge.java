package dev.heartline.neoforge;

import dev.heartline.client.HeartlineClient;
import dev.heartline.client.gui.HeartlineSettingsScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = HeartlineClient.MOD_ID, dist = Dist.CLIENT)
public final class HeartlineNeoForge {
    public HeartlineNeoForge(IEventBus modBus, ModContainer container) {
        HeartlineClient.init(FMLPaths.CONFIGDIR.get());

        KeyMapping toggle = HeartlineClient.createToggleKey();
        KeyMapping hide = HeartlineClient.createHideKey();
        HeartlineClient.setKeys(toggle, hide);
        modBus.addListener(RegisterKeyMappingsEvent.class, event -> {
            event.register(toggle);
            event.register(hide);
        });

        container.registerExtensionPoint(
                IConfigScreenFactory.class,
                (modContainer, parent) -> new HeartlineSettingsScreen(parent, Minecraft.getInstance().options));
    }
}
