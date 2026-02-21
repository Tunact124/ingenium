package com.ingenium.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.ingenium.core.Ingenium;
import com.ingenium.core.IngeniumSafetySystem;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigIO {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve(Ingenium.MOD_ID + ".json");

    private ConfigIO() {}

    public static void loadOrCreate() {
        if (!Files.exists(PATH)) {
            save();
            return;
        }

        try {
            var json = Files.readString(PATH, StandardCharsets.UTF_8);
            var loaded = GSON.fromJson(json, IngeniumConfig.class);
            if (loaded == null) {
                Ingenium.LOGGER.warn("Config file parsed to null; regenerating defaults.");
                save();
                return;
            }
            IngeniumConfig.set(loaded);
        } catch (JsonSyntaxException | IOException exception) {
            IngeniumSafetySystem.reportFailure("ConfigIO.loadOrCreate", exception);
        }
    }

    public static void save() {
        try {
            Files.createDirectories(PATH.getParent());
            var json = GSON.toJson(IngeniumConfig.get());
            Files.writeString(PATH, json, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            IngeniumSafetySystem.reportFailure("ConfigIO.save", exception);
        }
    }
}
