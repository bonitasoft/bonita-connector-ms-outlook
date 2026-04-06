package com.bonitasoft.connectors.msoutlook.model;

public record CreateEventResult(
    String eventId,
    String iCalUId,
    String webLink,
    String onlineMeetingUrl,
    String createdDateTime,
    boolean success,
    String errorMessage
) {}
