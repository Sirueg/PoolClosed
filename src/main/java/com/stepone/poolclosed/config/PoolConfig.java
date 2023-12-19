package com.stepone.poolclosed.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PoolConfig {
    private int poolSize = 10;
    private int maxPoolSize = 15;
    private int maxIdleTime = 1500;
    private int timeout=10000;
}
