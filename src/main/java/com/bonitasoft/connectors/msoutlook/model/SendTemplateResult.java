package com.bonitasoft.connectors.msoutlook.model;

public record SendTemplateResult(
    String messageId,
    String renderedSubject,
    boolean success,
    String errorMessage
) {}
