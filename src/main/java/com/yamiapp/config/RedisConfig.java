package com.yamiapp.config;

import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.net.URI;

@Configuration
public class RedisConfig {

    @Value("${redis.url}")
    private String redisUrl;

    @Bean
    public JedisPool jedisPool() {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setJmxEnabled(false);

        try {
            URI redisUri = new URI(redisUrl);
            return new JedisPool(poolConfig, redisUri);
        } catch (Exception e) {
            throw new RuntimeException("Invalid Redis URI", e);
        }
    }
}
