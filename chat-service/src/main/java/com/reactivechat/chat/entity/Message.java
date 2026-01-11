package com.reactivechat.chat.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "messages")
@CompoundIndex(name = "room_timestamp", def = "{ 'roomId' : 1, 'timestamp' : 1 }")
public class Message {

    @Id
    private String id;
    private String roomId;
    private String senderId;
    private String content;
    private Instant timestamp;
    private MessageStatus status;

    public enum MessageStatus { SENT, DELIVERED, READ }

    public Message() {
        this.timestamp = Instant.now();
        this.status = MessageStatus.SENT;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    public MessageStatus getStatus() { return status; }
    public void setStatus(MessageStatus status) { this.status = status; }
}
