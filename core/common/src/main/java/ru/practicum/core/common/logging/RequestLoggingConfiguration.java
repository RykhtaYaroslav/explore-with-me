package ru.practicum.core.common.logging;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

@Configuration(proxyBeanMethods = false)
public class RequestLoggingConfiguration {
    @Bean
    public CommonsRequestLoggingFilter commonsRequestLoggingFilter() {
        CommonsRequestLoggingFilter filter = new CommonsRequestLoggingFilter();
        filter.setIncludeQueryString(true);
        filter.setIncludePayload(true);
        filter.setIncludeHeaders(false);
        filter.setIncludeClientInfo(false);
        filter.setMaxPayloadLength(4096);
        filter.setBeforeMessagePrefix("Incoming controller request: ");
        filter.setAfterMessagePrefix("Incoming controller request: ");
        return filter;
    }
}
