package dev.darkblade.mbe.core.graph.dependency;

import java.util.Collection;

public interface DependencyNode<T> {
    String id();
    T payload();
    Collection<DependencyEdge> edges();
}
