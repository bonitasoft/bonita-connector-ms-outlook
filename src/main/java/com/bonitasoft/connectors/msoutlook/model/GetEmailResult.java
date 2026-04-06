package com.bonitasoft.connectors.msoutlook.model;

public record GetEmailResult(
    String messageId,
    String subject,
    String fromEmail,
    String fromName,
    String toRecipients,
    String ccRecipients,
    String receivedDateTime,
    String bodyContent,
    String bodyContentType,
    boolean isRead,
    boolean hasAttachments,
    String attachments,
    String importance,
    String conversationId,
    String internetMessageId,
    boolean success,
    String errorMessage
) {}
