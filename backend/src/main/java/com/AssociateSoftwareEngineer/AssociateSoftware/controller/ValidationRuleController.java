package com.AssociateSoftwareEngineer.AssociateSoftware.controller;

import com.AssociateSoftwareEngineer.AssociateSoftware.dto.ApiResponseDTO;
import com.AssociateSoftwareEngineer.AssociateSoftware.dto.DeploymentResultDTO;
import com.AssociateSoftwareEngineer.AssociateSoftware.dto.ValidationRuleDTO;
import com.AssociateSoftwareEngineer.AssociateSoftware.service.UserSessionService;
import com.AssociateSoftwareEngineer.AssociateSoftware.service.ValidationRuleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.List;

/**
 * Controller for managing Salesforce Account validation rules.
 *
 * Endpoints:
 * - GET   /api/validation-rules                   → Fetch all rules
 * - PATCH /api/validation-rules/{name}/toggle      → Toggle single rule
 * - PATCH /api/validation-rules/toggle-multiple     → Toggle multiple rules
 * - GET   /api/validation-rules/{id}/status         → Check deployment status
 */
@Slf4j
@RestController
@RequestMapping("/api/validation-rules")
public class ValidationRuleController {

    private final ValidationRuleService validationRuleService;
    private final UserSessionService userSessionService;

    public ValidationRuleController(ValidationRuleService validationRuleService,
                                    UserSessionService userSessionService) {
        this.validationRuleService = validationRuleService;
        this.userSessionService = userSessionService;
    }

    /**
     * Fetch all validation rules from the Account object.
     */
    @GetMapping
    public ResponseEntity<ApiResponseDTO<List<ValidationRuleDTO>>> getAllValidationRules(HttpSession session) {
        log.info("GET /api/validation-rules — Fetching all rules");

        if (!userSessionService.isAuthenticated(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponseDTO<>(false, "Not authenticated", null));
        }

        String accessToken = userSessionService.getAccessToken(session);
        String instanceUrl = userSessionService.getInstanceUrl(session);

        List<ValidationRuleDTO> rules = validationRuleService.getAllValidationRules(accessToken, instanceUrl);

        log.info("Retrieved {} validation rules", rules.size());
        return ResponseEntity.ok(new ApiResponseDTO<>(true, "Validation rules retrieved", rules));
    }

    /**
     * Toggle a single validation rule (active ↔ inactive).
     */
    @PatchMapping("/{ruleName}/toggle")
    public ResponseEntity<ApiResponseDTO<DeploymentResultDTO>> toggleRule(
            @PathVariable String ruleName,
            HttpSession session) {

        log.info("PATCH /api/validation-rules/{}/toggle", ruleName);

        if (!userSessionService.isAuthenticated(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponseDTO<>(false, "Not authenticated", null));
        }

        String accessToken = userSessionService.getAccessToken(session);
        String instanceUrl = userSessionService.getInstanceUrl(session);

        DeploymentResultDTO result = validationRuleService.toggleValidationRule(
                ruleName, accessToken, instanceUrl);

        return ResponseEntity.ok(new ApiResponseDTO<>(true, "Rule toggled — deployment in progress", result));
    }

    /**
     * Toggle multiple validation rules at once.
     */
    @PatchMapping("/toggle-multiple")
    public ResponseEntity<ApiResponseDTO<DeploymentResultDTO>> toggleMultipleRules(
            @RequestBody List<String> ruleNames,
            HttpSession session) {

        log.info("PATCH /api/validation-rules/toggle-multiple — {} rules", ruleNames.size());

        if (!userSessionService.isAuthenticated(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponseDTO<>(false, "Not authenticated", null));
        }

        if (ruleNames == null || ruleNames.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponseDTO<>(false, "No rule names provided", null));
        }

        String accessToken = userSessionService.getAccessToken(session);
        String instanceUrl = userSessionService.getInstanceUrl(session);

        DeploymentResultDTO result = validationRuleService.toggleMultipleValidationRules(
                ruleNames, accessToken, instanceUrl);

        return ResponseEntity.ok(new ApiResponseDTO<>(true, "Rules toggled — deployment in progress", result));
    }

    /**
     * Check deployment status.
     */
    @GetMapping("/{deploymentId}/status")
    public ResponseEntity<ApiResponseDTO<DeploymentResultDTO>> checkDeploymentStatus(
            @PathVariable String deploymentId,
            HttpSession session) {

        log.info("GET /api/validation-rules/{}/status", deploymentId);

        if (!userSessionService.isAuthenticated(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponseDTO<>(false, "Not authenticated", null));
        }

        String accessToken = userSessionService.getAccessToken(session);
        String instanceUrl = userSessionService.getInstanceUrl(session);

        DeploymentResultDTO status = validationRuleService.checkDeploymentStatus(
                deploymentId, accessToken, instanceUrl);

        return ResponseEntity.ok(new ApiResponseDTO<>(true, "Deployment status retrieved", status));
    }
}
