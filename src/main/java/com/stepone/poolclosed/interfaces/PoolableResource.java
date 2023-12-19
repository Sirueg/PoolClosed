package com.stepone.poolclosed.interfaces;

import io.vavr.control.Try;

import java.io.Closeable;
import java.util.concurrent.CompletableFuture;

public interface PoolableResource<RESPONSE, REQUEST> extends Closeable {
    void kill();
    void close();
    boolean isClosed();
    int id();
    long lastActionTime();
    CompletableFuture<Try<RESPONSE>> execute(REQUEST action);
}
