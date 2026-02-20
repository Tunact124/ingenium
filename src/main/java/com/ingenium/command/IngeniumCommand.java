package com.ingenium.command;

import com.ingenium.benchmark.IngeniumBenchmarkService;
import com.ingenium.config.IngeniumConfig;
import com.ingenium.core.IngeniumGovernor;
import com.ingenium.core.IngeniumGovernor.SubsystemType;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import static net.minecraft.server.command.CommandManager.literal;

/**
 * Registers /ingenium commands.
 *
 * <p>Permission level: 2 (operator).
 *
 * <p>Commands:
 * <ul>
 *   <li>/ingenium status</li>
 *   <li>/ingenium bench</li>
 * </ul>
 */
public final class IngeniumCommand {
    private IngeniumCommand() {}

    /**
     * Registers the command root.
     *
     * @param dispatcher brigadier dispatcher
     */
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("ingenium")
                .requires(src -> src.hasPermissionLevel(2))
                .then(literal("status").executes(IngeniumCommand::status))
                .then(literal("bench").executes(IngeniumCommand::bench))
                .then(literal("ping").executes(ctx -> {
                    ctx.getSource().sendFeedback(() -> Text.literal("Ingenium: pong"), false);
                    return 1;
                }))
        );
    }

    private static int bench(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer(); // null if console
        IngeniumBenchmarkService.getInstance().startBenchmark(player);
        return 1;
    }

    private static int status(CommandContext<ServerCommandSource> ctx) {
        final var gov = IngeniumGovernor.getInstance();
        final var src = ctx.getSource();

        src.sendMessage(Text.literal("=== Ingenium Status ===").formatted(Formatting.GOLD));
        src.sendMessage(Text.literal("Master enabled: " + IngeniumConfig.get().masterEnabled).formatted(Formatting.GRAY));
        src.sendMessage(Text.literal("Profile: " + gov.getCurrentProfile()).formatted(Formatting.GRAY));
        src.sendMessage(Text.literal("MSPT: " + gov.getCurrentMspt() + " ms").formatted(Formatting.GRAY));
        src.sendMessage(Text.literal("Bypass: " + gov.isBypassed()).formatted(Formatting.GRAY));

        src.sendMessage(Text.literal("Subsystem budgets (remaining ns):").formatted(Formatting.AQUA));
        for (SubsystemType t : SubsystemType.values()) {
            // Architect: Verify these methods exist in your Governor implementation.
            long remaining = gov.getRemainingBudgetNs(t);
            src.sendMessage(Text.literal("  " + t.name() + ": " + remaining + " ns").formatted(Formatting.GRAY));
        }

        src.sendMessage(Text.literal("Subsystem time share (accumulated ns):").formatted(Formatting.AQUA));
        for (SubsystemType t : SubsystemType.values()) {
            long ns = gov.getSubsystemTimeNs(t);
            src.sendMessage(Text.literal("  " + t.name() + ": " + ns + " ns").formatted(Formatting.DARK_GRAY));
        }

        return 1;
    }
}
