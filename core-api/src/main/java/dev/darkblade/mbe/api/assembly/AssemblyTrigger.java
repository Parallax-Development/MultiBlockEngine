package dev.darkblade.mbe.api.assembly;

public interface AssemblyTrigger {

    String id();

    boolean shouldTrigger(AssemblyContext context);
}

