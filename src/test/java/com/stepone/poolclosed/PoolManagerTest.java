package com.stepone.poolclosed;

import com.stepone.poolclosed.config.PoolConfig;
import com.stepone.poolclosed.mock.PoolFactoryMock;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.util.concurrent.ExecutionException;

@Slf4j
public class PoolManagerTest {

    @Test
    public void test() throws InterruptedException {
        PoolFactoryMock manager = new PoolFactoryMock();
        var config = new PoolConfig();
        config.setMaxPoolSize(100);
        PoolManager<String, String> poolManager = new PoolManager<>(manager, config);

        long currentTime = System.currentTimeMillis();
        for (int i = 0; i < 5; i++) {
            poolManager.execute("Action " + i +" "+ System.currentTimeMillis())
                    .thenAccept(result -> log.info("Last request: {}", result.get() + " " + (System.currentTimeMillis() - currentTime)));
        }
        log.info("Total time: {}", System.currentTimeMillis() - currentTime);
        poolManager.scheduledRun();
        try {
            String result = poolManager.execute("Hola").get().get();
            log.info("Last request: {}", result +" "+(System.currentTimeMillis()-currentTime));
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
        log.info("Total time: {}", System.currentTimeMillis() - currentTime);
    }
}