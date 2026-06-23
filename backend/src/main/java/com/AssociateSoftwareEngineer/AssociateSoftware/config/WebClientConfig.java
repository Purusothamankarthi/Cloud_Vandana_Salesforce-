package com.AssociateSoftwareEngineer.AssociateSoftware.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;

/**
 * WebClient configuration for Salesforce API calls.
 * Uses a custom connection pool to avoid "Connection reset" errors.
 */
@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient() {
        // Configure connection provider to aggressively drop idle connections
        // Salesforce often closes keep-alive connections prematurely, causing "Connection reset"
        ConnectionProvider provider = ConnectionProvider.builder("salesforce-pool")
                .maxIdleTime(Duration.ofSeconds(10))
                .maxLifeTime(Duration.ofSeconds(30))
                .pendingAcquireTimeout(Duration.ofSeconds(30))
                .build();

        HttpClient httpClient = HttpClient.create(provider);

        // Increase buffer size for large Salesforce API responses
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(10 * 1024 * 1024)) // 10 MB
                .build();

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(strategies)
                .build();
    }
}
