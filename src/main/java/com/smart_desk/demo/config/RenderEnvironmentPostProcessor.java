package com.smart_desk.demo.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Parses Render's DATABASE_URL and REDIS_URL into Spring Boot properties.
 * Render injects postgresql://user:pass@host:port/db — JDBC needs the jdbc: prefix.
 * Must run before datasource/redis beans are created, hence EnvironmentPostProcessor.
 */
public class RenderEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> props = new HashMap<>();

        String databaseUrl = environment.getProperty("DATABASE_URL");
        if (databaseUrl != null && !databaseUrl.isBlank()) {
            URI uri = URI.create(databaseUrl);
            String jdbcUrl = "jdbc:postgresql://" + uri.getHost() + ":" + uri.getPort() + uri.getPath();
            String[] userInfo = uri.getUserInfo().split(":", 2);
            props.put("spring.datasource.url", jdbcUrl);
            props.put("spring.datasource.username", userInfo[0]);
            props.put("spring.datasource.password", userInfo.length > 1 ? userInfo[1] : "");
        }

        String redisUrl = environment.getProperty("REDIS_URL");
        if (redisUrl != null && !redisUrl.isBlank()) {
            // Upstash uses rediss:// (TLS); normalize to redis:// for URI parsing
            boolean tls = redisUrl.startsWith("rediss://");
            URI uri = URI.create(tls ? redisUrl.replaceFirst("rediss://", "redis://") : redisUrl);
            props.put("spring.data.redis.host", uri.getHost());
            props.put("spring.data.redis.port", uri.getPort() == -1 ? 6379 : uri.getPort());
            props.put("spring.data.redis.ssl.enabled", tls);
            if (uri.getUserInfo() != null) {
                String[] userInfo = uri.getUserInfo().split(":", 2);
                if (userInfo.length > 1 && !userInfo[1].isBlank()) {
                    props.put("spring.data.redis.password", userInfo[1]);
                }
            }
        }

        if (!props.isEmpty()) {
            environment.getPropertySources().addFirst(new MapPropertySource("renderEnv", props));
        }
    }
}
