package com.ingenium.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.ServerCommandSource;

import static net.minecraft.server.command.CommandManager.literal;

public final class IngeniumCommands {
    private IngeniumCommands() {
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
            literal("ingenium")
                .requires(src -> true)
                .then(literal("ping").executes(ctx -> {
                    ctx.getSource().sendFeedback(() -> net.minecraft.text.Text.literal("Ingenium: pong"), false);
                    return 1;
                }))
        );
    }
}
