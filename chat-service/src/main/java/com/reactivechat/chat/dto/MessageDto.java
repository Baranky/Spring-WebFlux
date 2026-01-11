package com.reactivechat.chat.dto;

import com.reactivechat.chat.entity.Message;

import java.time.Instant;

public record MessageDto(
        String id,
        String roomId,
        String senderId,
        String content,
        Instant timestamp,
        Message.MessageStatus status,
        boolean sentByMe
) {
    public static MessageDto from(com.reactivechat.chat.entity.Message m) {
        return new MessageDto(m.getId(), m.getRoomId(), m.getSenderId(), m.getContent(), m.getTimestamp(), m.getStatus(), false);
    }

    public static MessageDto forViewer(com.reactivechat.chat.entity.Message m, String currentUserId) {
        boolean mine = currentUserId != null && currentUserId.equals(m.getSenderId());
        Message.MessageStatus viewerStatus = mine ? Message.MessageStatus.SENT : Message.MessageStatus.DELIVERED;
        return new MessageDto(m.getId(), m.getRoomId(), m.getSenderId(), m.getContent(), m.getTimestamp(), viewerStatus, mine);
    }
}
