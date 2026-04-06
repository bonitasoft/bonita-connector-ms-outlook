package com.bonitasoft.connectors.msoutlook;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.bonitasoft.connectors.msoutlook.model.SendTemplateResult;
import org.bonitasoft.engine.connector.ConnectorValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

@ExtendWith(MockitoExtension.class)
class SendTemplateConnectorTest {

    @Mock
    private MsOutlookClient mockClient;

    private SendTemplateConnector connector;

    @BeforeEach
    void setUp() {
        connector = new SendTemplateConnector();
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
                "toRecipients", "[{\"email\":\"john@test.com\"}]",
                "emailSubject", "Welcome {{name}}",
                "templateHtml", "<p>Hello {{name}}, your start date is {{date}}</p>",
                "templateVariables", "{\"name\":\"John\",\"date\":\"2026-04-01\"}"));
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
        when(mockClient.sendTemplate(any(), any(), any(), any(), any(), any(), any(), anyBoolean()))
                .thenReturn(new SendTemplateResult(null, "Welcome John", true, ""));

        connector.executeBusinessLogic();

        assertThat(getOutput(connector, "success")).isEqualTo(true);
        assertThat(getOutput(connector, "renderedSubject")).isEqualTo("Welcome John");
    }

    @Test
    void shouldFailValidationWhenTemplateHtmlMissing() {
        connector.setInputParameters(Map.of(
                "tenantId", "t", "clientId", "c", "clientSecret", "s", "senderUserId", "u",
                "toRecipients", "[{}]", "emailSubject", "S",
                "templateVariables", "{}"));

        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class);
    }

    @Test
    void shouldFailValidationWhenTemplateVariablesMissing() {
        connector.setInputParameters(Map.of(
                "tenantId", "t", "clientId", "c", "clientSecret", "s", "senderUserId", "u",
                "toRecipients", "[{}]", "emailSubject", "S",
                "templateHtml", "<p>Hello</p>"));

        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class);
    }

    @Test
    void shouldSetErrorOutputsOnFailure() throws Exception {
        setMandatoryInputs();
        injectMockClient();
        when(mockClient.sendTemplate(any(), any(), any(), any(), any(), any(), any(), anyBoolean()))
                .thenThrow(new MsOutlookException("Invalid JSON", 400, false));

        connector.executeBusinessLogic();
        assertThat(getOutput(connector, "success")).isEqualTo(false);
    }
}
