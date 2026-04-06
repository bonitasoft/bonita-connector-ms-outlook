package com.bonitasoft.connectors.msoutlook;

import com.bonitasoft.connectors.msoutlook.model.CreateEventResult;
import lombok.extern.slf4j.Slf4j;

/**
 * Create a calendar event in a user's Outlook calendar.
 */
@Slf4j
public class CreateEventConnector extends AbstractMsOutlookConnector {

    static final String INPUT_EVENT_SUBJECT = "eventSubject";
    static final String INPUT_EVENT_BODY = "eventBody";
    static final String INPUT_START_DATE_TIME = "startDateTime";
    static final String INPUT_END_DATE_TIME = "endDateTime";
    static final String INPUT_TIME_ZONE = "timeZone";
    static final String INPUT_ATTENDEES = "attendees";
    static final String INPUT_LOCATION = "location";
    static final String INPUT_IS_ONLINE_MEETING = "isOnlineMeeting";
    static final String INPUT_REMINDER_MINUTES = "reminderMinutes";
    static final String INPUT_IS_ALL_DAY = "isAllDay";
    static final String INPUT_RECURRENCE = "recurrence";
    static final String INPUT_CATEGORIES = "categories";
    static final String INPUT_IMPORTANCE = "importance";

    static final String OUTPUT_EVENT_ID = "eventId";
    static final String OUTPUT_ICAL_UID = "iCalUId";
    static final String OUTPUT_WEB_LINK = "webLink";
    static final String OUTPUT_ONLINE_MEETING_URL = "onlineMeetingUrl";
    static final String OUTPUT_CREATED_DATE_TIME = "createdDateTime";

    @Override
    protected MsOutlookConfiguration buildConfiguration() {
        return baseConfigBuilder().build();
    }

    @Override
    protected void validateConfiguration(MsOutlookConfiguration config) {
        super.validateConfiguration(config);
        if (readStringInput(INPUT_EVENT_SUBJECT) == null || readStringInput(INPUT_EVENT_SUBJECT).isBlank()) {
            throw new IllegalArgumentException("eventSubject is mandatory");
        }
        if (readStringInput(INPUT_START_DATE_TIME) == null || readStringInput(INPUT_START_DATE_TIME).isBlank()) {
            throw new IllegalArgumentException("startDateTime is mandatory");
        }
        if (readStringInput(INPUT_END_DATE_TIME) == null || readStringInput(INPUT_END_DATE_TIME).isBlank()) {
            throw new IllegalArgumentException("endDateTime is mandatory");
        }
    }

    @Override
    protected void doExecute() throws MsOutlookException {
        log.info("Executing Create Event connector");

        CreateEventResult result = client.createEvent(
                readStringInput(INPUT_EVENT_SUBJECT),
                readStringInput(INPUT_EVENT_BODY),
                readStringInput(INPUT_START_DATE_TIME),
                readStringInput(INPUT_END_DATE_TIME),
                readStringInput(INPUT_TIME_ZONE, "Europe/Madrid"),
                readStringInput(INPUT_ATTENDEES),
                readStringInput(INPUT_LOCATION),
                readBooleanInput(INPUT_IS_ONLINE_MEETING, false),
                readIntegerInput(INPUT_REMINDER_MINUTES, 15),
                readBooleanInput(INPUT_IS_ALL_DAY, false),
                readStringInput(INPUT_RECURRENCE),
                readStringInput(INPUT_CATEGORIES),
                readStringInput(INPUT_IMPORTANCE, "normal"));

        setOutputParameter(OUTPUT_EVENT_ID, result.eventId());
        setOutputParameter(OUTPUT_ICAL_UID, result.iCalUId());
        setOutputParameter(OUTPUT_WEB_LINK, result.webLink());
        setOutputParameter(OUTPUT_ONLINE_MEETING_URL, result.onlineMeetingUrl());
        setOutputParameter(OUTPUT_CREATED_DATE_TIME, result.createdDateTime());
        log.info("Create Event connector executed successfully, eventId: {}", result.eventId());
    }
}
