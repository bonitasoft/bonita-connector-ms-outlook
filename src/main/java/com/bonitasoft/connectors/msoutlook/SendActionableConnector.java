package com.bonitasoft.connectors.msoutlook;

import com.bonitasoft.connectors.msoutlook.model.SendActionableResult;
import lombok.extern.slf4j.Slf4j;

/**
 * Send an Actionable Message (Adaptive Card) with embedded action buttons in Outlook.
 */
@Slf4j
public class SendActionableConnector extends AbstractMsOutlookConnector {

    static final String INPUT_TO_RECIPIENTS = "toRecipients";
    static final String INPUT_CC_RECIPIENTS = "ccRecipients";
    static final String INPUT_EMAIL_SUBJECT = "emailSubject";
    static final String INPUT_EMAIL_BODY_FALLBACK = "emailBodyFallback";
    static final String INPUT_CARD_TITLE = "cardTitle";
    static final String INPUT_CARD_BODY = "cardBody";
    static final String INPUT_ACTIONS = "actions";
    static final String INPUT_CALLBACK_URL = "callbackUrl";
    static final String INPUT_ORIGINATOR = "originator";
    static final String INPUT_IMPORTANCE = "importance";

    static final String OUTPUT_MESSAGE_ID = "messageId";

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
        if (readStringInput(INPUT_EMAIL_BODY_FALLBACK) == null || readStringInput(INPUT_EMAIL_BODY_FALLBACK).isBlank()) {
            throw new IllegalArgumentException("emailBodyFallback is mandatory");
        }
        if (readStringInput(INPUT_CARD_TITLE) == null || readStringInput(INPUT_CARD_TITLE).isBlank()) {
            throw new IllegalArgumentException("cardTitle is mandatory");
        }
        if (readStringInput(INPUT_CARD_BODY) == null || readStringInput(INPUT_CARD_BODY).isBlank()) {
            throw new IllegalArgumentException("cardBody is mandatory");
        }
        if (readStringInput(INPUT_ACTIONS) == null || readStringInput(INPUT_ACTIONS).isBlank()) {
            throw new IllegalArgumentException("actions is mandatory");
        }
        if (readStringInput(INPUT_CALLBACK_URL) == null || readStringInput(INPUT_CALLBACK_URL).isBlank()) {
            throw new IllegalArgumentException("callbackUrl is mandatory");
        }
        if (readStringInput(INPUT_ORIGINATOR) == null || readStringInput(INPUT_ORIGINATOR).isBlank()) {
            throw new IllegalArgumentException("originator is mandatory");
        }
    }

    @Override
    protected void doExecute() throws MsOutlookException {
        log.info("Executing Send Actionable Message connector");

        SendActionableResult result = client.sendActionable(
                readStringInput(INPUT_TO_RECIPIENTS),
                readStringInput(INPUT_CC_RECIPIENTS),
                readStringInput(INPUT_EMAIL_SUBJECT),
                readStringInput(INPUT_EMAIL_BODY_FALLBACK),
                readStringInput(INPUT_CARD_TITLE),
                readStringInput(INPUT_CARD_BODY),
                readStringInput(INPUT_ACTIONS),
                readStringInput(INPUT_CALLBACK_URL),
                readStringInput(INPUT_ORIGINATOR),
                readStringInput(INPUT_IMPORTANCE, "normal"));

        setOutputParameter(OUTPUT_MESSAGE_ID, result.messageId());
        log.info("Send Actionable Message connector executed successfully");
    }
}
