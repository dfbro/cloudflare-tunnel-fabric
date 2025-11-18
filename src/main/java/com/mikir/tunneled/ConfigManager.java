package com.mikir.tunneled;

import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.*;
import java.io.IOException;

public class ConfigManager {

    public static TunnelConfig config;

    public static void load() {
        Path path = FabricLoader.getInstance().getConfigDir()
                .resolve("cloudflared/config.yml");

        try {
            if (!Files.exists(path)) {
                Files.createDirectories(path.getParent());
                Files.writeString(path, DEFAULT_CONFIG);
            }

            config = YamlUtil.parse(path);

        } catch (IOException e) {
            throw new RuntimeException("Failed to load config.yml", e);
        }
    }

    private static final String DEFAULT_CONFIG = """
        installed: false
        token: ""
        download:
          version: "2025.1.0"
          url: "https://github.com/cloudflare/cloudflared/releases/download/VERSION/cloudflared-linux-amd64"
        restart:
          auto-restart: true
          interval-seconds: 5
        log-level: "info"
        disable-tls: false
        """;
}
