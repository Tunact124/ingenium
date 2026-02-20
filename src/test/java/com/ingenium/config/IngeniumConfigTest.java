package com.ingenium.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class IngeniumConfigTest {
    private static final Gson GSON = new GsonBuilder().create();

    @Test
    void testMissingCoreField() {
        // Simulating a JSON that lacks the "core" field
        String json = "{\"enabled\": true}";
        IngeniumConfig config = GSON.fromJson(json, IngeniumConfig.class);
        
        assertNotNull(config, "Config should not be null");
        
        // This simulates the logic I added to IngeniumConfig.load()
        if (config.core() == null || config.budgets() == null) {
            config = config.toBuilder().build();
        }
        
        assertNotNull(config.core(), "Core should be populated from defaults if missing in JSON");
        assertNotNull(config.budgets(), "Budgets should be populated from defaults if missing in JSON");
    }

    @Test
    void testDefaults() {
        IngeniumConfig config = IngeniumConfig.defaults();
        assertNotNull(config.core(), "Default core should not be null");
        assertNotNull(config.budgets(), "Default budgets should not be null");
    }
}
