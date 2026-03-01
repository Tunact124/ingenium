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

        private static volatile boolean initialized;

        public static void init() {
                // Idempotency guard — prevents double-rendering if called more than once
                if (initialized)
                        return;
                initialized = true;

                OptionGUIConstruction.EVENT.register((pages) -> {
                        List<OptionGroup> groups = new ArrayList<>();
                        groups.add(buildGeneralGroup());
                        groups.add(buildTweakGroup());
                        groups.add(buildMemoryGroup());

                        OptionPage page = new OptionPage(Component.literal("Ingenium"),
                                        com.google.common.collect.ImmutableList.copyOf(groups));
                        ((IOptionGroupIdAccessor) page)
                                        .sodiumOptionsAPI$setId(net.minecraft.resources.ResourceLocation
                                                        .of("ingenium:general", ':'));
                        pages.add(page);
                });
        }

        private static OptionGroup buildGeneralGroup() {
                return OptionGroup.createBuilder()
                                .add(OptionImpl.createBuilder(boolean.class, new ConfigStorage())
                                                .setName(Component.literal("Enable Ingenium"))
                                                .setTooltip(Component.literal(
                                                                "Master switch for all Ingenium optimizations."))
                                                .setControl(TickBoxControl::new)
                                                .setBinding(
                                                                (opt, val) -> IngeniumConfig.set(IngeniumConfig.get()
                                                                                .toBuilder().enabled(val).build()),
                                                                (opt) -> IngeniumConfig.get().enabled())
                                                .build())
                                .add(OptionImpl.createBuilder(boolean.class, new ConfigStorage())
                                                .setName(Component.literal("Enable Diagnostics"))
                                                .setTooltip(Component.literal(
                                                                "Enables continuous performance diagnostics and metric gathering."))
                                                .setControl(TickBoxControl::new)
                                                .setBinding(
                                                                (opt, val) -> IngeniumConfig.set(IngeniumConfig.get()
                                                                                .toBuilder().diagnosticsEnabled(val)
                                                                                .build()),
                                                                (opt) -> IngeniumConfig.get().diagnosticsEnabled())
                                                .build())
                                .add(OptionImpl.createBuilder(boolean.class, new ConfigStorage())
                                                .setName(Component.literal("Spark Integration"))
                                                .setTooltip(Component.literal(
                                                                "Enables memory-efficient profiling through the Spark mod."))
                                                .setControl(TickBoxControl::new)
                                                .setBinding(
                                                                (opt, val) -> IngeniumConfig.set(IngeniumConfig.get()
                                                                                .toBuilder()
                                                                                .sparkIntegrationEnabled(val).build()),
                                                                (opt) -> IngeniumConfig.get().sparkIntegrationEnabled())
                                                .build())
                                .build();
        }

        private static OptionGroup buildTweakGroup() {
                return OptionGroup.createBuilder()
                                .add(OptionImpl.createBuilder(boolean.class, new ConfigStorage())
                                                .setName(Component.literal("Timing Wheel"))
                                                .setTooltip(Component.literal(
                                                                "High-performance world tick scheduler. Reduces overhead of thousands of pending ticks."))
                                                .setControl(TickBoxControl::new)
                                                .setBinding(
                                                                (opt, val) -> IngeniumConfig
                                                                                .set(IngeniumConfig.get().toBuilder()
                                                                                                .timingWheelEnabled(val)
                                                                                                .build()),
                                                                (opt) -> IngeniumConfig.get().timingWheelEnabled())
                                                .build())
                                .add(OptionImpl.createBuilder(boolean.class, new ConfigStorage())
                                                .setName(Component.literal("Throttle Block Entities"))
                                                .setTooltip(
                                                                Component.literal(
                                                                                "Slows down ticking of block entities outside the critical radius."))
                                                .setControl(TickBoxControl::new)
                                                .setBinding(
                                                                (opt, val) -> IngeniumConfig
                                                                                .set(IngeniumConfig.get().toBuilder()
                                                                                                .throttleBlockEntities(
                                                                                                                val)
                                                                                                .build()),
                                                                (opt) -> IngeniumConfig.get()
                                                                                .throttleBlockEntitiesEnabled())
                                                .build())
                                .add(OptionImpl.createBuilder(boolean.class, new ConfigStorage())
                                                .setName(Component.literal("Hopper Optimization"))
                                                .setTooltip(Component.literal(
                                                                "Greatly accelerates hopper transfers using cached inventory lookups."))
                                                .setControl(TickBoxControl::new)
                                                .setBinding(
                                                                (opt, val) -> IngeniumConfig.set(IngeniumConfig.get()
                                                                                .toBuilder()
                                                                                .hopperOptimizationEnabled(val)
                                                                                .build()),
                                                                (opt) -> IngeniumConfig.get().hopperOptimizationEnabled)
                                                .build())
                                .add(OptionImpl.createBuilder(boolean.class, new ConfigStorage())
                                                .setName(Component.literal("Redstone Optimization"))
                                                .setTooltip(Component.literal(
                                                                "Improves redstone wire updates and prevents extreme block update cascades."))
                                                .setControl(TickBoxControl::new)
                                                .setBinding(
                                                                (opt, val) -> IngeniumConfig.set(IngeniumConfig.get()
                                                                                .toBuilder()
                                                                                .redstoneOptimizationEnabled(val)
                                                                                .build()),
                                                                (opt) -> IngeniumConfig
                                                                                .get().redstoneOptimizationEnabled)
                                                .build())
                                .add(OptionImpl.createBuilder(boolean.class, new ConfigStorage())
                                                .setName(Component.literal("XP Orb Coalescing"))
                                                .setTooltip(Component.literal(
                                                                "Merges nearby XP orbs rapidly to reduce entity ticking overhead."))
                                                .setControl(TickBoxControl::new)
                                                .setBinding(
                                                                (opt, val) -> IngeniumConfig.set(IngeniumConfig.get()
                                                                                .toBuilder().xpOrbCoalescingEnabled(val)
                                                                                .build()),
                                                                (opt) -> IngeniumConfig.get().xpOrbCoalescingEnabled)
                                                .build())
                                .add(OptionImpl.createBuilder(boolean.class, new ConfigStorage())
                                                .setName(Component.literal("POI Spatial Hashing"))
                                                .setTooltip(Component.literal(
                                                                "Accelerates villager and mob Point of Interest searches and queries."))
                                                .setControl(TickBoxControl::new)
                                                .setBinding(
                                                                (opt, val) -> IngeniumConfig.set(IngeniumConfig.get()
                                                                                .toBuilder()
                                                                                .poiSpatialHashingEnabled(val).build()),
                                                                (opt) -> IngeniumConfig.get().poiSpatialHashingEnabled)
                                                .build())
                                .build();
        }

        private static OptionGroup buildMemoryGroup() {
                return OptionGroup.createBuilder()
                                .add(OptionImpl.createBuilder(boolean.class, new ConfigStorage())
                                                .setName(Component.literal("Off-Heap BE Metadata"))
                                                .setTooltip(Component.literal(
                                                                "Reduces heap usage and GC pressure by moving block entity metadata to off-heap memory."))
                                                .setControl(TickBoxControl::new)
                                                .setBinding(
                                                                (opt, val) -> IngeniumConfig
                                                                                .set(IngeniumConfig.get().toBuilder()
                                                                                                .offHeapBeMetadataEnabled(
                                                                                                                val)
                                                                                                .build()),
                                                                (opt) -> IngeniumConfig.get()
                                                                                .offHeapBeMetadataEnabled())
                                                .build())
                                .add(OptionImpl.createBuilder(boolean.class, new ConfigStorage())
                                                .setName(Component.literal("GC Coordination"))
                                                .setTooltip(Component.literal(
                                                                "Coordinates garbage collection cycles intelligently outside of hot paths."))
                                                .setControl(TickBoxControl::new)
                                                .setBinding(
                                                                (opt, val) -> IngeniumConfig.set(IngeniumConfig.get()
                                                                                .toBuilder().enableGcCoordination(val)
                                                                                .build()),
                                                                (opt) -> IngeniumConfig.get().enableGcCoordination)
                                                .build())
                                .build();
        }

        private static class ConfigStorage implements OptionStorage<Object> {
                @Override
                public Object getData() {
                        return null;
                }

                @Override
                public void save() {
                        IngeniumConfig.get().save();
                }
        }
}
