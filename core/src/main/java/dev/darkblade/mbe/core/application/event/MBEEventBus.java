package dev.darkblade.mbe.core.application.event;

import dev.darkblade.mbe.api.event.EventBusService;
import dev.darkblade.mbe.api.event.MBEEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MBEEventBus implements EventBusService {

    private static final String SERVICE_ID = "mbe:event_bus";
    private final Map<Class<? extends MBEEvent>, List<Consumer<? extends MBEEvent>>> listeners = new ConcurrentHashMap<>();
    private final Logger logger = Logger.getLogger("MBEEventBus");

    @Override
    public String getServiceId() {
        return SERVICE_ID;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void publish(MBEEvent event) {
        List<Consumer<? extends MBEEvent>> eventListeners = listeners.get(event.getClass());
        if (eventListeners != null) {
            for (Consumer<? extends MBEEvent> listener : eventListeners) {
                try {
                    ((Consumer<MBEEvent>) listener).accept(event);
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Error dispatching event " + event.getClass().getSimpleName(), e);
                }
            }
        }
    }

    @Override
    public <T extends MBEEvent> void subscribe(Class<T> eventClass, Consumer<T> listener) {
        listeners.computeIfAbsent(eventClass, k -> new ArrayList<>()).add(listener);
    }

    @Override
    public <T extends MBEEvent> void unsubscribe(Class<T> eventClass, Consumer<T> listener) {
        List<Consumer<? extends MBEEvent>> eventListeners = listeners.get(eventClass);
        if (eventListeners != null) {
            eventListeners.remove(listener);
        }
    }
}
