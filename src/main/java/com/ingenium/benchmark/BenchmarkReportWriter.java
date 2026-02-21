package com.ingenium.benchmark;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class BenchmarkReportWriter {

    private static final DateTimeFormatter TS =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    public static Path writeTxtReport(String reportBody) throws IOException {
        Path dir = FabricLoader.getInstance().getConfigDir()
                .resolve("ingenium")
                .resolve("benchmarks");

        Files.createDirectories(dir);

        String fileName = "ingenium-benchmark-" + TS.format(LocalDateTime.now()) + ".txt";
        Path out = dir.resolve(fileName);

        Files.writeString(out, reportBody, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.WRITE);

        return out;
    }
}
