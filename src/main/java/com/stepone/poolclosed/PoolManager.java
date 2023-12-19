package com.stepone.poolclosed;

import com.stepone.poolclosed.config.PoolConfig;
import com.stepone.poolclosed.interfaces.PoolableResource;
import com.stepone.poolclosed.interfaces.PoolableResourceFactory;
import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * PoolManager
 *
 * <p>Manages the pool of resources
 *
 * @param <RESPONSE> Response type
 * @param <REQUEST> Request type
 */
@Slf4j
public final class PoolManager<RESPONSE, REQUEST> {
    private final PoolConfig poolConfig;
    private final PoolableResourceFactory<RESPONSE, REQUEST> resourceFactory;
    private final Set<PoolableResource<RESPONSE, REQUEST>> resources = ConcurrentHashMap.newKeySet();
    private final BlockingQueue<PoolableResource<RESPONSE, REQUEST>> liveResources;

    /**
     * PoolManager constructor with PoolableResourceFactory and PoolConfig
     *
     * @param resourceFactory Factory for creating PoolableResource
     * @param poolConfig      Pool configuration
     */
    public PoolManager(PoolableResourceFactory<RESPONSE, REQUEST> resourceFactory, PoolConfig poolConfig) {
        this.resourceFactory = resourceFactory;
        this.poolConfig = poolConfig;
        this.liveResources = new LinkedBlockingQueue<>();
        //create initial resources
        for (int i = 0; i < poolConfig.getPoolSize(); i++) {
            createResource().ifPresent(liveResources::add);
        }
    }

    /**
     * if there is no resource in the pool, create one
     * unless resources size is greater than maxPoolSize
     * in this case wait for resource to be free until timeout is reached
     *
     * @return newly created resource
     */
    private Optional<PoolableResource<RESPONSE, REQUEST>> createResource() {
        if (resources.size() >= poolConfig.getMaxPoolSize()) {
            return Optional.empty();
        }
        var newResource = resourceFactory.create();
        resources.add(newResource);
        //log.info("Created new resource {}, size is {}", newResource, resources.size());
        return Optional.of(newResource);
    }

    /**
     * Run action on resource and returns CompletableFuture with result
     *
     * @param action Action to run on resource
     * @return CompletableFuture with result
     */
    public CompletableFuture<Try<RESPONSE>> execute(REQUEST action) {
        PoolableResource<RESPONSE, REQUEST> resource = getResource().orElseThrow(() -> new IllegalStateException("No resource available"));
        return futureTrick(resource.execute(action), resource);
    }

    /**
     * Trick to add resource to liveResources after action is completed
     *
     * @param future       CompletableFuture to add trick to
     * @param dataResource Resource to add to liveResources
     * @return CompletableFuture with result
     */
    private CompletableFuture<Try<RESPONSE>> futureTrick(CompletableFuture<Try<RESPONSE>> future, final PoolableResource<RESPONSE, REQUEST> dataResource) {
        return future.thenApply(result -> {
            if (!liveResources.add(dataResource)) {
                log.error("Could not add resource {} to queue, killing resource", dataResource.id());
                dataResource.kill();
                resources.remove(dataResource);
            }
            return result;
        });
    }

    /**
     * Get resource from pool and remove it from liveResources
     *
     * @return Optional with resource if available
     */
    public Optional<PoolableResource<RESPONSE, REQUEST>> getResource() {
        var liveResource = liveResources.poll();
        if (liveResource != null) {
            return Optional.of(liveResource);
        }

        var createdResource = createResource();

        if (createdResource.isPresent()) {
            return createdResource;
        }

        try {
            var resource = liveResources.poll(poolConfig.getTimeout(), TimeUnit.MILLISECONDS);
            if (resource != null) {
                return Optional.of(resource);
            } else {
                log.warn("Resource timed out");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return Optional.empty();
    }

    /**
     * Scheduled mainteinance run
     */
    public void scheduledRun() {
        Set<PoolableResource<RESPONSE, REQUEST>> closedResources = resources.stream()
                .filter(PoolableResource::isClosed)
                .collect(Collectors.toSet());
        closedResources.stream()
                .filter(liveResources::contains)
                .forEach(this::removeAndKill);
        Set<PoolableResource<RESPONSE, REQUEST>> expiredResources = liveResources.stream()
                .filter(resource -> (resource.lastActionTime() + poolConfig.getMaxIdleTime()) < System.currentTimeMillis())
                .collect(Collectors.toSet());
        expiredResources.stream()
                .filter(liveResources::contains)
                .forEach(this::removeAndKill);
        log.info("Current resources {}", resources.size());
        log.info("Current live resources {}", liveResources.size());
    }

    private void removeAndKill(PoolableResource<RESPONSE, REQUEST> resource) {
        if (liveResources.remove(resource)) {
            resources.remove(resource);
            resource.kill();
        }
    }
}
