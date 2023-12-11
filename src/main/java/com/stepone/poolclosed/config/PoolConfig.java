package com.stepone.poolclosed.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PoolConfig {
    private int poolSize = 10;
    private int maxPoolSize = 50;
    private int maxIdleTime = 500;
    private int timeout=5000;
}
