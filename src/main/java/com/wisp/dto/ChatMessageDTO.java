package com.wisp.dto;

public record ChatMessageDTO(
        Long senderId,
        String senderUsername,
        String content,
        String roomId
) {
    public ChatMessageDTO(Long senderId, String content, String roomId) {
        this(senderId, null, content, roomId);
    }
}
