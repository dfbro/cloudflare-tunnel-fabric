package com.mikir.tunneled;

import org.yaml.snakeyaml.Yaml;
import java.util.Map;
import java.nio.file.Files;
import java.nio.file.Path;

public class YamlUtil {

    public static TunnelConfig parse(Path path) {
        try {
            String text = Files.readString(path);
            Map<String, Object> map = new Yaml().load(text);

            boolean installed = (boolean) map.get("installed");
            String token = (String) map.get("token");

            Map<String, Object> dl = (Map<String, Object>) map.get("download");
            String version = (String) dl.get("version");
            String url = (String) dl.get("url");

            Map<String, Object> restart = (Map<String, Object>) map.get("restart");
            boolean autoRestart = (boolean) restart.get("auto-restart");
            int interval = (int) restart.get("interval-seconds");

            String logLevel = (String) map.get("log-level");
            boolean disableTLS = (boolean) map.get("disable-tls");

            return new TunnelConfig(
                installed, token, version, url, autoRestart, interval, logLevel, disableTLS
            );

        } catch (Exception e) {
            throw new RuntimeException("Invalid config.yml", e);
        }
    }
}
