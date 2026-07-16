package dev.darkblade.mbe.api.event;

/**
 * Base interface for all MBE domain events.
 */
public interface MBEEvent {
    
    /**
     * Optional method for cancellable events. 
     * Not all events are cancellable.
     * @return true if cancelled
     */
    default boolean isCancelled() {
        return false;
    }
    
    /**
     * Set the cancellation state.
     * @param cancel the state
     */
    default void setCancelled(boolean cancel) {
        // No-op by default
    }
}
