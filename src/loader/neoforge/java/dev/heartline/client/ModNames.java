package dev.heartline.client;

import net.neoforged.fml.ModList;

public final class ModNames {
    private ModNames() {
    }

    public static String of(String modId) {
        return ModList.get().getModContainerById(modId)
                .map(mod -> mod.getModInfo().getDisplayName())
                .orElse(modId);
    }
}
