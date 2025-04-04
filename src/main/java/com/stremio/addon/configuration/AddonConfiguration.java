package com.stremio.addon.configuration;


import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import org.springframework.http.MediaType;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

@Configuration
@EnableJdbcRepositories(basePackages = "com.stremio.addon")
@EnableScheduling
@Data
public class AddonConfiguration {
    @Value("${addon.download.path}")
    private String downloadPath;
    @Value("${addon.videos.path}")
    private String videosPath;
    @Value("${addon.jackett.apiKey}")
    private String apiKey;

    @Bean("restTemplateJson")
    public RestTemplate restTemplateJson() {
        return new RestTemplate();
    }

    @Bean("restTemplateXml")
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().add(xmlMessageConverter());
        return restTemplate;
    }

    @Bean
    public MappingJackson2XmlHttpMessageConverter xmlMessageConverter() {
        XmlMapper xmlMapper = new XmlMapper();
        MappingJackson2XmlHttpMessageConverter xmlConverter = new MappingJackson2XmlHttpMessageConverter(xmlMapper);

        // Añade soporte para application/rss+xml
        xmlConverter.setSupportedMediaTypes(Arrays.asList(
                MediaType.APPLICATION_XML,
                MediaType.TEXT_XML,
                MediaType.parseMediaType("application/rss+xml")
        ));

        return xmlConverter;
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**") // Aplica a todos los endpoints
                        .allowedOrigins("*") // Orígenes permitidos
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // Métodos HTTP permitidos
                        .allowedHeaders("*"); // Permite todos los encabezados
            }
        };
    }

}
