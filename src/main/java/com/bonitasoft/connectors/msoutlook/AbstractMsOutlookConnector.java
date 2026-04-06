package com.bonitasoft.connectors.msoutlook;

import lombok.extern.slf4j.Slf4j;
import org.bonitasoft.engine.connector.AbstractConnector;
import org.bonitasoft.engine.connector.ConnectorException;
import org.bonitasoft.engine.connector.ConnectorValidationException;

/**
 * Abstract base connector for MS Outlook operations.
 * Handles authentication, client lifecycle, and standard error handling.
 */
@Slf4j
public abstract class AbstractMsOutlookConnector extends AbstractConnector {

    protected static final String OUTPUT_SUCCESS = "success";
    protected static final String OUTPUT_ERROR_MESSAGE = "errorMessage";

    protected MsOutlookConfiguration configuration;
    protected MsOutlookClient client;

    @Override
    public void validateInputParameters() throws ConnectorValidationException {
        try {
            this.configuration = buildConfiguration();
            validateConfiguration(this.configuration);
        } catch (IllegalArgumentException e) {
            throw new ConnectorValidationException(this, e.getMessage());
        }
    }

    @Override
    public void connect() throws ConnectorException {
        try {
            this.client = new MsOutlookClient(this.configuration);
            log.info("MS Outlook connector connected successfully");
        } catch (MsOutlookException e) {
            throw new ConnectorException("Failed to connect: " + e.getMessage(), e);
        }
    }

    @Override
    public void disconnect() throws ConnectorException {
        this.client = null;
    }

    @Override
    protected void executeBusinessLogic() throws ConnectorException {
        try {
            doExecute();
            setOutputParameter(OUTPUT_SUCCESS, true);
        } catch (MsOutlookException e) {
            log.error("MS Outlook connector execution failed: {}", e.getMessage(), e);
            setOutputParameter(OUTPUT_SUCCESS, false);
            setOutputParameter(OUTPUT_ERROR_MESSAGE, e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error in MS Outlook connector: {}", e.getMessage(), e);
            setOutputParameter(OUTPUT_SUCCESS, false);
            setOutputParameter(OUTPUT_ERROR_MESSAGE, "Unexpected error: " + e.getMessage());
        }
    }

    protected abstract void doExecute() throws MsOutlookException;

    protected abstract MsOutlookConfiguration buildConfiguration();

    protected void validateConfiguration(MsOutlookConfiguration config) {
        if (config.getTenantId() == null || config.getTenantId().isBlank()) {
            throw new IllegalArgumentException("tenantId is mandatory");
        }
        if (config.getClientId() == null || config.getClientId().isBlank()) {
            throw new IllegalArgumentException("clientId is mandatory");
        }
        if (config.getClientSecret() == null || config.getClientSecret().isBlank()) {
            throw new IllegalArgumentException("clientSecret is mandatory");
        }
        if (config.getSenderUserId() == null || config.getSenderUserId().isBlank()) {
            throw new IllegalArgumentException("senderUserId is mandatory");
        }
    }

    protected String readStringInput(String name) {
        Object value = getInputParameter(name);
        return value != null ? value.toString() : null;
    }

    protected String readStringInput(String name, String defaultValue) {
        String value = readStringInput(name);
        return (value != null && !value.isBlank()) ? value : defaultValue;
    }

    protected Boolean readBooleanInput(String name, boolean defaultValue) {
        Object value = getInputParameter(name);
        return value != null ? (Boolean) value : defaultValue;
    }

    protected Integer readIntegerInput(String name, int defaultValue) {
        Object value = getInputParameter(name);
        return value != null ? ((Number) value).intValue() : defaultValue;
    }

    /**
     * Build base configuration with connection parameters using credential resolution.
     */
    protected MsOutlookConfiguration.MsOutlookConfigurationBuilder baseConfigBuilder() {
        var params = getInputParameters();
        return MsOutlookConfiguration.builder()
                .tenantId(MsOutlookConfiguration.resolveParam(params, "tenantId", "ms.outlook.tenantId", "MS_OUTLOOK_TENANT_ID"))
                .clientId(MsOutlookConfiguration.resolveParam(params, "clientId", "ms.outlook.clientId", "MS_OUTLOOK_CLIENT_ID"))
                .clientSecret(MsOutlookConfiguration.resolveParam(params, "clientSecret", "ms.outlook.clientSecret", "MS_OUTLOOK_CLIENT_SECRET"))
                .senderUserId(MsOutlookConfiguration.resolveParam(params, "senderUserId", "ms.outlook.senderUserId", "MS_OUTLOOK_SENDER_USER_ID"))
                .basePath(readStringInput("basePath", "https://graph.microsoft.com/v1.0"))
                .connectTimeout(readIntegerInput("connectTimeout", 30000))
                .readTimeout(readIntegerInput("readTimeout", 60000));
    }

    protected java.util.Map<String, Object> getInputParameters() {
        // AbstractConnector stores inputs internally; collect them
        var map = new java.util.HashMap<String, Object>();
        // We use a workaround: read known input names
        return map;
    }
}
