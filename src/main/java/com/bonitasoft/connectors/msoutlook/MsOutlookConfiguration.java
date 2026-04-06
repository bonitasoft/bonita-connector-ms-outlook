package com.bonitasoft.connectors.msoutlook;

import lombok.Builder;
import lombok.Data;

/**
 * Configuration for MS Outlook connector -- holds connection and auth parameters.
 */
@Data
@Builder
public class MsOutlookConfiguration {

    // Connection / Auth parameters (Project/Runtime scope)
    private String tenantId;
    private String clientId;
    private String clientSecret;
    private String senderUserId;

    @Builder.Default
    private String basePath = "https://graph.microsoft.com/v1.0";

    @Builder.Default
    private int connectTimeout = 30000;

    @Builder.Default
    private int readTimeout = 60000;

    @Builder.Default
    private int maxRetries = 3;

    /**
     * Resolve a parameter from input, system property, or environment variable.
     */
    public static String resolveParam(java.util.Map<String, Object> params, String key, String sysPropKey, String envVar) {
        Object value = params.get(key);
        String strValue = value != null ? value.toString() : null;
        if (strValue == null || strValue.isBlank()) {
            strValue = System.getProperty(sysPropKey);
        }
        if (strValue == null || strValue.isBlank()) {
            strValue = System.getenv(envVar);
        }
        return strValue;
    }
}
