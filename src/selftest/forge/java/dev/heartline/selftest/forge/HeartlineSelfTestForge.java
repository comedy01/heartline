package dev.heartline.selftest.forge;

import dev.heartline.selftest.HeartlineSelfTest;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;

@Mod("heartline_selftest")
public final class HeartlineSelfTestForge {
    public HeartlineSelfTestForge() {
        HeartlineSelfTest test = new HeartlineSelfTest();
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END) {
                test.tick();
            }
        });
    }
}
