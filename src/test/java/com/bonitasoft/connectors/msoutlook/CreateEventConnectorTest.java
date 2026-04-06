package com.bonitasoft.connectors.msoutlook;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.bonitasoft.connectors.msoutlook.model.CreateEventResult;
import org.bonitasoft.engine.connector.ConnectorValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

@ExtendWith(MockitoExtension.class)
class CreateEventConnectorTest {

    @Mock
    private MsOutlookClient mockClient;

    private CreateEventConnector connector;

    @BeforeEach
    void setUp() {
        connector = new CreateEventConnector();
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
                "eventSubject", "Team Meeting",
                "startDateTime", "2026-04-01T09:00:00",
                "endDateTime", "2026-04-01T10:00:00"));
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
        when(mockClient.createEvent(any(), any(), any(), any(), any(), any(), any(),
                anyBoolean(), anyInt(), anyBoolean(), any(), any(), any()))
                .thenReturn(new CreateEventResult("evt1", "ical1", "https://outlook.live.com/owa/?path=/calendar",
                        "", "2026-04-01T08:00:00Z", true, ""));

        connector.executeBusinessLogic();

        assertThat(getOutput(connector, "success")).isEqualTo(true);
        assertThat(getOutput(connector, "eventId")).isEqualTo("evt1");
    }

    @Test
    void shouldFailValidationWhenEventSubjectMissing() {
        connector.setInputParameters(Map.of(
                "tenantId", "t", "clientId", "c", "clientSecret", "s", "senderUserId", "u",
                "startDateTime", "2026-04-01T09:00:00", "endDateTime", "2026-04-01T10:00:00"));

        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class);
    }

    @Test
    void shouldFailValidationWhenStartDateTimeMissing() {
        connector.setInputParameters(Map.of(
                "tenantId", "t", "clientId", "c", "clientSecret", "s", "senderUserId", "u",
                "eventSubject", "Meeting", "endDateTime", "2026-04-01T10:00:00"));

        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class);
    }

    @Test
    void shouldSetErrorOutputsOnFailure() throws Exception {
        setMandatoryInputs();
        injectMockClient();
        when(mockClient.createEvent(any(), any(), any(), any(), any(), any(), any(),
                anyBoolean(), anyInt(), anyBoolean(), any(), any(), any()))
                .thenThrow(new MsOutlookException("Calendar unavailable", 503, true));

        connector.executeBusinessLogic();
        assertThat(getOutput(connector, "success")).isEqualTo(false);
    }
}
