package com.ingenium.compat.sodium;

// import com.ingenium.compat.ModDetect;
// import com.ingenium.config.IngeniumYaclScreen;
// import net.caffeinemc.mods.sodium.api.config.ConfigEntryPoint;
// import net.caffeinemc.mods.sodium.api.config.structure.ConfigBuilder;
// import net.caffeinemc.mods.sodium.api.config.structure.OptionGroup;
// import net.caffeinemc.mods.sodium.api.config.structure.OptionPage;
// import net.caffeinemc.mods.sodium.api.config.structure.control.ControlValue;
// import net.caffeinemc.mods.sodium.api.config.structure.control.impl.ButtonControl;
// import net.caffeinemc.mods.sodium.api.config.structure.option.OptionImpl;
// import net.minecraft.client.Minecraft;
// import net.minecraft.network.chat.Component;

public final class IngeniumSodiumConfigEntryPoint /* implements ConfigEntryPoint */ {
    /*
    @Override
    public void registerConfigLate(ConfigBuilder builder) {
        if (!ModDetect.isSodiumLoaded()) return;

        var openIngenium = OptionImpl.createBuilder(Void.class, ControlValue.empty())
            .setName(Component.literal("Ingenium Settings"))
            .setTooltip(Component.literal("Open Ingenium (YACL) settings."))
            .setControl(option -> new ButtonControl(
                option,
                Component.literal("Open"),
                () -> Minecraft.getInstance().setScreen(IngeniumYaclScreen.create(Minecraft.getInstance().screen))
            ))
            .setBinding(
                () -> null,
                v -> {}
            )
            .build();

        var group = OptionGroup.createBuilder()
            .setName(Component.literal("Ingenium"))
            .add(openIngenium)
            .build();

        var page = OptionPage.createBuilder()
            .setName(Component.literal("Ingenium"))
            .add(group)
            .build();

        builder.addPage(page);
    }
    */
}
