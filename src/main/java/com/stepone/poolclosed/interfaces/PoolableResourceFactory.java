package com.stepone.poolclosed.interfaces;

public interface PoolableResourceFactory<K,T> {

    PoolableResource<K,T> create();
}
