package com.bonitasoft.connectors.msoutlook;

import com.bonitasoft.connectors.msoutlook.model.GetEmailResult;
import lombok.extern.slf4j.Slf4j;

/**
 * Retrieve a specific email by its message ID, with optional attachment content.
 */
@Slf4j
public class GetEmailConnector extends AbstractMsOutlookConnector {

    static final String INPUT_MESSAGE_ID = "messageId";
    static final String INPUT_INCLUDE_ATTACHMENTS = "includeAttachments";

    static final String OUTPUT_MESSAGE_ID = "messageId";
    static final String OUTPUT_SUBJECT = "subject";
    static final String OUTPUT_FROM_EMAIL = "fromEmail";
    static final String OUTPUT_FROM_NAME = "fromName";
    static final String OUTPUT_TO_RECIPIENTS = "toRecipients";
    static final String OUTPUT_CC_RECIPIENTS = "ccRecipients";
    static final String OUTPUT_RECEIVED_DATE_TIME = "receivedDateTime";
    static final String OUTPUT_BODY_CONTENT = "bodyContent";
    static final String OUTPUT_BODY_CONTENT_TYPE = "bodyContentType";
    static final String OUTPUT_IS_READ = "isRead";
    static final String OUTPUT_HAS_ATTACHMENTS = "hasAttachments";
    static final String OUTPUT_ATTACHMENTS = "attachments";
    static final String OUTPUT_IMPORTANCE = "importance";
    static final String OUTPUT_CONVERSATION_ID = "conversationId";
    static final String OUTPUT_INTERNET_MESSAGE_ID = "internetMessageId";

    @Override
    protected MsOutlookConfiguration buildConfiguration() {
        return baseConfigBuilder().build();
    }

    @Override
    protected void validateConfiguration(MsOutlookConfiguration config) {
        super.validateConfiguration(config);
        String messageId = readStringInput(INPUT_MESSAGE_ID);
        if (messageId == null || messageId.isBlank()) {
            throw new IllegalArgumentException("messageId is mandatory");
        }
    }

    @Override
    protected void doExecute() throws MsOutlookException {
        log.info("Executing Get Email connector");

        GetEmailResult result = client.getEmail(
                readStringInput(INPUT_MESSAGE_ID),
                readBooleanInput(INPUT_INCLUDE_ATTACHMENTS, false));

        setOutputParameter(OUTPUT_MESSAGE_ID, result.messageId());
        setOutputParameter(OUTPUT_SUBJECT, result.subject());
        setOutputParameter(OUTPUT_FROM_EMAIL, result.fromEmail());
        setOutputParameter(OUTPUT_FROM_NAME, result.fromName());
        setOutputParameter(OUTPUT_TO_RECIPIENTS, result.toRecipients());
        setOutputParameter(OUTPUT_CC_RECIPIENTS, result.ccRecipients());
        setOutputParameter(OUTPUT_RECEIVED_DATE_TIME, result.receivedDateTime());
        setOutputParameter(OUTPUT_BODY_CONTENT, result.bodyContent());
        setOutputParameter(OUTPUT_BODY_CONTENT_TYPE, result.bodyContentType());
        setOutputParameter(OUTPUT_IS_READ, result.isRead());
        setOutputParameter(OUTPUT_HAS_ATTACHMENTS, result.hasAttachments());
        setOutputParameter(OUTPUT_ATTACHMENTS, result.attachments());
        setOutputParameter(OUTPUT_IMPORTANCE, result.importance());
        setOutputParameter(OUTPUT_CONVERSATION_ID, result.conversationId());
        setOutputParameter(OUTPUT_INTERNET_MESSAGE_ID, result.internetMessageId());
        log.info("Get Email connector executed successfully for message: {}", result.subject());
    }
}
