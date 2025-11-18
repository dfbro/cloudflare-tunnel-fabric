package com.mikir.tunneled;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

public class CloudflaredMod implements ModInitializer {

    public static CloudflaredRunner runner;

    @Override
    public void onInitialize() {
        ConfigManager.load(); // Load or create config.yml

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            runner = new CloudflaredRunner(ConfigManager.config);
            runner.start();
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            if (runner != null) runner.stop();
        });
    }
}
