package ru.practicum.analyzer.config;

import com.netflix.appinfo.ApplicationInfoManager;
import net.devh.boot.grpc.server.event.GrpcServerStartedEvent;
import org.springframework.cloud.netflix.eureka.EurekaInstanceConfigBean;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class GrpcEurekaRegistrationConfig {

    private final EurekaInstanceConfigBean eurekaInstanceConfigBean;
    private final ApplicationInfoManager applicationInfoManager;

    public GrpcEurekaRegistrationConfig(EurekaInstanceConfigBean eurekaInstanceConfigBean,
                                        ApplicationInfoManager applicationInfoManager) {
        this.eurekaInstanceConfigBean = eurekaInstanceConfigBean;
        this.applicationInfoManager = applicationInfoManager;
    }

    @EventListener
    public void onGrpcServerStarted(GrpcServerStartedEvent event) {
        int port = event.getPort();
        String portStr = String.valueOf(port);

        Map<String, String> metadata = eurekaInstanceConfigBean.getMetadataMap();
        metadata.put("grpcPort", portStr);
        metadata.put("gRPC.port", portStr);
        metadata.put("gRPC_port", portStr);
        metadata.put("grpc.port", portStr);

        applicationInfoManager.registerAppMetadata(metadata);
    }
}