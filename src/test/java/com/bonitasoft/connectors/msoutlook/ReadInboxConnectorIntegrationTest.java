package com.bonitasoft.connectors.msoutlook;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named = "MS_OUTLOOK_CLIENT_SECRET", matches = ".+")
class ReadInboxConnectorIntegrationTest {

    @Test
    void shouldReadInboxAgainstRealApi() throws Exception {
        var connector = new ReadInboxConnector();
        connector.setInputParameters(java.util.Map.of(
                "tenantId", System.getenv("MS_OUTLOOK_TENANT_ID"),
                "clientId", System.getenv("MS_OUTLOOK_CLIENT_ID"),
                "clientSecret", System.getenv("MS_OUTLOOK_CLIENT_SECRET"),
                "senderUserId", System.getenv("MS_OUTLOOK_SENDER_USER_ID")));

        connector.validateInputParameters();
        connector.connect();
        connector.executeBusinessLogic();
        connector.disconnect();
    }
}
