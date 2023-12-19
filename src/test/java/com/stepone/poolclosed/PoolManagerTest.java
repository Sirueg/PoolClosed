package com.stepone.poolclosed;

import com.stepone.poolclosed.config.PoolConfig;
import com.stepone.poolclosed.mock.PoolFactoryMock;
import com.stepone.poolclosed.mock.PoolableResourceMock;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.util.concurrent.ExecutionException;

@Slf4j
public class PoolManagerTest {

    @Test
    public void test() throws InterruptedException {
        PoolFactoryMock manager = new PoolFactoryMock();
        var config = new PoolConfig();
        config.setPoolSize(25);
        config.setMaxPoolSize(50);
        config.setTimeout(600000);
        PoolManager<String, String> poolManager = new PoolManager<>(manager, config);
        poolManager.scheduledRun();
        long currentTime = System.currentTimeMillis();
        for (int i = 0; i < 199; i++) {
                poolManager.execute("Action " + i + " " + System.currentTimeMillis());
        }
        log.info("Total time: {}", System.currentTimeMillis() - currentTime);

        poolManager.scheduledRun();
        try {
            String result = poolManager.execute("Hola")
                    .get()
                    .get();

            log.info("Last request: {}", result + " " + (System.currentTimeMillis() - currentTime));
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
        var finalTime = System.currentTimeMillis() - currentTime;
        log.info("Total time: {} {}", System.currentTimeMillis() - currentTime, PoolableResourceMock.getCounter().get());
        //assert final time less than 5 seconds
        assert finalTime < 5000;
    }
}