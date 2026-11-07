package ru.practicum.ewm.stats.collector;

import com.netflix.appinfo.ApplicationInfoManager;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.event.GrpcServerStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "eureka.client.enabled", havingValue = "true", matchIfMissing = true)
public class GrpcPortRegistration {
    private final ApplicationInfoManager applicationInfoManager;

    /** Publishes the bound port in the Eureka metadata consumed by gRPC discovery. */
    @EventListener
    public void registerGrpcPort(GrpcServerStartedEvent event) {
        applicationInfoManager.registerAppMetadata(Map.of("gRPC_port", Integer.toString(event.getPort())));
    }
}
