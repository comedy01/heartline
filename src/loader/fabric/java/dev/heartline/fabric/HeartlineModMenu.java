package dev.heartline.fabric;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.heartline.client.gui.HeartlineSettingsScreen;
import net.minecraft.client.Minecraft;

public final class HeartlineModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> new HeartlineSettingsScreen(parent, Minecraft.getInstance().options);
    }
}
