package com.falsegod.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

/**
 * 跨域配置。
 *
 * <p>正常运行方式是前端经 Vite 代理访问（/api -> localhost:8080），本身不跨域。
 * 这里只为「前端直接以 5173 源访问后端」的场景保留白名单。
 *
 * <p>安全要点：绝不能同时用 {@code allowedOrigins("*")} 与
 * {@code allowCredentials(true)}，否则任意站点都能带 cookie 调你的接口。
 * 现在只允许配置中列出的具体来源。
 */
@Configuration
public class CorsConfig {

    /** 允许的来源，逗号分隔，可在 application.yml 覆盖 */
    @Value("${falsegod.cors.allowed-origins:http://localhost:5173,http://127.0.0.1:5173}")
    private String allowedOrigins;

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        config.setAllowedOrigins(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Content-Type", "Authorization"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
