package com.stepone.poolclosed.interfaces;

import io.vavr.control.Try;

import java.io.Closeable;
import java.util.concurrent.CompletableFuture;

public interface PoolableResource<K,T> extends Closeable {
    void kill();
    void close();
    boolean isClosed();
    boolean inUse();
    int id();
    long lastActionTime();
    CompletableFuture<Try<K>> execute(T action);
}
