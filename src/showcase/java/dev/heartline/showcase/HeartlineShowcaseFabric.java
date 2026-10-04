package dev.heartline.showcase;

import net.fabricmc.api.ClientModInitializer;

public final class HeartlineShowcaseFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Showcase.start();
    }
}
