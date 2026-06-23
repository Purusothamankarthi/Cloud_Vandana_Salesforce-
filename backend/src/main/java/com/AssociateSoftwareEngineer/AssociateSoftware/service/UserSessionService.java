package com.AssociateSoftwareEngineer.AssociateSoftware.service;

import com.AssociateSoftwareEngineer.AssociateSoftware.dto.OAuth2TokenDTO;
import com.AssociateSoftwareEngineer.AssociateSoftware.dto.UserProfileDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import jakarta.servlet.http.HttpSession;
import java.util.Map;

/**
 * Manages user session data (OAuth tokens, profile).
 */
@Slf4j
@Service
public class UserSessionService {

    private final WebClient webClient;

    public UserSessionService(WebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * Store OAuth tokens in the HTTP session.
     */
    public void storeTokensInSession(HttpSession session, OAuth2TokenDTO tokenResponse) {
        session.setAttribute("accessToken", tokenResponse.getAccessToken());
        session.setAttribute("refreshToken", tokenResponse.getRefreshToken());
        session.setAttribute("instanceUrl", tokenResponse.getInstanceUrl());
        session.setAttribute("authenticated", true);
        log.debug("Tokens stored in session: {}", session.getId());
    }

    /**
     * Retrieve the user profile from Salesforce using the stored access token.
     */
    public UserProfileDTO getUserProfile(HttpSession session) {
        String accessToken = (String) session.getAttribute("accessToken");
        String instanceUrl = (String) session.getAttribute("instanceUrl");

        if (accessToken == null || instanceUrl == null) {
            return null;
        }

        try {
            // Call Salesforce UserInfo endpoint
            @SuppressWarnings("unchecked")
            Map<String, Object> userInfo = webClient.get()
                    .uri(instanceUrl + "/services/oauth2/userinfo")
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (userInfo == null) {
                return null;
            }

            return UserProfileDTO.builder()
                    .userId((String) userInfo.get("user_id"))
                    .name((String) userInfo.get("name"))
                    .email((String) userInfo.get("email"))
                    .orgId((String) userInfo.get("organization_id"))
                    .username((String) userInfo.get("preferred_username"))
                    .instanceUrl(instanceUrl)
                    .build();

        } catch (Exception e) {
            log.error("Error fetching user profile from Salesforce", e);
            return null;
        }
    }

    /**
     * Check if the session contains valid authentication data.
     */
    public boolean isAuthenticated(HttpSession session) {
        Boolean authenticated = (Boolean) session.getAttribute("authenticated");
        String accessToken = (String) session.getAttribute("accessToken");
        return Boolean.TRUE.equals(authenticated) && accessToken != null;
    }

    /**
     * Get access token from session.
     */
    public String getAccessToken(HttpSession session) {
        return (String) session.getAttribute("accessToken");
    }

    /**
     * Get instance URL from session.
     */
    public String getInstanceUrl(HttpSession session) {
        return (String) session.getAttribute("instanceUrl");
    }
}
