package com.stepone.poolclosed.mock;

import com.stepone.poolclosed.interfaces.PoolableResource;
import io.vavr.control.Try;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

@Slf4j
@RequiredArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class PoolableResourceMock implements PoolableResource<String, String> {
    @EqualsAndHashCode.Include
    final private int id;
    private boolean ded =false;
    private boolean inUse = false;
    private long lastActionTime = System.currentTimeMillis();

    @Override
    public void kill() {
        log.info("im dead now");
        ded = true;
    }

    @Override
    public void close() {
        log.info("closing");
        kill();
    }

    @Override
    public boolean isClosed() {
        return ded;
    }

    @Override
    public boolean inUse() {
        return inUse;
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
        return CompletableFuture.supplyAsync(() -> {
            inUse = true;
            lastActionTime = System.currentTimeMillis();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            inUse = false;
            lastActionTime = System.currentTimeMillis();
            return Try.success(action);
        });
    }
}
