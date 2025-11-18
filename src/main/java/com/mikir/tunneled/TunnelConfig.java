package com.mikir.tunneled;

public record TunnelConfig(
    boolean installed,
    String token,
    String version,
    String downloadURL,
    boolean autoRestart,
    int restartInterval,
    String logLevel,
    boolean disableTLS
) {}
