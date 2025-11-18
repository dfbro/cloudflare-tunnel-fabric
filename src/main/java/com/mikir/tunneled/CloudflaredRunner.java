package com.mikir.tunneled;

import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.nio.channels.*;
import java.nio.file.*;
import java.util.concurrent.*;

public class CloudflaredRunner {

    private final TunnelConfig config;
    private Process process;
    private volatile boolean running = false;

    private final Path configDir;
    private final Path logFile;

    public CloudflaredRunner(TunnelConfig config) {
        this.config = config;

        // Create config/tunneled directory
        this.configDir = FabricLoader.getInstance()
                .getConfigDir()
                .resolve("tunneled");

        try {
            Files.createDirectories(configDir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create config/tunneled directory", e);
        }

        // log file path inside config folder
        this.logFile = configDir.resolve("cloudflared.log");
    }

    public void start() {
        if (!config.installed()) {
            System.out.println("[Tunneled] Not installed in config.yml — skipping.");
            return;
        }

        running = true;
        spawnLoop();
    }

    private void spawnLoop() {
        process = startCloudflared();

        process.onExit().thenRun(() -> {
            System.out.println("[Tunneled] cloudflared exited with code " + process.exitValue());

            if (!running || !config.autoRestart())
                return;

            Executors.newSingleThreadScheduledExecutor().schedule(
                    this::spawnLoop,
                    config.restartInterval(),
                    TimeUnit.SECONDS);
        });
    }

    private Process startCloudflared() {
        File binary = downloadBinaryIfNeeded();

        try {
            ProcessBuilder builder = new ProcessBuilder(
                    binary.getPath(),
                    "tunnel",
                    "--loglevel", config.logLevel(),
                    "--logfile", logFile.toAbsolutePath().toString(), // <— NOW INSIDE CONFIG FOLDER
                    "--no-autoupdate",
                    "run",
                    "--token", config.token());

            if (config.disableTLS()) {
                builder.command().add("--no-tls-verify");
            }

            builder.redirectErrorStream(true);

            return builder.start();

        } catch (IOException e) {
            throw new RuntimeException("Failed to launch cloudflared", e);
        }
    }

    private File downloadBinaryIfNeeded() {
        // Store cloudflared binary also inside config folder
        File dir = configDir.resolve("bin").toFile();
        dir.mkdirs();

        File bin = new File(dir, "cloudflared-" + config.version());

        if (bin.exists())
            return bin;

        System.out.println("[Tunneled] Downloading cloudflared " + config.version() + "...");

        try {
            URL url = URI.create(
                    config.downloadURL().replace("VERSION", config.version())).toURL();

            try (ReadableByteChannel in = Channels.newChannel(url.openStream());
                    FileOutputStream out = new FileOutputStream(bin)) {

                out.getChannel().transferFrom(in, 0, Long.MAX_VALUE);
            }

            bin.setExecutable(true);
            return bin;

        } catch (IOException e) {
            throw new RuntimeException("cloudflared download failed", e);
        }
    }

    public void stop() {
        running = false;
        if (process != null)
            process.destroy();
    }
}
