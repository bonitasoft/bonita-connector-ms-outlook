package com.bonitasoft.connectors.msoutlook;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.bonitasoft.connectors.msoutlook.model.GetEmailResult;
import org.bonitasoft.engine.connector.ConnectorValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

@ExtendWith(MockitoExtension.class)
class GetEmailConnectorTest {

    @Mock
    private MsOutlookClient mockClient;

    private GetEmailConnector connector;

    @BeforeEach
    void setUp() {
        connector = new GetEmailConnector();
    }

    @SuppressWarnings("unchecked")
    private static Object getOutput(AbstractMsOutlookConnector c, String name) throws Exception {
        var method = org.bonitasoft.engine.connector.AbstractConnector.class.getDeclaredMethod("getOutputParameters");
        method.setAccessible(true);
        return ((Map<String, Object>) method.invoke(c)).get(name);
    }

    private void setMandatoryInputs() {
        connector.setInputParameters(Map.of(
                "tenantId", "test-tenant",
                "clientId", "test-client",
                "clientSecret", "test-secret",
                "senderUserId", "user@test.com",
                "messageId", "AAMkAGI1AAAoZCfHAAA="));
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
        when(mockClient.getEmail(anyString(), anyBoolean()))
                .thenReturn(new GetEmailResult("msg1", "Test Subject", "from@test.com", "From",
                        "[]", "[]", "2026-03-25T09:00:00Z", "<p>body</p>", "html",
                        false, false, "[]", "normal", "conv1", "inet1", true, ""));

        connector.executeBusinessLogic();

        assertThat(getOutput(connector, "success")).isEqualTo(true);
        assertThat(getOutput(connector, "subject")).isEqualTo("Test Subject");
        assertThat(getOutput(connector, "fromEmail")).isEqualTo("from@test.com");
    }

    @Test
    void shouldFailValidationWhenMessageIdMissing() {
        connector.setInputParameters(Map.of(
                "tenantId", "t", "clientId", "c", "clientSecret", "s", "senderUserId", "u"));

        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class);
    }

    @Test
    void shouldHandleNotFoundError() throws Exception {
        setMandatoryInputs();
        injectMockClient();
        when(mockClient.getEmail(anyString(), anyBoolean()))
                .thenThrow(new MsOutlookException("Message not found", 404, false));

        connector.executeBusinessLogic();
        assertThat(getOutput(connector, "success")).isEqualTo(false);
        assertThat((String) getOutput(connector, "errorMessage")).contains("not found");
    }
}
