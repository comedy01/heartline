package dev.heartline.fabric;

import dev.heartline.client.HeartlineClient;
import dev.heartline.client.KeyRegistrar;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public final class HeartlineFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HeartlineClient.init(FabricLoader.getInstance().getConfigDir());
        HeartlineClient.setKeys(
                KeyRegistrar.register(HeartlineClient.createToggleKey()),
                KeyRegistrar.register(HeartlineClient.createHideKey()));
    }
}
