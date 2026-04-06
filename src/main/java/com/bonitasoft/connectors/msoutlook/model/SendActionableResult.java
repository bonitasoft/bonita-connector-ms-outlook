package com.bonitasoft.connectors.msoutlook.model;

public record SendActionableResult(
    String messageId,
    boolean success,
    String errorMessage
) {}
