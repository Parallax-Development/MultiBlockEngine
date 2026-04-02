package dev.darkblade.mbe.api.wiring.debug;

public interface NetworkInspectView {

    void putMetric(String key, Object value);

    void addLine(String line);
}

