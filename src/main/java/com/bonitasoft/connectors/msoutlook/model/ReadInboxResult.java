package com.bonitasoft.connectors.msoutlook.model;

public record ReadInboxResult(
    String messages,
    int messageCount,
    int totalCount,
    String nextLink,
    boolean success,
    String errorMessage
) {}
