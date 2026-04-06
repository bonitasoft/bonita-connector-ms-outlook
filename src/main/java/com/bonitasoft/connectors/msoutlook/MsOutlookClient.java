package com.bonitasoft.connectors.msoutlook;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.bonitasoft.connectors.msoutlook.model.*;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Microsoft Graph API client for Outlook operations.
 * Uses java.net.http.HttpClient and MSAL4J-style token acquisition.
 */
@Slf4j
public class MsOutlookClient {

    private static final String TOKEN_URL_TEMPLATE = "https://login.microsoftonline.com/%s/oauth2/v2.0/token";
    private static final String GRAPH_SCOPE = "https://graph.microsoft.com/.default";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final MsOutlookConfiguration configuration;
    private final RetryPolicy retryPolicy;
    private final HttpClient httpClient;
    private String accessToken;

    public MsOutlookClient(MsOutlookConfiguration configuration) throws MsOutlookException {
        this.configuration = configuration;
        this.retryPolicy = new RetryPolicy(configuration.getMaxRetries());
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(configuration.getConnectTimeout()))
                .build();
        authenticate();
        log.debug("MsOutlookClient initialized");
    }

    /**
     * Acquire OAuth2 access token using Client Credentials flow.
     */
    void authenticate() throws MsOutlookException {
        try {
            String tokenUrl = String.format(TOKEN_URL_TEMPLATE, configuration.getTenantId());
            String body = String.format("grant_type=client_credentials&client_id=%s&client_secret=%s&scope=%s",
                    URLEncoder.encode(configuration.getClientId(), StandardCharsets.UTF_8),
                    URLEncoder.encode(configuration.getClientSecret(), StandardCharsets.UTF_8),
                    URLEncoder.encode(GRAPH_SCOPE, StandardCharsets.UTF_8));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(tokenUrl))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .timeout(Duration.ofMillis(configuration.getReadTimeout()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new MsOutlookException(
                        "Authentication failed -- verify client credentials and API permissions. Status: " + response.statusCode(),
                        response.statusCode(), false);
            }

            JsonNode tokenJson = MAPPER.readTree(response.body());
            this.accessToken = tokenJson.get("access_token").asText();
            log.info("OAuth2 token acquired successfully");
        } catch (MsOutlookException e) {
            throw e;
        } catch (Exception e) {
            throw new MsOutlookException("Failed to acquire OAuth2 token: " + e.getMessage(), e);
        }
    }

    // === Send Email ===
    public SendEmailResult sendEmail(String toRecipients, String ccRecipients, String bccRecipients,
                                     String emailSubject, String emailBody, String bodyContentType,
                                     String importance, String attachments, boolean saveToSentItems,
                                     String replyTo) throws MsOutlookException {
        return retryPolicy.execute(() -> {
            ObjectNode payload = buildSendMailPayload(toRecipients, ccRecipients, bccRecipients,
                    emailSubject, emailBody, bodyContentType, importance, attachments, replyTo);
            payload.put("saveToSentItems", saveToSentItems);

            String url = String.format("%s/users/%s/sendMail",
                    configuration.getBasePath(), configuration.getSenderUserId());

            HttpResponse<String> response = executeGraphRequest("POST", url, MAPPER.writeValueAsString(payload));
            handleErrorResponse(response);

            return new SendEmailResult(null, true, "");
        });
    }

    // === Send Actionable Message ===
    public SendActionableResult sendActionable(String toRecipients, String ccRecipients,
                                                String emailSubject, String emailBodyFallback,
                                                String cardTitle, String cardBody, String actions,
                                                String callbackUrl, String originator, String importance)
            throws MsOutlookException {
        return retryPolicy.execute(() -> {
            ObjectNode message = MAPPER.createObjectNode();
            message.put("subject", emailSubject);

            // Body with fallback HTML
            ObjectNode body = MAPPER.createObjectNode();
            body.put("contentType", "html");
            body.put("content", emailBodyFallback);
            message.set("body", body);

            // Recipients
            message.set("toRecipients", buildRecipientArray(toRecipients));
            if (ccRecipients != null && !ccRecipients.isBlank()) {
                message.set("ccRecipients", buildRecipientArray(ccRecipients));
            }

            if (importance != null && !importance.isBlank()) {
                message.put("importance", importance);
            }

            // Build MessageCard as attachment
            ObjectNode card = MAPPER.createObjectNode();
            card.put("@type", "MessageCard");
            card.put("@context", "https://schema.org/extensions");
            card.put("originator", originator);
            card.put("summary", cardTitle);
            card.put("themeColor", "0076D7");
            card.put("title", cardTitle);

            // Sections from cardBody
            ObjectNode section = MAPPER.createObjectNode();
            section.put("activityTitle", cardTitle);
            if (cardBody != null && !cardBody.isBlank()) {
                section.set("facts", MAPPER.readTree(cardBody));
            }
            section.put("markdown", true);
            card.set("sections", MAPPER.createArrayNode().add(section));

            // Actions
            if (actions != null && !actions.isBlank()) {
                card.set("potentialAction", MAPPER.readTree(actions));
            }

            // Attach the card as an ItemAttachment
            ArrayNode attachments = MAPPER.createArrayNode();
            ObjectNode cardAttachment = MAPPER.createObjectNode();
            cardAttachment.put("@odata.type", "#microsoft.graph.itemAttachment");
            cardAttachment.put("name", "ActionableMessage");
            cardAttachment.put("contentType", "application/vnd.microsoft.card.adaptive");
            cardAttachment.set("item", card);

            // For sendMail, embed card JSON in the HTML body via script tag
            String cardJson = MAPPER.writeValueAsString(card);
            String enrichedBody = emailBodyFallback +
                    "<script type=\"application/adaptivecard+json\">" + cardJson + "</script>";
            body.put("content", enrichedBody);
            message.set("body", body);

            ObjectNode payload = MAPPER.createObjectNode();
            payload.set("message", message);
            payload.put("saveToSentItems", true);

            String url = String.format("%s/users/%s/sendMail",
                    configuration.getBasePath(), configuration.getSenderUserId());

            HttpResponse<String> response = executeGraphRequest("POST", url, MAPPER.writeValueAsString(payload));
            handleErrorResponse(response);

            return new SendActionableResult(null, true, "");
        });
    }

    // === Read Inbox ===
    public ReadInboxResult readInbox(String folderId, String filter, String orderBy,
                                     int top, int skip, String select, boolean markAsRead)
            throws MsOutlookException {
        return retryPolicy.execute(() -> {
            StringBuilder urlBuilder = new StringBuilder(configuration.getBasePath());
            urlBuilder.append("/users/").append(configuration.getSenderUserId());

            if (folderId != null && !folderId.isBlank() && !"inbox".equalsIgnoreCase(folderId)) {
                urlBuilder.append("/mailFolders/").append(URLEncoder.encode(folderId, StandardCharsets.UTF_8)).append("/messages");
            } else {
                urlBuilder.append("/mailFolders/inbox/messages");
            }

            List<String> queryParams = new ArrayList<>();
            queryParams.add("$top=" + top);
            if (skip > 0) {
                queryParams.add("$skip=" + skip);
            }
            if (filter != null && !filter.isBlank()) {
                queryParams.add("$filter=" + URLEncoder.encode(filter, StandardCharsets.UTF_8));
            }
            if (orderBy != null && !orderBy.isBlank()) {
                queryParams.add("$orderby=" + URLEncoder.encode(orderBy, StandardCharsets.UTF_8));
            }
            if (select != null && !select.isBlank()) {
                queryParams.add("$select=" + URLEncoder.encode(select, StandardCharsets.UTF_8));
            }
            queryParams.add("$count=true");

            urlBuilder.append("?").append(String.join("&", queryParams));

            HttpResponse<String> response = executeGraphRequest("GET", urlBuilder.toString(), null);
            handleErrorResponse(response);

            JsonNode responseJson = MAPPER.readTree(response.body());
            String messages = "[]";
            int messageCount = 0;
            int totalCount = -1;
            String nextLink = "";

            if (responseJson.has("value")) {
                messages = MAPPER.writeValueAsString(responseJson.get("value"));
                messageCount = responseJson.get("value").size();
            }
            if (responseJson.has("@odata.count")) {
                totalCount = responseJson.get("@odata.count").asInt();
            }
            if (responseJson.has("@odata.nextLink")) {
                nextLink = responseJson.get("@odata.nextLink").asText();
            }

            // Mark as read if requested
            if (markAsRead && responseJson.has("value")) {
                for (JsonNode msg : responseJson.get("value")) {
                    if (msg.has("id") && msg.has("isRead") && !msg.get("isRead").asBoolean()) {
                        try {
                            String patchUrl = String.format("%s/users/%s/messages/%s",
                                    configuration.getBasePath(), configuration.getSenderUserId(),
                                    msg.get("id").asText());
                            executeGraphRequest("PATCH", patchUrl, "{\"isRead\":true}");
                        } catch (Exception e) {
                            log.warn("Failed to mark message {} as read: {}", msg.get("id").asText(), e.getMessage());
                        }
                    }
                }
            }

            return new ReadInboxResult(messages, messageCount, totalCount, nextLink, true, "");
        });
    }

    // === Get Email ===
    public GetEmailResult getEmail(String messageId, boolean includeAttachments) throws MsOutlookException {
        return retryPolicy.execute(() -> {
            String url = String.format("%s/users/%s/messages/%s",
                    configuration.getBasePath(), configuration.getSenderUserId(), messageId);
            if (includeAttachments) {
                url += "?$expand=attachments";
            }

            HttpResponse<String> response = executeGraphRequest("GET", url, null);
            handleErrorResponse(response);

            JsonNode msg = MAPPER.readTree(response.body());

            String subject = getTextOrEmpty(msg, "subject");
            String fromEmail = "";
            String fromName = "";
            if (msg.has("from") && msg.get("from").has("emailAddress")) {
                JsonNode fromAddr = msg.get("from").get("emailAddress");
                fromEmail = getTextOrEmpty(fromAddr, "address");
                fromName = getTextOrEmpty(fromAddr, "name");
            }

            String toRecipientsJson = msg.has("toRecipients") ? MAPPER.writeValueAsString(msg.get("toRecipients")) : "[]";
            String ccRecipientsJson = msg.has("ccRecipients") ? MAPPER.writeValueAsString(msg.get("ccRecipients")) : "[]";
            String receivedDateTime = getTextOrEmpty(msg, "receivedDateTime");
            String bodyContent = "";
            String bodyContentType = "";
            if (msg.has("body")) {
                bodyContent = getTextOrEmpty(msg.get("body"), "content");
                bodyContentType = getTextOrEmpty(msg.get("body"), "contentType");
            }
            boolean isRead = msg.has("isRead") && msg.get("isRead").asBoolean();
            boolean hasAttachments = msg.has("hasAttachments") && msg.get("hasAttachments").asBoolean();
            String attachmentsJson = "[]";
            if (includeAttachments && msg.has("attachments")) {
                attachmentsJson = MAPPER.writeValueAsString(msg.get("attachments"));
            }
            String importanceVal = getTextOrEmpty(msg, "importance");
            String conversationId = getTextOrEmpty(msg, "conversationId");
            String internetMessageId = getTextOrEmpty(msg, "internetMessageId");

            return new GetEmailResult(messageId, subject, fromEmail, fromName, toRecipientsJson,
                    ccRecipientsJson, receivedDateTime, bodyContent, bodyContentType, isRead,
                    hasAttachments, attachmentsJson, importanceVal, conversationId, internetMessageId,
                    true, "");
        });
    }

    // === Create Event ===
    public CreateEventResult createEvent(String eventSubject, String eventBody,
                                          String startDateTime, String endDateTime, String timeZone,
                                          String attendees, String location, boolean isOnlineMeeting,
                                          int reminderMinutes, boolean isAllDay, String recurrence,
                                          String categories, String importance) throws MsOutlookException {
        return retryPolicy.execute(() -> {
            ObjectNode event = MAPPER.createObjectNode();
            event.put("subject", eventSubject);

            if (eventBody != null && !eventBody.isBlank()) {
                ObjectNode body = MAPPER.createObjectNode();
                body.put("contentType", "html");
                body.put("content", eventBody);
                event.set("body", body);
            }

            // Start
            ObjectNode start = MAPPER.createObjectNode();
            start.put("dateTime", startDateTime);
            start.put("timeZone", timeZone != null ? timeZone : "Europe/Madrid");
            event.set("start", start);

            // End
            ObjectNode end = MAPPER.createObjectNode();
            end.put("dateTime", endDateTime);
            end.put("timeZone", timeZone != null ? timeZone : "Europe/Madrid");
            event.set("end", end);

            event.put("isAllDay", isAllDay);
            event.put("isOnlineMeeting", isOnlineMeeting);
            if (isOnlineMeeting) {
                event.put("onlineMeetingProvider", "teamsForBusiness");
            }

            event.put("isReminderOn", reminderMinutes > 0);
            event.put("reminderMinutesBeforeStart", reminderMinutes);

            if (importance != null && !importance.isBlank()) {
                event.put("importance", importance);
            }

            if (location != null && !location.isBlank()) {
                ObjectNode loc = MAPPER.createObjectNode();
                loc.put("displayName", location);
                event.set("location", loc);
            }

            if (attendees != null && !attendees.isBlank()) {
                JsonNode attendeeList = MAPPER.readTree(attendees);
                ArrayNode graphAttendees = MAPPER.createArrayNode();
                for (JsonNode att : attendeeList) {
                    ObjectNode graphAtt = MAPPER.createObjectNode();
                    ObjectNode emailAddr = MAPPER.createObjectNode();
                    emailAddr.put("address", att.has("email") ? att.get("email").asText() : "");
                    emailAddr.put("name", att.has("name") ? att.get("name").asText() : "");
                    graphAtt.set("emailAddress", emailAddr);
                    graphAtt.set("type", MAPPER.createObjectNode()
                            .put("dummy", att.has("type") ? att.get("type").asText() : "required"));
                    graphAtt.put("type", att.has("type") ? att.get("type").asText() : "required");
                    graphAttendees.add(graphAtt);
                }
                event.set("attendees", graphAttendees);
            }

            if (recurrence != null && !recurrence.isBlank()) {
                event.set("recurrence", MAPPER.readTree(recurrence));
            }

            if (categories != null && !categories.isBlank()) {
                ArrayNode cats = MAPPER.createArrayNode();
                for (String cat : categories.split(",")) {
                    cats.add(cat.trim());
                }
                event.set("categories", cats);
            }

            String url = String.format("%s/users/%s/events",
                    configuration.getBasePath(), configuration.getSenderUserId());

            HttpResponse<String> response = executeGraphRequest("POST", url, MAPPER.writeValueAsString(event));
            handleErrorResponse(response);

            JsonNode result = MAPPER.readTree(response.body());
            String eventId = getTextOrEmpty(result, "id");
            String iCalUId = getTextOrEmpty(result, "iCalUId");
            String webLink = getTextOrEmpty(result, "webLink");
            String onlineMeetingUrl = "";
            if (result.has("onlineMeeting") && result.get("onlineMeeting").has("joinUrl")) {
                onlineMeetingUrl = result.get("onlineMeeting").get("joinUrl").asText();
            }
            String createdDateTime = getTextOrEmpty(result, "createdDateTime");

            return new CreateEventResult(eventId, iCalUId, webLink, onlineMeetingUrl, createdDateTime, true, "");
        });
    }

    // === Send Template Email ===
    public SendTemplateResult sendTemplate(String toRecipients, String ccRecipients,
                                            String emailSubject, String templateHtml,
                                            String templateVariables, String importance,
                                            String attachments, boolean saveToSentItems)
            throws MsOutlookException {
        return retryPolicy.execute(() -> {
            // Render template
            Map<String, String> variables = MAPPER.readValue(templateVariables,
                    new TypeReference<Map<String, String>>() {});

            String renderedBody = renderTemplate(templateHtml, variables);
            String renderedSubject = renderTemplate(emailSubject, variables);

            ObjectNode payload = buildSendMailPayload(toRecipients, ccRecipients, null,
                    renderedSubject, renderedBody, "html", importance, attachments, null);
            payload.put("saveToSentItems", saveToSentItems);

            String url = String.format("%s/users/%s/sendMail",
                    configuration.getBasePath(), configuration.getSenderUserId());

            HttpResponse<String> response = executeGraphRequest("POST", url, MAPPER.writeValueAsString(payload));
            handleErrorResponse(response);

            return new SendTemplateResult(null, renderedSubject, true, "");
        });
    }

    // === Helper methods ===

    String renderTemplate(String template, Map<String, String> variables) {
        if (template == null) return "";
        String result = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (key.startsWith("raw:")) {
                result = result.replace("{{" + key.substring(4) + "}}", value);
            } else {
                result = result.replace("{{" + key + "}}", escapeHtml(value));
            }
        }
        // Log unresolved placeholders
        java.util.regex.Matcher matcher = Pattern.compile("\\{\\{(\\w+)}}").matcher(result);
        List<String> unresolved = new ArrayList<>();
        while (matcher.find()) {
            unresolved.add(matcher.group(1));
        }
        if (!unresolved.isEmpty()) {
            log.warn("Unresolved template placeholders: {}", unresolved);
        }
        return result;
    }

    static String escapeHtml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private ObjectNode buildSendMailPayload(String toRecipients, String ccRecipients,
                                             String bccRecipients, String subject, String body,
                                             String contentType, String importance,
                                             String attachments, String replyTo) throws Exception {
        ObjectNode message = MAPPER.createObjectNode();
        message.put("subject", subject);

        ObjectNode bodyNode = MAPPER.createObjectNode();
        bodyNode.put("contentType", contentType != null ? contentType : "html");
        bodyNode.put("content", body);
        message.set("body", bodyNode);

        message.set("toRecipients", buildRecipientArray(toRecipients));

        if (ccRecipients != null && !ccRecipients.isBlank()) {
            message.set("ccRecipients", buildRecipientArray(ccRecipients));
        }
        if (bccRecipients != null && !bccRecipients.isBlank()) {
            message.set("bccRecipients", buildRecipientArray(bccRecipients));
        }

        if (importance != null && !importance.isBlank()) {
            message.put("importance", importance);
        }

        if (replyTo != null && !replyTo.isBlank()) {
            ArrayNode replyToArr = MAPPER.createArrayNode();
            ObjectNode replyAddr = MAPPER.createObjectNode();
            ObjectNode emailAddr = MAPPER.createObjectNode();
            emailAddr.put("address", replyTo);
            replyAddr.set("emailAddress", emailAddr);
            replyToArr.add(replyAddr);
            message.set("replyTo", replyToArr);
        }

        if (attachments != null && !attachments.isBlank()) {
            JsonNode attList = MAPPER.readTree(attachments);
            ArrayNode graphAttachments = MAPPER.createArrayNode();
            for (JsonNode att : attList) {
                ObjectNode graphAtt = MAPPER.createObjectNode();
                graphAtt.put("@odata.type", "#microsoft.graph.fileAttachment");
                graphAtt.put("name", att.has("name") ? att.get("name").asText() : "attachment");
                graphAtt.put("contentType", att.has("contentType") ? att.get("contentType").asText() : "application/octet-stream");
                graphAtt.put("contentBytes", att.has("contentBase64") ? att.get("contentBase64").asText() : "");
                graphAttachments.add(graphAtt);
            }
            message.set("attachments", graphAttachments);
        }

        ObjectNode payload = MAPPER.createObjectNode();
        payload.set("message", message);
        return payload;
    }

    private ArrayNode buildRecipientArray(String recipientsJson) throws Exception {
        ArrayNode result = MAPPER.createArrayNode();
        if (recipientsJson == null || recipientsJson.isBlank()) return result;

        JsonNode recipients = MAPPER.readTree(recipientsJson);
        for (JsonNode r : recipients) {
            ObjectNode recipient = MAPPER.createObjectNode();
            ObjectNode emailAddress = MAPPER.createObjectNode();
            emailAddress.put("address", r.has("email") ? r.get("email").asText() : "");
            emailAddress.put("name", r.has("name") ? r.get("name").asText() : "");
            recipient.set("emailAddress", emailAddress);
            result.add(recipient);
        }
        return result;
    }

    HttpResponse<String> executeGraphRequest(String method, String url, String body)
            throws Exception {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofMillis(configuration.getReadTimeout()));

        switch (method.toUpperCase()) {
            case "GET" -> requestBuilder.GET();
            case "POST" -> requestBuilder.POST(HttpRequest.BodyPublishers.ofString(body != null ? body : ""));
            case "PATCH" -> requestBuilder.method("PATCH", HttpRequest.BodyPublishers.ofString(body != null ? body : ""));
            case "DELETE" -> requestBuilder.DELETE();
            default -> throw new MsOutlookException("Unsupported HTTP method: " + method);
        }

        HttpResponse<String> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());

        // Handle 401 with token refresh
        if (response.statusCode() == 401) {
            log.info("Received 401, attempting token refresh");
            authenticate();
            requestBuilder.header("Authorization", "Bearer " + accessToken);
            response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
        }

        return response;
    }

    private void handleErrorResponse(HttpResponse<String> response) throws MsOutlookException {
        int status = response.statusCode();
        if (status >= 200 && status < 300) return;

        String errorMsg = "Graph API error (HTTP " + status + ")";
        try {
            JsonNode errorJson = MAPPER.readTree(response.body());
            if (errorJson.has("error")) {
                JsonNode error = errorJson.get("error");
                String code = getTextOrEmpty(error, "code");
                String message = getTextOrEmpty(error, "message");
                errorMsg = String.format("Graph API error: %s -- %s (HTTP %d)", code, message, status);
            }
        } catch (Exception ignored) {
            // Use default error message
        }

        boolean retryable = RetryPolicy.isRetryableStatusCode(status);
        throw new MsOutlookException(errorMsg, status, retryable);
    }

    private static String getTextOrEmpty(JsonNode node, String field) {
        return node != null && node.has(field) && !node.get(field).isNull() ? node.get(field).asText() : "";
    }
}
