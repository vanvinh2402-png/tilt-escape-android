package com.vanvinh2402.tiltescape.input;

public interface SensorRegistration {
    boolean isAvailable();
    void register();
    void unregister();
}
