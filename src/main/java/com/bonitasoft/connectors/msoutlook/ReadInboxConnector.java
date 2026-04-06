package com.bonitasoft.connectors.msoutlook;

import com.bonitasoft.connectors.msoutlook.model.ReadInboxResult;
import lombok.extern.slf4j.Slf4j;

/**
 * Read emails from a user's inbox or a specific mail folder.
 */
@Slf4j
public class ReadInboxConnector extends AbstractMsOutlookConnector {

    static final String INPUT_FOLDER_ID = "folderId";
    static final String INPUT_FILTER = "filter";
    static final String INPUT_ORDER_BY = "orderBy";
    static final String INPUT_TOP = "top";
    static final String INPUT_SKIP = "skip";
    static final String INPUT_SELECT = "select";
    static final String INPUT_MARK_AS_READ = "markAsRead";

    static final String OUTPUT_MESSAGES = "messages";
    static final String OUTPUT_MESSAGE_COUNT = "messageCount";
    static final String OUTPUT_TOTAL_COUNT = "totalCount";
    static final String OUTPUT_NEXT_LINK = "nextLink";

    @Override
    protected MsOutlookConfiguration buildConfiguration() {
        return baseConfigBuilder().build();
    }

    @Override
    protected void doExecute() throws MsOutlookException {
        log.info("Executing Read Inbox connector");

        ReadInboxResult result = client.readInbox(
                readStringInput(INPUT_FOLDER_ID, "inbox"),
                readStringInput(INPUT_FILTER),
                readStringInput(INPUT_ORDER_BY, "receivedDateTime desc"),
                readIntegerInput(INPUT_TOP, 10),
                readIntegerInput(INPUT_SKIP, 0),
                readStringInput(INPUT_SELECT, "id,subject,from,receivedDateTime,bodyPreview,isRead,hasAttachments"),
                readBooleanInput(INPUT_MARK_AS_READ, false));

        setOutputParameter(OUTPUT_MESSAGES, result.messages());
        setOutputParameter(OUTPUT_MESSAGE_COUNT, result.messageCount());
        setOutputParameter(OUTPUT_TOTAL_COUNT, result.totalCount());
        setOutputParameter(OUTPUT_NEXT_LINK, result.nextLink());
        log.info("Read Inbox connector executed successfully, {} messages returned", result.messageCount());
    }
}
