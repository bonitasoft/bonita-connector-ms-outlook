package com.bonitasoft.connectors.msoutlook;

import com.bonitasoft.connectors.msoutlook.model.SendEmailResult;
import lombok.extern.slf4j.Slf4j;

/**
 * Send a rich HTML email via Microsoft Graph API.
 */
@Slf4j
public class SendEmailConnector extends AbstractMsOutlookConnector {

    static final String INPUT_TO_RECIPIENTS = "toRecipients";
    static final String INPUT_CC_RECIPIENTS = "ccRecipients";
    static final String INPUT_BCC_RECIPIENTS = "bccRecipients";
    static final String INPUT_EMAIL_SUBJECT = "emailSubject";
    static final String INPUT_EMAIL_BODY = "emailBody";
    static final String INPUT_BODY_CONTENT_TYPE = "bodyContentType";
    static final String INPUT_IMPORTANCE = "importance";
    static final String INPUT_ATTACHMENTS = "attachments";
    static final String INPUT_SAVE_TO_SENT_ITEMS = "saveToSentItems";
    static final String INPUT_REPLY_TO = "replyTo";

    static final String OUTPUT_MESSAGE_ID = "messageId";

    @Override
    protected MsOutlookConfiguration buildConfiguration() {
        return baseConfigBuilder().build();
    }

    @Override
    protected void validateConfiguration(MsOutlookConfiguration config) {
        super.validateConfiguration(config);
        String toRecipients = readStringInput(INPUT_TO_RECIPIENTS);
        if (toRecipients == null || toRecipients.isBlank()) {
            throw new IllegalArgumentException("toRecipients is mandatory");
        }
        String emailSubject = readStringInput(INPUT_EMAIL_SUBJECT);
        if (emailSubject == null || emailSubject.isBlank()) {
            throw new IllegalArgumentException("emailSubject is mandatory");
        }
        String emailBody = readStringInput(INPUT_EMAIL_BODY);
        if (emailBody == null || emailBody.isBlank()) {
            throw new IllegalArgumentException("emailBody is mandatory");
        }
    }

    @Override
    protected void doExecute() throws MsOutlookException {
        log.info("Executing Send Email connector");

        SendEmailResult result = client.sendEmail(
                readStringInput(INPUT_TO_RECIPIENTS),
                readStringInput(INPUT_CC_RECIPIENTS),
                readStringInput(INPUT_BCC_RECIPIENTS),
                readStringInput(INPUT_EMAIL_SUBJECT),
                readStringInput(INPUT_EMAIL_BODY),
                readStringInput(INPUT_BODY_CONTENT_TYPE, "html"),
                readStringInput(INPUT_IMPORTANCE, "normal"),
                readStringInput(INPUT_ATTACHMENTS),
                readBooleanInput(INPUT_SAVE_TO_SENT_ITEMS, true),
                readStringInput(INPUT_REPLY_TO));

        setOutputParameter(OUTPUT_MESSAGE_ID, result.messageId());
        log.info("Send Email connector executed successfully");
    }
}
