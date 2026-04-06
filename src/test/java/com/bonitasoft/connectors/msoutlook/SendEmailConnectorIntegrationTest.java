package com.bonitasoft.connectors.msoutlook;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/**
 * Integration test for Send Email -- skipped unless MS Outlook credentials are available.
 */
@EnabledIfEnvironmentVariable(named = "MS_OUTLOOK_CLIENT_SECRET", matches = ".+")
class SendEmailConnectorIntegrationTest {

    @Test
    void shouldSendEmailAgainstRealApi() throws Exception {
        var connector = new SendEmailConnector();
        connector.setInputParameters(java.util.Map.of(
                "tenantId", System.getenv("MS_OUTLOOK_TENANT_ID"),
                "clientId", System.getenv("MS_OUTLOOK_CLIENT_ID"),
                "clientSecret", System.getenv("MS_OUTLOOK_CLIENT_SECRET"),
                "senderUserId", System.getenv("MS_OUTLOOK_SENDER_USER_ID"),
                "toRecipients", "[{\"email\":\"" + System.getenv("MS_OUTLOOK_TEST_RECIPIENT") + "\"}]",
                "emailSubject", "Bonita Connector Integration Test",
                "emailBody", "<p>This is an automated integration test email.</p>"));

        connector.validateInputParameters();
        connector.connect();
        connector.executeBusinessLogic();
        connector.disconnect();
    }
}
