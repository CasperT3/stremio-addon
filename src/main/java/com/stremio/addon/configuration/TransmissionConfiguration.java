package com.stremio.addon.configuration;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
public class TransmissionConfiguration {
    @Value("${transmission.url:http://localhost:9091/transmission/rpc}")
    private String transmissionUrl;

    @Value("${transmission.username:transmission}")
    private String username;

    @Value("${transmission.password:transmission}")
    private String password;
}
