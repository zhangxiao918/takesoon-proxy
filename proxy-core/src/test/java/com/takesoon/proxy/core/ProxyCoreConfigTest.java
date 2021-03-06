package com.takesoon.proxy.core;

import com.ouer.cache.redis.RedisCacheService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;

/**
 * 核心服务类配置项
 *
 * @author bluestome
 */
@Component
public class ProxyCoreConfigTest {

    @Value("${redis.host}")
    private String redisHost;

    @Value("${redis.port}")
    private int redisPort;

    @Value("${redis.db}")
    private int redisDb;

    @Value("${cookie.cache.key.prefix")
    private String cookieKey;


    /**
     * 佣金调用缓存
     *
     * @return
     */
    @Bean
    public RedisCacheService cookieCache() {
        RedisCacheService redisCacheService = new RedisCacheService(redisHost, redisPort, redisDb, cookieKey);
        redisCacheService.setUseLoaclCache(false);
        return redisCacheService;
    }
}
