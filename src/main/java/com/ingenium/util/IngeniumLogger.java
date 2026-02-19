package com.ingenium.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class IngeniumLogger {
    private static final Logger LOGGER = LoggerFactory.getLogger("Ingenium");

    public static void info(String message) {
        LOGGER.info(message);
    }

    public static void warn(String message) {
        LOGGER.warn(message);
    }

    public static void error(String message) {
        LOGGER.error(message);
    }

    public static void error(String message, Throwable t) {
        LOGGER.error(message, t);
    }

    public static void debug(String message) {
        LOGGER.debug(message);
    }
}
