package com.project.notifications;

import com.project.config.DnsUpdaterConfig;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    private final DnsUpdaterConfig config;

    public NotificationService(DnsUpdaterConfig config) {
        this.config = config;
    }

    public void notifyIpChange(String newIp) {
        if (!config.getNotifications().getTelegram().isEnabled()) {
            return;
        }

        try {
            String botToken = config.getNotifications().getTelegram().getBotToken();
            String chatId = config.getNotifications().getTelegram().getChatId();
            String message = config.getNotifications().getTelegram().getMessage()
                    .replace("{ip}", newIp);

            String url = String.format("https://api.telegram.org/bot%s/sendMessage", botToken);
            String json = String.format("{\"chat_id\":\"%s\",\"text\":\"%s\"}", chatId, message);

            try (CloseableHttpClient client = HttpClients.createDefault()) {
                HttpPost httpPost = new HttpPost(url);
                httpPost.setEntity(new StringEntity(json));
                httpPost.setHeader("Content-Type", "application/json");
                
                client.execute(httpPost, response -> {
                    logger.debug("Telegram notification sent. Status: {}", 
                        response.getCode());
                    return null;
                });
            }
        } catch (Exception e) {
            logger.error("Failed to send Telegram notification", e);
        }
    }
} 