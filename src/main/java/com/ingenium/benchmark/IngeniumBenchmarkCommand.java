package com.ingenium.benchmark;

import com.ingenium.core.Ingenium;
import com.ingenium.core.IngeniumSafetySystem;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;

public final class IngeniumBenchmarkCommand {
    private IngeniumBenchmarkCommand() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(Commands.literal("ingenium")
                    .then(Commands.literal("benchmark")
                            .requires(source -> source.hasPermission(2))
                            .executes(context -> {
                                try {
                                    var player = context.getSource().getPlayerOrException();
                                    IngeniumBenchmarkService.get().run(player);
                                    return 1;
                                } catch (Throwable throwable) {
                                    IngeniumSafetySystem.reportFailure("IngeniumBenchmarkCommand", throwable);
                                    return 0;
                                }
                            })));
        });

        Ingenium.LOGGER.info("Registered /ingenium benchmark");
    }
}
