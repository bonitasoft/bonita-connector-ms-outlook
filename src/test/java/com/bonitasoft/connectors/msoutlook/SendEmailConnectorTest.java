package com.bonitasoft.connectors.msoutlook;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.bonitasoft.connectors.msoutlook.model.SendEmailResult;
import org.bonitasoft.engine.connector.ConnectorValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

@ExtendWith(MockitoExtension.class)
class SendEmailConnectorTest {

    @Mock
    private MsOutlookClient mockClient;

    private SendEmailConnector connector;

    @BeforeEach
    void setUp() {
        connector = new SendEmailConnector();
    }

    private void setMandatoryInputs() {
        connector.setInputParameters(Map.of(
                "tenantId", "test-tenant-id",
                "clientId", "test-client-id",
                "clientSecret", "test-secret",
                "senderUserId", "user@test.com",
                "toRecipients", "[{\"email\":\"john@test.com\",\"name\":\"John\"}]",
                "emailSubject", "Test Subject",
                "emailBody", "<p>Test Body</p>"));
    }

    private void injectMockClient() throws Exception {
        var clientField = AbstractMsOutlookConnector.class.getDeclaredField("client");
        clientField.setAccessible(true);
        clientField.set(connector, mockClient);
    }

    @SuppressWarnings("unchecked")
    private static Object getOutput(AbstractMsOutlookConnector c, String name) throws Exception {
        var method = org.bonitasoft.engine.connector.AbstractConnector.class.getDeclaredMethod("getOutputParameters");
        method.setAccessible(true);
        var map = (Map<String, Object>) method.invoke(c);
        return map.get(name);
    }

    @Test
    void shouldExecuteSuccessfully() throws Exception {
        setMandatoryInputs();
        injectMockClient();
        when(mockClient.sendEmail(anyString(), any(), any(), anyString(), anyString(),
                anyString(), anyString(), any(), anyBoolean(), any()))
                .thenReturn(new SendEmailResult("msg-123", true, ""));

        connector.executeBusinessLogic();

        assertThat(getOutput(connector, "success")).isEqualTo(true);
        assertThat(getOutput(connector, "messageId")).isEqualTo("msg-123");
    }

    @Test
    void shouldFailValidationWhenTenantIdMissing() {
        connector.setInputParameters(Map.of(
                "clientId", "test-client-id",
                "clientSecret", "test-secret",
                "senderUserId", "user@test.com",
                "toRecipients", "[{\"email\":\"john@test.com\"}]",
                "emailSubject", "Test",
                "emailBody", "Body"));

        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class);
    }

    @Test
    void shouldFailValidationWhenClientIdMissing() {
        connector.setInputParameters(Map.of(
                "tenantId", "test-tenant",
                "clientSecret", "test-secret",
                "senderUserId", "user@test.com",
                "toRecipients", "[{\"email\":\"john@test.com\"}]",
                "emailSubject", "Test",
                "emailBody", "Body"));

        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class);
    }

    @Test
    void shouldFailValidationWhenClientSecretMissing() {
        connector.setInputParameters(Map.of(
                "tenantId", "test-tenant",
                "clientId", "test-client-id",
                "senderUserId", "user@test.com",
                "toRecipients", "[{\"email\":\"john@test.com\"}]",
                "emailSubject", "Test",
                "emailBody", "Body"));

        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class);
    }

    @Test
    void shouldFailValidationWhenSenderUserIdMissing() {
        connector.setInputParameters(Map.of(
                "tenantId", "test-tenant",
                "clientId", "test-client-id",
                "clientSecret", "test-secret",
                "toRecipients", "[{\"email\":\"john@test.com\"}]",
                "emailSubject", "Test",
                "emailBody", "Body"));

        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class);
    }

    @Test
    void shouldFailValidationWhenToRecipientsMissing() {
        connector.setInputParameters(Map.of(
                "tenantId", "test-tenant",
                "clientId", "test-client-id",
                "clientSecret", "test-secret",
                "senderUserId", "user@test.com",
                "emailSubject", "Test",
                "emailBody", "Body"));

        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class);
    }

    @Test
    void shouldFailValidationWhenEmailSubjectMissing() {
        connector.setInputParameters(Map.of(
                "tenantId", "test-tenant",
                "clientId", "test-client-id",
                "clientSecret", "test-secret",
                "senderUserId", "user@test.com",
                "toRecipients", "[{\"email\":\"john@test.com\"}]",
                "emailBody", "Body"));

        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class);
    }

    @Test
    void shouldFailValidationWhenEmailBodyMissing() {
        connector.setInputParameters(Map.of(
                "tenantId", "test-tenant",
                "clientId", "test-client-id",
                "clientSecret", "test-secret",
                "senderUserId", "user@test.com",
                "toRecipients", "[{\"email\":\"john@test.com\"}]",
                "emailSubject", "Test"));

        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class);
    }

    @Test
    void shouldSetErrorOutputsOnFailure() throws Exception {
        setMandatoryInputs();
        injectMockClient();
        when(mockClient.sendEmail(anyString(), any(), any(), anyString(), anyString(),
                anyString(), anyString(), any(), anyBoolean(), any()))
                .thenThrow(new MsOutlookException("Graph API error: BadRequest", 400, false));

        connector.executeBusinessLogic();

        assertThat(getOutput(connector, "success")).isEqualTo(false);
        assertThat((String) getOutput(connector, "errorMessage")).contains("BadRequest");
    }

    @Test
    void shouldHandleUnexpectedException() throws Exception {
        setMandatoryInputs();
        injectMockClient();
        when(mockClient.sendEmail(anyString(), any(), any(), anyString(), anyString(),
                anyString(), anyString(), any(), anyBoolean(), any()))
                .thenThrow(new RuntimeException("Network failure"));

        connector.executeBusinessLogic();

        assertThat(getOutput(connector, "success")).isEqualTo(false);
        assertThat((String) getOutput(connector, "errorMessage")).contains("Network failure");
    }

    @Test
    void shouldApplyDefaultsForOptionalInputs() throws Exception {
        setMandatoryInputs();
        injectMockClient();
        when(mockClient.sendEmail(anyString(), isNull(), isNull(), anyString(), anyString(),
                eq("html"), eq("normal"), isNull(), eq(true), isNull()))
                .thenReturn(new SendEmailResult(null, true, ""));

        connector.executeBusinessLogic();

        assertThat(getOutput(connector, "success")).isEqualTo(true);
    }
}
