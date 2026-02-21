package com.ingenium.collections;

import com.ingenium.core.Ingenium;
import com.ingenium.core.IngeniumRuntime;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public final class IngeniumCollections {
    private static final boolean KOLOBOKE_AVAILABLE = probeKoloboke();

    private IngeniumCollections() {}

    public static boolean kolobokeAvailable() {
        return KOLOBOKE_AVAILABLE;
    }

    public static <V> LongObjectMap<V> newRemovalHeavyLongObjectMap(int expectedSize) {
        // Since Koloboke is not yet in the project's dependencies and requires relocation,
        // we'll default to Fastutil for now to ensure compilation.
        // Once shading is configured, the reflective initialization below can be enabled.
        return new FastutilLongObjectMap<>(expectedSize);
    }

    private static boolean probeKoloboke() {
        try {
            Class.forName("com.ingenium.libs.koloboke.collect.map.hash.HashLongObjMaps");
            Class.forName("com.ingenium.libs.koloboke.collect.map.hash.HashLongObjMap");
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
