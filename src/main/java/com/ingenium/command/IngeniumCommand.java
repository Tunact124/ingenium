package com.ingenium.command;

import com.ingenium.compat.CompatibilityBridge;
import com.ingenium.config.IngeniumConfig;
import com.ingenium.governor.IngeniumGovernor;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class IngeniumCommand {
 
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("ingenium")
            .requires(src -> src.hasPermissionLevel(2))
 
            .then(CommandManager.literal("compat")
                .executes(ctx -> {
                    ServerCommandSource src = ctx.getSource();
                    src.sendFeedback(() -> Text.literal("=== Ingenium Compat Status ===")
                         .styled(s -> s.withColor(Formatting.GOLD)), false);
 
                    printMod(src, "Sodium",      CompatibilityBridge.HAS_SODIUM);
                    printMod(src, "Lithium",     CompatibilityBridge.HAS_LITHIUM);
                    printMod(src, "FerriteCore", CompatibilityBridge.HAS_FERRITECORE);
                    printMod(src, "Starlight",   CompatibilityBridge.HAS_STARLIGHT);
                    printMod(src, "Bobby",       CompatibilityBridge.HAS_BOBBY);
 
                    src.sendFeedback(() -> Text.literal("Active Profile: ")
                        .append(Text.literal(IngeniumGovernor.current().name())
                            .styled(s -> s.withColor(Formatting.AQUA))), false);
 
                    return Command.SINGLE_SUCCESS;
                }))
 
            .then(CommandManager.literal("profile")
                .executes(ctx -> {
                    ctx.getSource().sendFeedback(() -> Text.literal(
                        "Profile: " + IngeniumGovernor.current()), false);
                    return Command.SINGLE_SUCCESS;
                }))
 
            .then(CommandManager.literal("reload")
                .executes(ctx -> {
                    IngeniumConfig.HANDLER.load();
                    ctx.getSource().sendFeedback(() ->
                        Text.literal("Config reloaded."), true);
                    return Command.SINGLE_SUCCESS;
                }))
            
            .then(IngeniumBenchmarkCommand.getSubcommand())
        );
    }
 
    private static void printMod(ServerCommandSource src, String name, boolean present) {
        Formatting color = present ? Formatting.GREEN : Formatting.GRAY;
        String status    = present ? "✔ Present" : "✘ Absent";
        src.sendFeedback(() -> Text.literal("  " + name + ": ")
            .append(Text.literal(status).styled(s -> s.withColor(color))), false);
    }
}
