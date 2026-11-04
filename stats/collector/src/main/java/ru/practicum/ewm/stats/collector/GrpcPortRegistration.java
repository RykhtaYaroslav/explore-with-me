package ru.practicum.ewm.stats.collector;

import com.netflix.appinfo.ApplicationInfoManager;
import net.devh.boot.grpc.server.event.GrpcServerStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
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
