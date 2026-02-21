package com.ingenium.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class IngeniumSafetySystem {
    private static final Logger LOGGER = LoggerFactory.getLogger("Ingenium/Safety");

    private IngeniumSafetySystem() { }

    public static void reportFailure(String boundary, Throwable throwable) {
        LOGGER.error("Failure at boundary '{}': {}", boundary, throwable.toString(), throwable);
    }
}
