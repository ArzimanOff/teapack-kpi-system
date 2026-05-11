package com.teapack.kpi.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Прокидывает JWT пользователя в исходящие Feign-вызовы.
 * Нужно для рекомендаций — они вызывают защищённые эндпоинты
 * (/api/shifts с фильтрами) и должны идти от имени запросившего.
 */
@Configuration
public class FeignAuthConfig {

    @Bean
    public RequestInterceptor forwardAuthorizationHeader() {
        return template -> {
            var attrs = RequestContextHolder.getRequestAttributes();
            if (attrs instanceof ServletRequestAttributes sra) {
                String auth = sra.getRequest().getHeader("Authorization");
                if (auth != null && !auth.isBlank()) {
                    template.header("Authorization", auth);
                }
            }
        };
    }
}
