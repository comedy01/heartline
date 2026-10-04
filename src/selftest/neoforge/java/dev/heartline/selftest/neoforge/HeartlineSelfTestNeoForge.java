package dev.heartline.selftest.neoforge;

import dev.heartline.selftest.HeartlineSelfTest;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = "heartline_selftest", dist = Dist.CLIENT)
public final class HeartlineSelfTestNeoForge {
    public HeartlineSelfTestNeoForge() {
        HeartlineSelfTest test = new HeartlineSelfTest();
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, event -> test.tick());
    }
}
