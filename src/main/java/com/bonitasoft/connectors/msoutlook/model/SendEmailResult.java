package com.bonitasoft.connectors.msoutlook.model;

public record SendEmailResult(
    String messageId,
    boolean success,
    String errorMessage
) {}
