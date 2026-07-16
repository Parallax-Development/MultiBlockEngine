package dev.darkblade.mbe.api.event;

import dev.darkblade.mbe.api.service.MBEService;
import java.util.function.Consumer;

/**
 * Service responsible for decoupled pub-sub event handling in MBE.
 * This completely avoids dependency on org.bukkit.event.Event.
 */
public interface EventBusService extends MBEService {

    /**
     * Publishes an event to all registered listeners.
     * @param event the event to publish
     */
    void publish(MBEEvent event);

    /**
     * Subscribes a listener to a specific event class.
     * @param eventClass the event class to listen for
     * @param listener the action to execute when the event is published
     * @param <T> the event type
     */
    <T extends MBEEvent> void subscribe(Class<T> eventClass, Consumer<T> listener);
    
    /**
     * Unsubscribes a previously registered listener.
     * @param eventClass the event class
     * @param listener the action to remove
     * @param <T> the event type
     */
    <T extends MBEEvent> void unsubscribe(Class<T> eventClass, Consumer<T> listener);
}
