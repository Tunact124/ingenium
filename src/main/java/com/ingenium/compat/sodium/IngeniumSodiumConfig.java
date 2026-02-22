package com.ingenium.compat.sodium;

import com.ingenium.config.IngeniumConfig;
import me.jellysquid.mods.sodium.client.gui.options.OptionGroup;
import me.jellysquid.mods.sodium.client.gui.options.OptionImpl;
import me.jellysquid.mods.sodium.client.gui.options.OptionPage;
import me.jellysquid.mods.sodium.client.gui.options.control.TickBoxControl;
import me.jellysquid.mods.sodium.client.gui.options.storage.OptionStorage;
import net.minecraft.network.chat.Component;
import toni.sodiumoptionsapi.api.OptionGUIConstruction;
import toni.sodiumoptionsapi.util.IOptionGroupIdAccessor;

import java.util.ArrayList;
import java.util.List;

public final class IngeniumSodiumConfig {

    public static void init() {
        OptionGUIConstruction.EVENT.register((pages) -> {
            List<OptionGroup> groups = new ArrayList<>();
            groups.add(buildGeneralGroup());
            groups.add(buildTweakGroup());
            
            OptionPage page = new OptionPage(Component.literal("Ingenium"), com.google.common.collect.ImmutableList.copyOf(groups));
            ((IOptionGroupIdAccessor) page).sodiumOptionsAPI$setId(net.minecraft.resources.ResourceLocation.of("ingenium:general", ':'));
            pages.add(page);
        });
    }

    private static OptionGroup buildGeneralGroup() {
        return OptionGroup.createBuilder()
                .add(OptionImpl.createBuilder(boolean.class, new ConfigStorage())
                        .setName(Component.literal("Enable Ingenium"))
                        .setTooltip(Component.literal("Master switch for all Ingenium optimizations."))
                        .setControl(TickBoxControl::new)
                        .setBinding((opt, val) -> IngeniumConfig.set(IngeniumConfig.get().toBuilder().enabled(val).build()),
                                    (opt) -> IngeniumConfig.get().enabled())
                        .build())
                .add(OptionImpl.createBuilder(boolean.class, new ConfigStorage())
                        .setName(Component.literal("Timing Wheel"))
                        .setTooltip(Component.literal("High-performance world tick scheduler. Reduces overhead of thousands of pending ticks."))
                        .setControl(TickBoxControl::new)
                        .setBinding((opt, val) -> IngeniumConfig.set(IngeniumConfig.get().toBuilder().timingWheelEnabled(val).build()),
                                    (opt) -> IngeniumConfig.get().timingWheelEnabled())
                        .build())
                .build();
    }

    private static OptionGroup buildTweakGroup() {
        return OptionGroup.createBuilder()
                .add(OptionImpl.createBuilder(boolean.class, new ConfigStorage())
                        .setName(Component.literal("Off-Heap BE Metadata"))
                        .setTooltip(Component.literal("Reduces heap usage and GC pressure by moving block entity metadata to off-heap memory."))
                        .setControl(TickBoxControl::new)
                        .setBinding((opt, val) -> IngeniumConfig.set(IngeniumConfig.get().toBuilder().offHeapBeMetadataEnabled(val).build()),
                                    (opt) -> IngeniumConfig.get().offHeapBeMetadataEnabled())
                        .build())
                .add(OptionImpl.createBuilder(boolean.class, new ConfigStorage())
                        .setName(Component.literal("Throttle Block Entities"))
                        .setTooltip(Component.literal("Slows down ticking of block entities outside the critical radius."))
                        .setControl(TickBoxControl::new)
                        .setBinding((opt, val) -> IngeniumConfig.set(IngeniumConfig.get().toBuilder().throttleBlockEntities(val).build()),
                                    (opt) -> IngeniumConfig.get().throttleBlockEntitiesEnabled())
                        .build())
                .build();
    }

    private static class ConfigStorage implements OptionStorage<Object> {
        @Override
        public Object getData() { return null; }
        @Override
        public void save() { IngeniumConfig.get().save(); }
    }
}
