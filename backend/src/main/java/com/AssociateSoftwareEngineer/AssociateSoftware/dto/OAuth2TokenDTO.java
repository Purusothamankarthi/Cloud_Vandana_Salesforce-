package com.AssociateSoftwareEngineer.AssociateSoftware.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Holds OAuth 2.0 token response from Salesforce.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OAuth2TokenDTO {

    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("refresh_token")
    private String refreshToken;

    @JsonProperty("instance_url")
    private String instanceUrl;

    @JsonProperty("token_type")
    private String tokenType;

    @JsonProperty("issued_at")
    private String issuedAt;

    @JsonProperty("id")
    private String idUrl;

    @JsonProperty("scope")
    private String scope;
}
