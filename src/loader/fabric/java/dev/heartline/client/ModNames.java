package dev.heartline.client;

import net.fabricmc.loader.api.FabricLoader;

public final class ModNames {
    private ModNames() {
    }

    public static String of(String modId) {
        return FabricLoader.getInstance().getModContainer(modId)
                .map(mod -> mod.getMetadata().getName())
                .orElse(modId);
    }
}
