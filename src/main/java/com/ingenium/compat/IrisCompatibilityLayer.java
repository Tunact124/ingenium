package com.ingenium.compat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class IrisCompatibilityLayer {
    private static final Logger LOG = LoggerFactory.getLogger("Ingenium");

    private static volatile boolean irisPresent;
    private static volatile boolean oculusPresent;
    private static volatile boolean sodiumPresent;

    private IrisCompatibilityLayer() {
    }

    public static void detectEarly() {
        irisPresent = isClassPresent(
                "net.irisshaders.iris.api.v0.IrisApi",
                "net.coderbot.iris.Iris"
        );
        oculusPresent = isClassPresent(
                "com.kiranthefox.oculus.Oculus",
                "com.irisshaders.oculus.OculusMod"
        );
        sodiumPresent = isClassPresent(
                "me.jellysquid.mods.sodium.client.SodiumClientMod",
                "net.caffeinemc.mods.sodium.client.SodiumClientMod"
        );
        LOG.info("[Ingenium] Detected: Iris={}, Oculus={}, Sodium={}", irisPresent, oculusPresent, sodiumPresent);
    }

    private static boolean isClassPresent(String... names) {
        for (String n : names) {
            try {
                Class.forName(n, false, IrisCompatibilityLayer.class.getClassLoader());
                return true;
            } catch (Throwable ignored) {
            }
        }
        return false;
    }

    public static String summary() {
        return "iris=" + irisPresent + ", oculus=" + oculusPresent + ", sodium=" + sodiumPresent;
    }

    public static boolean isIrisPresent() {
        return irisPresent;
    }

    public static boolean isOculusPresent() {
        return oculusPresent;
    }

    public static boolean isSodiumPresent() {
        return sodiumPresent;
    }
}
