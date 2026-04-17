package dev.darkblade.mbe.api.compat;

public interface SchedulerCompatService {
    void runSync(Runnable task);

    void runAsync(Runnable task);
}
