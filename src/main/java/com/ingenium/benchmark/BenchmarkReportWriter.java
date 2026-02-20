package com.ingenium.benchmark;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class BenchmarkReportWriter {
    private BenchmarkReportWriter() {
    }

    public static Path writeReport(String fileName, String report) throws IOException {
        Path dir = FabricLoader.getInstance().getConfigDir().resolve("ingenium").resolve("benchmark");
        Files.createDirectories(dir);
        Path out = dir.resolve(fileName);
        Files.writeString(out, report, StandardCharsets.UTF_8);
        return out;
    }
}
