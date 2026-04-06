package com.bonitasoft.connectors.msoutlook;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named = "MS_OUTLOOK_CLIENT_SECRET", matches = ".+")
class CreateEventConnectorIntegrationTest {

    @Test
    void shouldCreateEventAgainstRealApi() throws Exception {
        var connector = new CreateEventConnector();
        connector.setInputParameters(java.util.Map.of(
                "tenantId", System.getenv("MS_OUTLOOK_TENANT_ID"),
                "clientId", System.getenv("MS_OUTLOOK_CLIENT_ID"),
                "clientSecret", System.getenv("MS_OUTLOOK_CLIENT_SECRET"),
                "senderUserId", System.getenv("MS_OUTLOOK_SENDER_USER_ID"),
                "eventSubject", "Bonita Connector Test Event",
                "startDateTime", "2026-12-25T09:00:00",
                "endDateTime", "2026-12-25T10:00:00"));

        connector.validateInputParameters();
        connector.connect();
        connector.executeBusinessLogic();
        connector.disconnect();
    }
}
