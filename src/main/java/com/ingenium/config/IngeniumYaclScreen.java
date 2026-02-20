package com.ingenium.config;

import com.ingenium.IngeniumMod;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public final class IngeniumYaclScreen {
    private IngeniumYaclScreen() {}

    public static Screen create(Screen parent) {
        IngeniumConfig config = IngeniumConfig.get();
        return YetAnotherConfigLib.createBuilder()
            .title(Text.literal("Ingenium"))
            .category(ConfigCategory.createBuilder()
                .name(Text.literal("General"))
                .option(Option.<Boolean>createBuilder()
                    .name(Text.literal("Master Enable"))
                    .binding(
                        true,
                        () -> config.masterEnabled,
                        v -> config.masterEnabled = v
                    )
                    .controller(BooleanControllerBuilder::create)
                    .build())
                .build())
            .save(() -> {
                config.save();
                IngeniumMod.LOG.info("[Ingenium] Config saved");
            })
            .build()
            .generateScreen(parent);
    }
}
