package com.mikir.tunneled;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.nio.channels.*;
import java.util.concurrent.*;

public class CloudflaredRunner {

    private final TunnelConfig config;
    private Process process;
    private volatile boolean running = false;

    public CloudflaredRunner(TunnelConfig config) {
        this.config = config;
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

            if (!running || !config.autoRestart()) return;

            Executors.newSingleThreadScheduledExecutor().schedule(
                this::spawnLoop,
                config.restartInterval(),
                TimeUnit.SECONDS
            );
        });
    }

    private Process startCloudflared() {
        File binary = downloadBinaryIfNeeded();

        try {
            ProcessBuilder builder = new ProcessBuilder(
                binary.getPath(),
                "tunnel",
                "--loglevel", config.logLevel(),
                "--logfile", "cloudflared.log",
                "--no-autoupdate",
                "run",
                "--token", config.token()
            );

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
        File dir = new File("cloudflared-cache");
        dir.mkdirs();

        File bin = new File(dir, "cloudflared-" + config.version());

        if (bin.exists()) return bin;

        System.out.println("[Tunneled] Downloading cloudflared...");

        try {
            URL url = URI.create(
                config.downloadURL().replace("VERSION", config.version())
            ).toURL();

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
        if (process != null) process.destroy();
    }
}
