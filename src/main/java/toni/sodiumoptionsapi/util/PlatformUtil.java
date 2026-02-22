package toni.sodiumoptionsapi.util;

import net.fabricmc.loader.api.FabricLoader;

public class PlatformUtil {
    public static boolean modPresent(String modid) {
        return FabricLoader.getInstance().isModLoaded(modid);
    }
}
