package com.aryan.edenic.models;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ChatMessage {
    private String messageId;
    private String senderId;
    private String senderName;
    private String content;
    private long timestamp;
    private MessageType type;
    private String stockSymbol; // For stock share messages
    private double stockPrice;  // For stock share messages
    private boolean read;

    public enum MessageType {
        TEXT, STOCK_SHARE
    }

    public ChatMessage() {
        // Required empty constructor for Firebase
    }

    // Constructor for text messages
    public ChatMessage(String senderId, String senderName, String content) {
        this.messageId = generateMessageId();
        this.senderId = senderId;
        this.senderName = senderName;
        this.content = content;
        this.timestamp = new Date().getTime();
        this.type = MessageType.TEXT;
        this.read = false;
    }

    // Constructor for stock share messages
    public ChatMessage(String senderId, String senderName, String stockSymbol, double stockPrice) {
        this.messageId = generateMessageId();
        this.senderId = senderId;
        this.senderName = senderName;
        this.content = "Shared " + stockSymbol + " stock";
        this.timestamp = new Date().getTime();
        this.type = MessageType.STOCK_SHARE;
        this.stockSymbol = stockSymbol;
        this.stockPrice = stockPrice;
        this.read = false;
    }

    private String generateMessageId() {
        return java.util.UUID.randomUUID().toString();
    }

    // Getters and setters
    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public MessageType getType() { return type; }
    public void setType(MessageType type) { this.type = type; }

    public String getStockSymbol() { return stockSymbol; }
    public void setStockSymbol(String stockSymbol) { this.stockSymbol = stockSymbol; }

    public double getStockPrice() { return stockPrice; }
    public void setStockPrice(double stockPrice) { this.stockPrice = stockPrice; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    // For Firebase
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("messageId", messageId);
        result.put("senderId", senderId);
        result.put("senderName", senderName);
        result.put("content", content);
        result.put("timestamp", timestamp);
        result.put("type", type.toString());
        result.put("read", read);

        if (type == MessageType.STOCK_SHARE) {
            result.put("stockSymbol", stockSymbol);
            result.put("stockPrice", stockPrice);
        }

        return result;
    }
}