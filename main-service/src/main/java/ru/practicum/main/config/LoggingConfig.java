package ru.practicum.main.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

@Configuration
public class LoggingConfig {

    @Bean
    public CommonsRequestLoggingFilter logFilter() {
        CommonsRequestLoggingFilter filter = new CommonsRequestLoggingFilter();
        filter.setIncludeQueryString(true);   // Логировать query-параметры (?param=val)
        filter.setIncludePayload(true);       // Логировать тело запроса (JSON)
        filter.setMaxPayloadLength(10000);    // Максимальная длина тела в логе
        filter.setIncludeHeaders(false);      // Не писать заголовки (или true, если нужно)
        filter.setAfterMessagePrefix("REQUEST DATA: ");
        return filter;
    }
}