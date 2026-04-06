package com.bonitasoft.connectors.msoutlook;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named = "MS_OUTLOOK_CLIENT_SECRET", matches = ".+")
class SendActionableConnectorIntegrationTest {

    @Test
    void shouldSendActionableAgainstRealApi() throws Exception {
        // Requires MS_OUTLOOK_ORIGINATOR_ID and MS_OUTLOOK_TEST_RECIPIENT
        var connector = new SendActionableConnector();
        connector.setInputParameters(java.util.Map.ofEntries(
                java.util.Map.entry("tenantId", System.getenv("MS_OUTLOOK_TENANT_ID")),
                java.util.Map.entry("clientId", System.getenv("MS_OUTLOOK_CLIENT_ID")),
                java.util.Map.entry("clientSecret", System.getenv("MS_OUTLOOK_CLIENT_SECRET")),
                java.util.Map.entry("senderUserId", System.getenv("MS_OUTLOOK_SENDER_USER_ID")),
                java.util.Map.entry("toRecipients", "[{\"email\":\"" + System.getenv("MS_OUTLOOK_TEST_RECIPIENT") + "\"}]"),
                java.util.Map.entry("emailSubject", "Test Actionable Message"),
                java.util.Map.entry("emailBodyFallback", "<p>Fallback body</p>"),
                java.util.Map.entry("cardTitle", "Test Card"),
                java.util.Map.entry("cardBody", "[{\"type\":\"TextBlock\",\"text\":\"Test\"}]"),
                java.util.Map.entry("actions", "[{\"@type\":\"HttpPOST\",\"name\":\"OK\",\"target\":\"https://example.com\"}]"),
                java.util.Map.entry("callbackUrl", "https://example.com/callback"),
                java.util.Map.entry("originator", System.getenv("MS_OUTLOOK_ORIGINATOR_ID"))));

        connector.validateInputParameters();
        connector.connect();
        connector.executeBusinessLogic();
        connector.disconnect();
    }
}
