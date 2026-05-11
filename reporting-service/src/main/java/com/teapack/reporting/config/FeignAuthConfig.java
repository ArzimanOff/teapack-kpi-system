package com.teapack.reporting.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Прокидывает JWT текущего HTTP-запроса в исходящие Feign-вызовы.
 * Без этого Feign к защищённым эндпоинтам (@PreAuthorize) получит 403.
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
