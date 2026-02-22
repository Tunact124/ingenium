package com.ingenium.render;

import com.ingenium.compat.BuddyLogic;
import com.ingenium.compat.BuddyLogic.KnownMod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper that provides Sodium-accelerated vertex emission when available.
 */
public final class SodiumVertexHelper {

    private static final Logger LOG = LoggerFactory.getLogger("Ingenium/SodiumVertex");

    private static final boolean AVAILABLE;
    private static final java.lang.reflect.Method PUSH_METHOD;
    private static final Class<?> VERTEX_FORMAT_CLASS;

    static {
        boolean available = false;
        java.lang.reflect.Method pushMethod = null;
        Class<?> formatClass = null;

        if (BuddyLogic.isPresent(KnownMod.SODIUM)
            && BuddyLogic.getResult(KnownMod.SODIUM).hasCapability("vertex_writer_api")) {

            try {
                Class<?> writerClass = Class.forName("net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter");
                formatClass = Class.forName("net.caffeinemc.mods.sodium.api.vertex.format.VertexFormatDescription");

                pushMethod = writerClass.getMethod("push",
                    org.lwjgl.system.MemoryStack.class, long.class, int.class, formatClass);

                available = true;
                LOG.info("[Ingenium] Sodium VertexBufferWriter resolved — bulk vertex emission ACTIVE");

            } catch (Exception e) {
                LOG.warn("[Ingenium] Failed to resolve Sodium vertex API: {}", e.getMessage());
            }
        }

        AVAILABLE = available;
        PUSH_METHOD = pushMethod;
        VERTEX_FORMAT_CLASS = formatClass;
    }

    public static boolean isAvailable() {
        return AVAILABLE;
    }

    public static boolean tryBulkPush(Object writer, org.lwjgl.system.MemoryStack stack,
                                       long pointer, int count, Object format) {
        if (!AVAILABLE || PUSH_METHOD == null) return false;
        try {
            PUSH_METHOD.invoke(writer, stack, pointer, count, format);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private SodiumVertexHelper() {}
}
