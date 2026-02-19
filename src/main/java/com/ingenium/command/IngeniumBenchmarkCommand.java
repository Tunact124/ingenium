package com.ingenium.command;

import com.ingenium.benchmark.SimpleBenchmark;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class IngeniumBenchmarkCommand {

    public static com.mojang.brigadier.builder.LiteralArgumentBuilder<ServerCommandSource> getSubcommand() {
        return CommandManager.literal("benchmark")
                .requires(source -> source.hasPermissionLevel(2))
                .executes(context -> {
                    // Default behavior: Start 30s per phase benchmark
                    new SimpleBenchmark().run(context.getSource(), 30);
                    return 1;
                })
                .then(CommandManager.literal("start")
                    .executes(context -> {
                        new SimpleBenchmark().run(context.getSource(), 30);
                        return 1;
                    })
                    .then(CommandManager.argument("seconds", IntegerArgumentType.integer(5, 300))
                        .executes(context -> {
                            int seconds = IntegerArgumentType.getInteger(context, "seconds");
                            new SimpleBenchmark().run(context.getSource(), seconds);
                            return 1;
                        })))
                .then(CommandManager.literal("quick")
                    .executes(context -> {
                        // Quick 10s per phase test
                        new SimpleBenchmark().run(context.getSource(), 10);
                        return 1;
                    }))
                .then(CommandManager.literal("help")
                    .executes(context -> {
                        context.getSource().sendMessage(Text.literal("=== Ingenium Benchmark Help ===").formatted(Formatting.GOLD));
                        context.getSource().sendMessage(Text.literal("/ingenium benchmark - Runs full stress test (30s phases)"));
                        context.getSource().sendMessage(Text.literal("/ingenium benchmark start <seconds> - Custom duration"));
                        context.getSource().sendMessage(Text.literal("/ingenium benchmark quick - Runs 10s per phase test"));
                        context.getSource().sendMessage(Text.literal("Reports are saved to ingenium-benchmarks/").formatted(Formatting.GRAY));
                        return 1;
                    }));
    }
}
