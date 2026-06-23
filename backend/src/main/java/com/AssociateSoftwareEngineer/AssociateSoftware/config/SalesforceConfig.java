package com.AssociateSoftwareEngineer.AssociateSoftware.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for Salesforce integration.
 * Reads from application.yml under the 'salesforce' prefix.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "salesforce")
public class SalesforceConfig {

    private String instanceUrl = "https://login.salesforce.com";
    private String apiVersion = "59.0";
}
