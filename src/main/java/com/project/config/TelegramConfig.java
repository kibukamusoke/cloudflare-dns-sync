package com.project.config;

public class TelegramConfig {
    private boolean enabled = false;
    private String botToken;
    private String chatId;
    private String message = "IP address changed to: {ip}";

    // Getters and setters
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getBotToken() { return botToken; }
    public void setBotToken(String botToken) { this.botToken = botToken; }
    public String getChatId() { return chatId; }
    public void setChatId(String chatId) { this.chatId = chatId; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
} 