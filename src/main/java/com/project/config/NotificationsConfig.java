package com.project.config;

public class NotificationsConfig {
    private TelegramConfig telegram = new TelegramConfig();

    public TelegramConfig getTelegram() {
        return telegram;
    }

    public void setTelegram(TelegramConfig telegram) {
        this.telegram = telegram;
    }
} 