package com.ingenium.command;

import com.ingenium.benchmark.IngeniumBenchmarkService;
import com.ingenium.config.IngeniumConfig;
import com.ingenium.core.IngeniumGovernor;
import com.ingenium.core.IngeniumGovernor.SubsystemType;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import static net.minecraft.commands.Commands.literal;

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
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(literal("ingenium")
                .requires(src -> src.hasPermission(2))
                .then(literal("status").executes(IngeniumCommand::status))
                .then(literal("bench").executes(IngeniumCommand::bench))
                .then(literal("ping").executes(ctx -> {
                    ctx.getSource().sendSuccess(() -> Component.literal("Ingenium: pong"), false);
                    return 1;
                }))
        );
    }

    private static int bench(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer(); // null if console
        IngeniumBenchmarkService.get().run(player);
        return 1;
    }

    private static int status(CommandContext<CommandSourceStack> ctx) {
        final var gov = IngeniumGovernor.get();
        final var src = ctx.getSource();

        src.sendSystemMessage(Component.literal("=== Ingenium Status ===").withStyle(ChatFormatting.GOLD));
        src.sendSystemMessage(Component.literal("Master enabled: " + IngeniumConfig.get().masterEnabled).withStyle(ChatFormatting.GRAY));
        src.sendSystemMessage(Component.literal("Profile: " + gov.getCurrentProfile()).withStyle(ChatFormatting.GRAY));
        src.sendSystemMessage(Component.literal("MSPT: " + gov.getCurrentMspt() + " ms").withStyle(ChatFormatting.GRAY));
        src.sendSystemMessage(Component.literal("Bypass: " + gov.isBypassed()).withStyle(ChatFormatting.GRAY));

        src.sendSystemMessage(Component.literal("Subsystem budgets (remaining ns):").withStyle(ChatFormatting.AQUA));
        for (SubsystemType t : SubsystemType.values()) {
            long remaining = gov.getRemainingBudgetNs(t);
            src.sendSystemMessage(Component.literal("  " + t.name() + ": " + remaining + " ns").withStyle(ChatFormatting.GRAY));
        }

        src.sendSystemMessage(Component.literal("Subsystem time share (accumulated ns):").withStyle(ChatFormatting.AQUA));
        for (SubsystemType t : SubsystemType.values()) {
            long ns = gov.getSubsystemTimeNs(t);
            src.sendSystemMessage(Component.literal("  " + t.name() + ": " + ns + " ns").withStyle(ChatFormatting.DARK_GRAY));
        }

        return 1;
    }
}
