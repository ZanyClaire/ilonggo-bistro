package com.ilonggobistro.config;

import com.ilonggobistro.shared.ImageStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final ImageStorageService imageStorageService;

    public WebConfig(ImageStorageService imageStorageService) {
        this.imageStorageService = imageStorageService;
    }

    @Bean
    public Clock bistroClock(@Value("${app.timezone:Asia/Manila}") String timezone) {
        return Clock.system(ZoneId.of(timezone));
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uploadUri = imageStorageService.getUploadRoot().toUri().toString();
        if (!uploadUri.endsWith("/")) {
            uploadUri += "/";
        }
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadUri);
    }
}
