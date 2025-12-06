package com.yamiapp.util;

import com.yamiapp.exception.BadGatewayException;
import com.yamiapp.exception.ErrorStrings;
import com.yamiapp.exception.ServiceUnavailableException;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.function.Function;

@Component
public class RedisHelper {

    private final JedisPool jedisPool;

    public RedisHelper(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    public <T> T execute(Function<Jedis, T> function) {
        try (var jedis = jedisPool.getResource()) {
            return function.apply(jedis);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            throw new ServiceUnavailableException(ErrorStrings.REDIS_ERROR.getMessage());
        }
    }

    public void executeVoid(java.util.function.Consumer<Jedis> consumer) {
        try (var jedis = jedisPool.getResource()) {
            consumer.accept(jedis);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            throw new ServiceUnavailableException(ErrorStrings.REDIS_ERROR.getMessage());
        }
    }
}
