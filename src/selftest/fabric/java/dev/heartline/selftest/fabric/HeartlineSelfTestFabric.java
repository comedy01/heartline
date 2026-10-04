package dev.heartline.selftest.fabric;

import dev.heartline.selftest.HeartlineSelfTest;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public final class HeartlineSelfTestFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HeartlineSelfTest test = new HeartlineSelfTest();
        ClientTickEvents.END_CLIENT_TICK.register(client -> test.tick());
    }
}
