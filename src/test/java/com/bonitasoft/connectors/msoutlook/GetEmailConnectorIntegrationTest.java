package com.bonitasoft.connectors.msoutlook;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named = "MS_OUTLOOK_CLIENT_SECRET", matches = ".+")
class GetEmailConnectorIntegrationTest {

    @Test
    void shouldGetEmailAgainstRealApi() throws Exception {
        // Requires MS_OUTLOOK_TEST_MESSAGE_ID to be set
        var connector = new GetEmailConnector();
        connector.setInputParameters(java.util.Map.of(
                "tenantId", System.getenv("MS_OUTLOOK_TENANT_ID"),
                "clientId", System.getenv("MS_OUTLOOK_CLIENT_ID"),
                "clientSecret", System.getenv("MS_OUTLOOK_CLIENT_SECRET"),
                "senderUserId", System.getenv("MS_OUTLOOK_SENDER_USER_ID"),
                "messageId", System.getenv("MS_OUTLOOK_TEST_MESSAGE_ID")));

        connector.validateInputParameters();
        connector.connect();
        connector.executeBusinessLogic();
        connector.disconnect();
    }
}
