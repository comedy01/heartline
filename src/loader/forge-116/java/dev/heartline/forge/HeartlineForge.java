package dev.heartline.forge;

import dev.heartline.client.HeartlineClient;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.network.FMLNetworkConstants;
import org.apache.commons.lang3.tuple.Pair;

@Mod(HeartlineClient.MOD_ID)
public final class HeartlineForge {
    public HeartlineForge() {
        ModLoadingContext context = ModLoadingContext.get();
        context.registerExtensionPoint(
                ExtensionPoint.DISPLAYTEST,
                () -> Pair.of(() -> FMLNetworkConstants.IGNORESERVERONLY, (remote, fromServer) -> true));
        if (FMLEnvironment.dist != Dist.CLIENT) {
            return;
        }

        HeartlineClient.init(FMLPaths.CONFIGDIR.get());

        KeyMapping toggle = HeartlineClient.createToggleKey();
        KeyMapping hide = HeartlineClient.createHideKey();
        HeartlineClient.setKeys(toggle, hide);
        ForgeClient.register(context, FMLJavaModLoadingContext.get().getModEventBus(), toggle, hide);
    }
}
