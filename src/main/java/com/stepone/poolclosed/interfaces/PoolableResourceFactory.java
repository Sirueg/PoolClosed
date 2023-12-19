package com.stepone.poolclosed.interfaces;

public interface PoolableResourceFactory<RESPONSE, REQUEST> {

    PoolableResource<RESPONSE, REQUEST> create();
}
