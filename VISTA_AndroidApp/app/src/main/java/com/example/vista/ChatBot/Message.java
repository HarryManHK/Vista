package com.example.vista.ChatBot;

public class Message {
    private String content;
    private boolean isUserMessage;  // 判斷這條訊息是用戶發的還是機器人發的

    public Message(String content, boolean isUserMessage) {
        this.content = content;
        this.isUserMessage = isUserMessage;
    }

    public String getContent() {
        return content;
    }

    public boolean isUserMessage() {
        return isUserMessage;
    }
}
