package com.AssociateSoftwareEngineer.AssociateSoftware.service;

import com.AssociateSoftwareEngineer.AssociateSoftware.config.SalesforceConfig;
import com.AssociateSoftwareEngineer.AssociateSoftware.dto.OAuth2TokenDTO;
import com.AssociateSoftwareEngineer.AssociateSoftware.exception.SalesforceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Handles Salesforce OAuth 2.0 Authorization Code flow.
 *
 * Responsibilities:
 * - Build the authorization URL for user redirect
 * - Exchange authorization code for access/refresh tokens
 * - Refresh expired access tokens
 * - Revoke tokens on logout
 */
@Slf4j
@Service
public class SalesforceOAuthService {

    private final WebClient webClient;
    private final SalesforceConfig salesforceConfig;

    @Value("${spring.security.oauth2.client.registration.salesforce.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.salesforce.client-secret}")
    private String clientSecret;

    @Value("${spring.security.oauth2.client.registration.salesforce.redirect-uri}")
    private String redirectUri;

    public SalesforceOAuthService(WebClient webClient, SalesforceConfig salesforceConfig) {
        this.webClient = webClient;
        this.salesforceConfig = salesforceConfig;
    }

    /**
     * Build the Salesforce OAuth authorization URL.
     * The frontend redirects the user's browser to this URL.
     */
    public String getAuthorizationUrl() {
        String baseUrl = salesforceConfig.getInstanceUrl();
        try {
            return baseUrl + "/services/oauth2/authorize"
                    + "?response_type=code"
                    + "&client_id=" + clientId
                    + "&redirect_uri=" + java.net.URLEncoder.encode(redirectUri, "UTF-8")
                    + "&scope=api+refresh_token+full";
        } catch (java.io.UnsupportedEncodingException e) {
            throw new RuntimeException("UTF-8 encoding not supported", e);
        }
    }

    /**
     * Exchange the authorization code for OAuth tokens.
     *
     * @param code Authorization code from Salesforce callback
     * @return OAuth2TokenDTO containing access_token, refresh_token, instance_url
     */
    public OAuth2TokenDTO exchangeCodeForToken(String code) {
        log.info("Exchanging authorization code for tokens");

        String tokenUrl = salesforceConfig.getInstanceUrl() + "/services/oauth2/token";

        try {
            OAuth2TokenDTO tokenResponse = webClient.post()
                    .uri(tokenUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters
                            .fromFormData("grant_type", "authorization_code")
                            .with("code", code)
                            .with("client_id", clientId)
                            .with("client_secret", clientSecret)
                            .with("redirect_uri", redirectUri))
                    .retrieve()
                    .bodyToMono(OAuth2TokenDTO.class)
                    .block();

            if (tokenResponse == null || tokenResponse.getAccessToken() == null) {
                throw new SalesforceException("Failed to obtain access token from Salesforce");
            }

            log.info("Successfully obtained tokens. Instance URL: {}", tokenResponse.getInstanceUrl());
            return tokenResponse;

        } catch (SalesforceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error exchanging code for token", e);
            throw new SalesforceException("Failed to exchange authorization code: " + e.getMessage(), e);
        }
    }

    /**
     * Refresh an expired access token using the refresh token.
     *
     * @param refreshToken The refresh token
     * @return New OAuth2TokenDTO with fresh access_token
     */
    public OAuth2TokenDTO refreshAccessToken(String refreshToken) {
        log.info("Refreshing access token");

        String tokenUrl = salesforceConfig.getInstanceUrl() + "/services/oauth2/token";

        try {
            OAuth2TokenDTO tokenResponse = webClient.post()
                    .uri(tokenUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters
                            .fromFormData("grant_type", "refresh_token")
                            .with("refresh_token", refreshToken)
                            .with("client_id", clientId)
                            .with("client_secret", clientSecret))
                    .retrieve()
                    .bodyToMono(OAuth2TokenDTO.class)
                    .block();

            log.info("Access token refreshed successfully");
            return tokenResponse;

        } catch (Exception e) {
            log.error("Error refreshing access token", e);
            throw new SalesforceException("Failed to refresh access token: " + e.getMessage(), e);
        }
    }

    /**
     * Revoke an access token at Salesforce.
     * Called during logout.
     *
     * @param accessToken The access token to revoke
     */
    public void revokeToken(String accessToken) {
        log.info("Revoking access token");

        String revokeUrl = salesforceConfig.getInstanceUrl() + "/services/oauth2/revoke";

        try {
            webClient.post()
                    .uri(revokeUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData("token", accessToken))
                    .retrieve()
                    .toBodilessEntity()
                    .block();

            log.info("Token revoked successfully");

        } catch (Exception e) {
            log.warn("Failed to revoke token (non-critical): {}", e.getMessage());
        }
    }
}
