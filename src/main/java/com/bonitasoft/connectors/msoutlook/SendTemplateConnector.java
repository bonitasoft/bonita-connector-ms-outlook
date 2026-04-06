package com.bonitasoft.connectors.msoutlook;

import com.bonitasoft.connectors.msoutlook.model.SendTemplateResult;
import lombok.extern.slf4j.Slf4j;

/**
 * Send a template-based email with variable substitution.
 */
@Slf4j
public class SendTemplateConnector extends AbstractMsOutlookConnector {

    static final String INPUT_TO_RECIPIENTS = "toRecipients";
    static final String INPUT_CC_RECIPIENTS = "ccRecipients";
    static final String INPUT_EMAIL_SUBJECT = "emailSubject";
    static final String INPUT_TEMPLATE_HTML = "templateHtml";
    static final String INPUT_TEMPLATE_VARIABLES = "templateVariables";
    static final String INPUT_IMPORTANCE = "importance";
    static final String INPUT_ATTACHMENTS = "attachments";
    static final String INPUT_SAVE_TO_SENT_ITEMS = "saveToSentItems";

    static final String OUTPUT_MESSAGE_ID = "messageId";
    static final String OUTPUT_RENDERED_SUBJECT = "renderedSubject";

    @Override
    protected MsOutlookConfiguration buildConfiguration() {
        return baseConfigBuilder().build();
    }

    @Override
    protected void validateConfiguration(MsOutlookConfiguration config) {
        super.validateConfiguration(config);
        if (readStringInput(INPUT_TO_RECIPIENTS) == null || readStringInput(INPUT_TO_RECIPIENTS).isBlank()) {
            throw new IllegalArgumentException("toRecipients is mandatory");
        }
        if (readStringInput(INPUT_EMAIL_SUBJECT) == null || readStringInput(INPUT_EMAIL_SUBJECT).isBlank()) {
            throw new IllegalArgumentException("emailSubject is mandatory");
        }
        if (readStringInput(INPUT_TEMPLATE_HTML) == null || readStringInput(INPUT_TEMPLATE_HTML).isBlank()) {
            throw new IllegalArgumentException("templateHtml is mandatory");
        }
        if (readStringInput(INPUT_TEMPLATE_VARIABLES) == null || readStringInput(INPUT_TEMPLATE_VARIABLES).isBlank()) {
            throw new IllegalArgumentException("templateVariables is mandatory");
        }
    }

    @Override
    protected void doExecute() throws MsOutlookException {
        log.info("Executing Send Template Email connector");

        SendTemplateResult result = client.sendTemplate(
                readStringInput(INPUT_TO_RECIPIENTS),
                readStringInput(INPUT_CC_RECIPIENTS),
                readStringInput(INPUT_EMAIL_SUBJECT),
                readStringInput(INPUT_TEMPLATE_HTML),
                readStringInput(INPUT_TEMPLATE_VARIABLES),
                readStringInput(INPUT_IMPORTANCE, "normal"),
                readStringInput(INPUT_ATTACHMENTS),
                readBooleanInput(INPUT_SAVE_TO_SENT_ITEMS, true));

        setOutputParameter(OUTPUT_MESSAGE_ID, result.messageId());
        setOutputParameter(OUTPUT_RENDERED_SUBJECT, result.renderedSubject());
        log.info("Send Template Email connector executed successfully");
    }
}
