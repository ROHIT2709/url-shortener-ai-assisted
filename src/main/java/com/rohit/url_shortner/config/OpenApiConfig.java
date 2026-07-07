package com.rohit.url_shortner.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI urlShortenerOpenApi() {
        return new OpenAPI().info(new Info()
                .title("URL Shortener API")
                .description("Shortens URLs to base62 codes, redirects with click tracking, "
                        + "and exposes per-link statistics. Interactive docs at /docs.")
                .version("v1"));
    }
}
