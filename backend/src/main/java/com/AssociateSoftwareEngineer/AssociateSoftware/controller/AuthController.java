package com.AssociateSoftwareEngineer.AssociateSoftware.controller;

import com.AssociateSoftwareEngineer.AssociateSoftware.dto.ApiResponseDTO;
import com.AssociateSoftwareEngineer.AssociateSoftware.dto.UserProfileDTO;
import com.AssociateSoftwareEngineer.AssociateSoftware.service.SalesforceOAuthService;
import com.AssociateSoftwareEngineer.AssociateSoftware.service.UserSessionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.net.URI;

/**
 * Authentication controller handling Salesforce OAuth 2.0 Authorization Code flow.
 *
 * Endpoints:
 * - GET  /api/auth/login    → Redirect to Salesforce OAuth login
 * - GET  /api/auth/callback → Handle OAuth callback, exchange code for tokens
 * - GET  /api/auth/user     → Get current user profile
 * - GET  /api/auth/status   → Check authentication status
 * - POST /api/auth/logout   → Logout and revoke tokens
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final SalesforceOAuthService oAuthService;
    private final UserSessionService userSessionService;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    public AuthController(SalesforceOAuthService oAuthService, UserSessionService userSessionService) {
        this.oAuthService = oAuthService;
        this.userSessionService = userSessionService;
    }

    /**
     * Redirect user to Salesforce OAuth login page.
     */
    @GetMapping("/login")
    public ResponseEntity<Void> login() {
        log.info("Login endpoint called — redirecting to Salesforce OAuth");
        String authUrl = oAuthService.getAuthorizationUrl();
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(authUrl))
                .build();
    }

    /**
     * Handle OAuth callback from Salesforce.
     * Exchange authorization code for tokens and redirect to frontend dashboard.
     */
    @GetMapping("/callback")
    public ResponseEntity<Void> handleCallback(
            @RequestParam String code,
            HttpSession session) {

        log.info("OAuth callback received");

        try {
            // Exchange authorization code for tokens
            var tokenResponse = oAuthService.exchangeCodeForToken(code);

            // Store tokens in session
            userSessionService.storeTokensInSession(session, tokenResponse);

            log.info("Authentication successful — redirecting to dashboard");

            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(frontendUrl + "/dashboard"))
                    .build();

        } catch (Exception e) {
            log.error("OAuth callback error", e);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(frontendUrl + "/error?message=Authentication+failed"))
                    .build();
        }
    }

    /**
     * Get current user profile from Salesforce.
     */
    @GetMapping("/user")
    public ResponseEntity<ApiResponseDTO<UserProfileDTO>> getUserProfile(HttpSession session) {
        log.info("User profile endpoint called");

        if (!userSessionService.isAuthenticated(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponseDTO<>(false, "Not authenticated", null));
        }

        UserProfileDTO profile = userSessionService.getUserProfile(session);
        if (profile == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "Failed to retrieve user profile", null));
        }

        return ResponseEntity.ok(new ApiResponseDTO<>(true, "User profile retrieved", profile));
    }

    /**
     * Check if current session is authenticated.
     */
    @GetMapping("/status")
    public ResponseEntity<ApiResponseDTO<Boolean>> getAuthStatus(HttpSession session) {
        boolean authenticated = userSessionService.isAuthenticated(session);
        log.debug("Auth status check: {}", authenticated);

        return ResponseEntity.ok(new ApiResponseDTO<>(true, "Auth status retrieved", authenticated));
    }

    /**
     * Logout — revoke token and invalidate session.
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponseDTO<Void>> logout(HttpSession session) {
        log.info("Logout endpoint called");

        String accessToken = userSessionService.getAccessToken(session);

        if (accessToken != null) {
            try {
                oAuthService.revokeToken(accessToken);
            } catch (Exception e) {
                log.warn("Token revocation failed (non-critical): {}", e.getMessage());
            }
        }

        session.invalidate();

        return ResponseEntity.ok(new ApiResponseDTO<>(true, "Logged out successfully", null));
    }
}
