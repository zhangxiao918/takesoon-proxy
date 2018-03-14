package com.takesoon.proxy.core.impl;

import com.ouer.cache.redis.RedisCacheService;
import com.takesoon.proxy.core.AbstractProxyPool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * 代理类
 * @author bluestome
 */
@Component
public class ProxyPool extends AbstractProxyPool {

    @Value("${proxypool.servers}")
    private String proxyServer;

    @Autowired
    private RedisCacheService cookieCache;

    @PostConstruct
    public void init() {
        init(proxyServer, cookieCache);
    }
}
