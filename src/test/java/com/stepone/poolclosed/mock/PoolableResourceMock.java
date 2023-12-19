package com.stepone.poolclosed.mock;

import com.stepone.poolclosed.interfaces.PoolableResource;
import io.vavr.control.Try;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@RequiredArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class PoolableResourceMock implements PoolableResource<String, String> {
    @EqualsAndHashCode.Include
    final private int id;
    private boolean ded = false;
    private long lastActionTime = System.currentTimeMillis();
    @Getter
    private static final AtomicInteger counter = new AtomicInteger(0);

    @Override
    public void kill() {
        //log.info("im dead now");
        ded = true;
    }

    @Override
    public void close() {
        //log.info("closing");
        kill();
    }

    @Override
    public boolean isClosed() {
        return ded;
    }

    @Override
    public long lastActionTime() {
        return lastActionTime;
    }

    public int id() {
        return id;
    }

    @Override
    public CompletableFuture<Try<String>> execute(String action) {
        var futureStuff = new CompletableFuture<Try<String>>();
        Thread.ofVirtual().start(() -> {
            lastActionTime = System.currentTimeMillis();
            try {
                Thread.sleep(1000);
                counter.incrementAndGet();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            lastActionTime = System.currentTimeMillis();
            var success = Try.success(action);
            futureStuff.complete(success);
        });
        return futureStuff;
    }
}
