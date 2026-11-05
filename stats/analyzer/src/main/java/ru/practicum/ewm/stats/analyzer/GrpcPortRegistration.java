package ru.practicum.ewm.stats.analyzer;

import com.netflix.appinfo.ApplicationInfoManager;
import net.devh.boot.grpc.server.event.GrpcServerStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "eureka.client.enabled", havingValue = "true", matchIfMissing = true)
public class GrpcPortRegistration {
    private final ApplicationInfoManager applicationInfoManager;

    public GrpcPortRegistration(ApplicationInfoManager applicationInfoManager) {
        this.applicationInfoManager = applicationInfoManager;
    }

    @EventListener
    public void registerGrpcPort(GrpcServerStartedEvent event) {
        applicationInfoManager.getInfo().getMetadata()
                .put("grpc.port", Integer.toString(event.getPort()));
    }
}
