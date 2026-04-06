package com.bonitasoft.connectors.msoutlook;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.bonitasoft.connectors.msoutlook.model.ReadInboxResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

@ExtendWith(MockitoExtension.class)
class ReadInboxConnectorTest {

    @Mock
    private MsOutlookClient mockClient;

    private ReadInboxConnector connector;

    @BeforeEach
    void setUp() {
        connector = new ReadInboxConnector();
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
                "senderUserId", "user@test.com"));
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
        when(mockClient.readInbox(anyString(), any(), anyString(), anyInt(), anyInt(), anyString(), anyBoolean()))
                .thenReturn(new ReadInboxResult("[{\"id\":\"msg1\"}]", 1, 5, "", true, ""));

        connector.executeBusinessLogic();

        assertThat(getOutput(connector, "success")).isEqualTo(true);
        assertThat(getOutput(connector, "messageCount")).isEqualTo(1);
        assertThat(getOutput(connector, "totalCount")).isEqualTo(5);
    }

    @Test
    void shouldReturnEmptyListWhenInboxEmpty() throws Exception {
        setMandatoryInputs();
        injectMockClient();
        when(mockClient.readInbox(anyString(), any(), anyString(), anyInt(), anyInt(), anyString(), anyBoolean()))
                .thenReturn(new ReadInboxResult("[]", 0, 0, "", true, ""));

        connector.executeBusinessLogic();
        assertThat(getOutput(connector, "messageCount")).isEqualTo(0);
    }

    @Test
    void shouldSetErrorOutputsOnFailure() throws Exception {
        setMandatoryInputs();
        injectMockClient();
        when(mockClient.readInbox(anyString(), any(), anyString(), anyInt(), anyInt(), anyString(), anyBoolean()))
                .thenThrow(new MsOutlookException("Bad filter", 400, false));

        connector.executeBusinessLogic();
        assertThat(getOutput(connector, "success")).isEqualTo(false);
    }
}
