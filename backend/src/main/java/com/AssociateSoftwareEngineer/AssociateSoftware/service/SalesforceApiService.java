package com.AssociateSoftwareEngineer.AssociateSoftware.service;

import com.AssociateSoftwareEngineer.AssociateSoftware.config.SalesforceConfig;
import com.AssociateSoftwareEngineer.AssociateSoftware.exception.SalesforceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Map;

/**
 * Low-level service for making authenticated calls to Salesforce APIs.
 * Supports both REST (Tooling API) and SOAP (Metadata API) calls.
 */
@Slf4j
@Service
public class SalesforceApiService {

    private final WebClient webClient;
    private final SalesforceConfig salesforceConfig;

    public SalesforceApiService(WebClient webClient, SalesforceConfig salesforceConfig) {
        this.webClient = webClient;
        this.salesforceConfig = salesforceConfig;
    }

    /**
     * Execute a Tooling API query (SOQL).
     *
     * @param query       The SOQL query string
     * @param accessToken OAuth access token
     * @param instanceUrl Salesforce instance URL
     * @return Raw response as a Map
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> toolingQuery(String query, String accessToken, String instanceUrl) {
        String url = instanceUrl + "/services/data/v" + salesforceConfig.getApiVersion()
                + "/tooling/query?q={query}";

        log.debug("Tooling API query: {}", query);

        try {
            Map<String, Object> response = webClient.get()
                    .uri(url, query)
                    .header("Authorization", "Bearer " + accessToken)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            return response;

        } catch (WebClientResponseException e) {
            log.error("Tooling API query failed: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new SalesforceException("Tooling API query failed: " + e.getMessage(), e.getStatusCode().value());
        } catch (Exception e) {
            log.error("Tooling API query error", e);
            throw new SalesforceException("Tooling API query error: " + e.getMessage(), e);
        }
    }

    /**
     * Get a specific Tooling API record by ID.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> toolingGet(String sobjectType, String recordId,
                                          String accessToken, String instanceUrl) {
        String url = instanceUrl + "/services/data/v" + salesforceConfig.getApiVersion()
                + "/tooling/sobjects/" + sobjectType + "/" + recordId;

        try {
            return webClient.get()
                    .uri(url)
                    .header("Authorization", "Bearer " + accessToken)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

        } catch (WebClientResponseException e) {
            log.error("Tooling API GET failed: {}", e.getResponseBodyAsString());
            throw new SalesforceException("Tooling API GET failed: " + e.getMessage(), e.getStatusCode().value());
        }
    }

    /**
     * Update a Tooling API record (PATCH).
     */
    public void toolingUpdate(String sobjectType, String recordId, Map<String, Object> body,
                              String accessToken, String instanceUrl) {
        String url = instanceUrl + "/services/data/v" + salesforceConfig.getApiVersion()
                + "/tooling/sobjects/" + sobjectType + "/" + recordId;

        try {
            webClient.patch()
                    .uri(url)
                    .header("Authorization", "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .toBodilessEntity()
                    .block();

            log.info("Tooling API update successful: {}/{}", sobjectType, recordId);

        } catch (WebClientResponseException e) {
            log.error("Tooling API PATCH failed: {}", e.getResponseBodyAsString());
            throw new SalesforceException("Tooling API update failed: " + e.getMessage(), e.getStatusCode().value());
        }
    }

    /**
     * Send a SOAP request to the Metadata API.
     *
     * @param soapEnvelope The complete SOAP XML envelope
     * @param soapAction   The SOAPAction header value
     * @param accessToken  OAuth access token
     * @param instanceUrl  Salesforce instance URL
     * @return Raw XML response as String
     */
    public String metadataApiCall(String soapEnvelope, String soapAction,
                                  String accessToken, String instanceUrl) {
        String url = instanceUrl + "/services/Soap/m/" + salesforceConfig.getApiVersion();

        log.debug("Metadata API call - SOAPAction: {}", soapAction);

        try {
            String response = webClient.post()
                    .uri(url)
                    .header("Authorization", "Bearer " + accessToken)
                    .header("SOAPAction", soapAction)
                    .contentType(MediaType.TEXT_XML)
                    .bodyValue(soapEnvelope)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return response;

        } catch (WebClientResponseException e) {
            log.error("Metadata API call failed: {}", e.getResponseBodyAsString());
            throw new SalesforceException("Metadata API call failed: " + e.getMessage(), e.getStatusCode().value());
        } catch (Exception e) {
            log.error("Metadata API error", e);
            throw new SalesforceException("Metadata API error: " + e.getMessage(), e);
        }
    }

    /**
     * Deploy a zip package via Metadata API (deploy()).
     */
    public String deployMetadata(byte[] zipBytes, String accessToken, String instanceUrl) {
        String base64Zip = java.util.Base64.getEncoder().encodeToString(zipBytes);

        String soapEnvelope = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                + "xmlns:met=\"http://soap.sforce.com/2006/04/metadata\">"
                + "<soapenv:Header>"
                + "<met:SessionHeader><met:sessionId>" + accessToken + "</met:sessionId></met:SessionHeader>"
                + "</soapenv:Header>"
                + "<soapenv:Body>"
                + "<met:deploy>"
                + "<met:ZipFile>" + base64Zip + "</met:ZipFile>"
                + "<met:DeployOptions>"
                + "<met:rollbackOnError>true</met:rollbackOnError>"
                + "<met:singlePackage>true</met:singlePackage>"
                + "</met:DeployOptions>"
                + "</met:deploy>"
                + "</soapenv:Body>"
                + "</soapenv:Envelope>";

        String response = metadataApiCall(soapEnvelope, "deploy", accessToken, instanceUrl);

        // Extract deployment ID from response
        return extractValueFromXml(response, "id");
    }

    /**
     * Check deployment status via Metadata API.
     */
    public Map<String, String> checkDeployStatus(String deploymentId, String accessToken, String instanceUrl) {
        String soapEnvelope = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                + "xmlns:met=\"http://soap.sforce.com/2006/04/metadata\">"
                + "<soapenv:Header>"
                + "<met:SessionHeader><met:sessionId>" + accessToken + "</met:sessionId></met:SessionHeader>"
                + "</soapenv:Header>"
                + "<soapenv:Body>"
                + "<met:checkDeployStatus>"
                + "<met:asyncProcessId>" + deploymentId + "</met:asyncProcessId>"
                + "<met:includeDetails>true</met:includeDetails>"
                + "</met:checkDeployStatus>"
                + "</soapenv:Body>"
                + "</soapenv:Envelope>";

        String response = metadataApiCall(soapEnvelope, "checkDeployStatus", accessToken, instanceUrl);

        String errorMessage = extractValueFromXml(response, "errorMessage");
        if (errorMessage == null || errorMessage.trim().isEmpty()) {
            errorMessage = extractValueFromXml(response, "problem");
        }

        return Map.of(
                "status", extractValueFromXml(response, "status"),
                "success", extractValueFromXml(response, "success"),
                "id", deploymentId,
                "errorMessage", errorMessage != null ? errorMessage : ""
        );
    }

    /**
     * Simple XML value extractor (avoids heavy XML parsing dependency).
     */
    private String extractValueFromXml(String xml, String tagName) {
        if (xml == null) return "";
        // Try with namespace prefix first
        String[] patterns = {
                "<" + tagName + ">", "<met:" + tagName + ">",
                "<ns:" + tagName + ">", "<result>" // fallback patterns
        };
        String[] endPatterns = {
                "</" + tagName + ">", "</met:" + tagName + ">",
                "</ns:" + tagName + ">"
        };

        for (int i = 0; i < patterns.length - 1; i++) {
            int start = xml.indexOf(patterns[i]);
            if (start >= 0) {
                start += patterns[i].length();
                int end = xml.indexOf(endPatterns[i], start);
                if (end > start) {
                    return xml.substring(start, end).trim();
                }
            }
        }
        return "";
    }

}

