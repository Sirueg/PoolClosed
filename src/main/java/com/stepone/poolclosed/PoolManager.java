package com.stepone.poolclosed;

import com.stepone.poolclosed.config.PoolConfig;
import com.stepone.poolclosed.interfaces.PoolableResource;
import com.stepone.poolclosed.interfaces.PoolableResourceFactory;
import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
public class PoolManager<K, T> {
    private final PoolConfig poolConfig;
    private final PoolableResourceFactory<K, T> resourceFactory;
    private final Set<PoolableResource<K, T>> resources = ConcurrentHashMap.newKeySet();
    private final Set<PoolableResource<K, T>> liveResources = ConcurrentHashMap.newKeySet();

    public PoolManager(PoolableResourceFactory<K, T> resourceFactory, PoolConfig poolConfig) {
        this.resourceFactory = resourceFactory;
        this.poolConfig = poolConfig;
        //create initial resources
        for (int i = 0; i < poolConfig.getPoolSize(); i++) {
            liveResources.add(createResource());
        }
    }

    /**
     * if there is no resource in the pool, create one
     * unless resources size is greater than maxPoolSize
     * in this case wait for resource to be free until timeout is reached
     *
     * @return newly created resource
     */
    private PoolableResource<K, T> createResource() {
        var newResource = resourceFactory.create();
        resources.add(newResource);
        log.info("Created new resource {}, size is {}", newResource,resources.size());
        return newResource;
    }

    public CompletableFuture<Try<K>> execute(T action) {
        PoolableResource<K, T> resource = getResource().orElseThrow(() -> new IllegalStateException("No resource available"));
        return futureTrick(resource.execute(action), resource);
    }

    private CompletableFuture<Try<K>> futureTrick(CompletableFuture<Try<K>> future, final PoolableResource<K, T> dataResource) {
        return future.thenApply(result -> {
            liveResources.add(dataResource);
            log.info("Released resource {} {}", dataResource.id(), result);
            this.notifyFromOtherThread();
            return result;
        });
    }

    public synchronized void notifyFromOtherThread() {
        notify();
    }

    public synchronized Optional<PoolableResource<K, T>> getResource() {
        Optional<PoolableResource<K, T>> liveResource = liveResources.stream()
                .filter(r -> !r.isClosed())
                .findFirst();

        if (liveResource.isPresent()) {
            log.info("Resource already free, recycling {}", liveResource.get().id());
            liveResources.remove(liveResource.get());
            return liveResource;
        }

        if (resources.size() < poolConfig.getMaxPoolSize()) {
            return Optional.of(createResource());
        }

        long waitTime = System.currentTimeMillis() + poolConfig.getTimeout();
        while (System.currentTimeMillis() < waitTime) {
            try {
                wait(3000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            log.info("Waited for resource for {}", waitTime - System.currentTimeMillis());
            liveResource = liveResources.stream()
                    .filter(r -> !r.isClosed())
                    .findFirst();
            if (liveResource.isPresent()) {
                log.info("Resource free, recicling {}", liveResource.get().id());
                liveResources.remove(liveResource.get());
                return liveResource;
            }
        }

        return Optional.empty();
    }

    /**
     * Scheduled mainteinance run
     */
    public void scheduledRun() {
        Set<PoolableResource<K, T>> closedResources = resources.stream()
                .filter(PoolableResource::isClosed)
                .collect(Collectors.toSet());
        resources.removeAll(closedResources);
        closedResources.stream()
                .filter(r -> !liveResources.contains(r))
                .forEach(this::removeAndKill);

        Set<PoolableResource<K, T>> expiredResources = liveResources.stream()
                .filter(resource -> resource.lastActionTime() < System.currentTimeMillis())
                .collect(Collectors.toSet());
        liveResources.removeAll(expiredResources);
        expiredResources.stream()
                .filter(r -> !liveResources.contains(r))
                .forEach(this::removeAndKill);
    }

    private void removeAndKill(PoolableResource<K, T> resource) {
        resources.remove(resource);
        liveResources.remove(resource);
        resource.kill();
    }

}
