package com.bonitasoft.connectors.msoutlook;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.bonitasoft.connectors.msoutlook.model.SendActionableResult;
import org.bonitasoft.engine.connector.ConnectorValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

@ExtendWith(MockitoExtension.class)
class SendActionableConnectorTest {

    @Mock
    private MsOutlookClient mockClient;

    private SendActionableConnector connector;

    @BeforeEach
    void setUp() {
        connector = new SendActionableConnector();
    }

    @SuppressWarnings("unchecked")
    private static Object getOutput(AbstractMsOutlookConnector c, String name) throws Exception {
        var method = org.bonitasoft.engine.connector.AbstractConnector.class.getDeclaredMethod("getOutputParameters");
        method.setAccessible(true);
        return ((Map<String, Object>) method.invoke(c)).get(name);
    }

    private void setMandatoryInputs() {
        connector.setInputParameters(Map.ofEntries(
                Map.entry("tenantId", "test-tenant"),
                Map.entry("clientId", "test-client"),
                Map.entry("clientSecret", "test-secret"),
                Map.entry("senderUserId", "user@test.com"),
                Map.entry("toRecipients", "[{\"email\":\"mgr@test.com\"}]"),
                Map.entry("emailSubject", "Approval Required"),
                Map.entry("emailBodyFallback", "<p>Please approve</p>"),
                Map.entry("cardTitle", "Purchase Request"),
                Map.entry("cardBody", "[{\"type\":\"TextBlock\",\"text\":\"Vendor: ACME\"}]"),
                Map.entry("actions", "[{\"@type\":\"HttpPOST\",\"name\":\"Approve\",\"target\":\"https://bonita/api\"}]"),
                Map.entry("callbackUrl", "https://bonita/api/task/123"),
                Map.entry("originator", "12345678-1234-1234-1234-123456789012")));
    }

    private void injectMockClient() throws Exception {
        var clientField = AbstractMsOutlookConnector.class.getDeclaredField("client");
        clientField.setAccessible(true);
        clientField.set(connector, mockClient);
    }

    @Test
    void shouldExecuteSuccessfully() throws Exception {
        setMandatoryInputs();
        injectMockClient();
        when(mockClient.sendActionable(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new SendActionableResult(null, true, ""));

        connector.executeBusinessLogic();
        assertThat(getOutput(connector, "success")).isEqualTo(true);
    }

    @Test
    void shouldFailValidationWhenOriginatorMissing() {
        connector.setInputParameters(Map.ofEntries(
                Map.entry("tenantId", "t"), Map.entry("clientId", "c"),
                Map.entry("clientSecret", "s"), Map.entry("senderUserId", "u"),
                Map.entry("toRecipients", "[{}]"), Map.entry("emailSubject", "S"),
                Map.entry("emailBodyFallback", "F"), Map.entry("cardTitle", "T"),
                Map.entry("cardBody", "[{}]"), Map.entry("actions", "[{}]"),
                Map.entry("callbackUrl", "https://test.com")));

        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class);
    }

    @Test
    void shouldSetErrorOutputsOnFailure() throws Exception {
        setMandatoryInputs();
        injectMockClient();
        when(mockClient.sendActionable(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new MsOutlookException("Permission denied", 403, false));

        connector.executeBusinessLogic();
        assertThat(getOutput(connector, "success")).isEqualTo(false);
        assertThat((String) getOutput(connector, "errorMessage")).contains("Permission denied");
    }
}
