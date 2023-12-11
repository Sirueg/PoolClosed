package com.stepone.poolclosed.mock;

import com.stepone.poolclosed.interfaces.PoolableResource;
import com.stepone.poolclosed.interfaces.PoolableResourceFactory;

import java.util.concurrent.atomic.AtomicInteger;

public class PoolFactoryMock implements PoolableResourceFactory<String, String> {
    private final AtomicInteger counter = new AtomicInteger(1);

    @Override
    public PoolableResource<String, String> create() {
        return new PoolableResourceMock(counter.getAndIncrement());
    }
}
