package com.project.notifications;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.project.config.DnsUpdaterConfig;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final DnsUpdaterConfig config;
    private final CloseableHttpClient httpClient;

    public NotificationService(DnsUpdaterConfig config, CloseableHttpClient httpClient) {
        this.config = config;
        this.httpClient = httpClient;
    }

    /**
     * Sends a Telegram notification for a single changed record. The message
     * template supports {@code {ip}} and {@code {record}} placeholders.
     */
    public void notifyIpChange(String recordName, String newIp) {
        if (!config.getNotifications().getTelegram().isEnabled()) {
            return;
        }

        try {
            String botToken = config.getNotifications().getTelegram().getBotToken();
            String chatId = config.getNotifications().getTelegram().getChatId();
            String message = config.getNotifications().getTelegram().getMessage()
                    .replace("{ip}", newIp)
                    .replace("{record}", recordName);

            String url = String.format("https://api.telegram.org/bot%s/sendMessage", botToken);

            // Build the body with Jackson so any character in the message is
            // safely escaped (no hand-rolled JSON / injection risk).
            ObjectNode body = objectMapper.createObjectNode();
            body.put("chat_id", chatId);
            body.put("text", message);
            String json = objectMapper.writeValueAsString(body);

            HttpPost httpPost = new HttpPost(url);
            httpPost.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));

            httpClient.execute(httpPost, response -> {
                logger.debug("Telegram notification sent. Status: {}", response.getCode());
                return null;
            });
        } catch (Exception e) {
            logger.error("Failed to send Telegram notification", e);
        }
    }
}
