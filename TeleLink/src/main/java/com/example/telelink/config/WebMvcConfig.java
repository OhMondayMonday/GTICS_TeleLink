package com.example.telelink.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        // Since we disabled automatic resource mappings, we need to add all of them manually
        // This gives us full control over the patterns and prevents conflicts
        
        // Admin static resources
        registry.addResourceHandler("/admin/assets/**")
                .addResourceLocations("classpath:/static/admin/assets/")
                .setCachePeriod(3600);
        
        // Superadmin static resources  
        registry.addResourceHandler("/superadmin/assets/**")
                .addResourceLocations("classpath:/static/superadmin/assets/")
                .setCachePeriod(3600);
        
        // General assets
        registry.addResourceHandler("/assets/**")
                .addResourceLocations("classpath:/static/assets/")
                .setCachePeriod(3600);
        
        // Vecino assets
        registry.addResourceHandler("/vecino.assets/**")
                .addResourceLocations("classpath:/static/vecino.assets/")
                .setCachePeriod(3600);
        
        // Static files under /static/** path
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(3600);
        
        // Common files like favicon
        registry.addResourceHandler("/favicon.ico", "/robots.txt")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(3600);
    }
}
