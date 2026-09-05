package com.wisp.websocket;

import com.wisp.dto.ChatMessageDTO;
import com.wisp.entity.Message;
import com.wisp.entity.User;
import com.wisp.repository.MessageRepository;
import com.wisp.repository.UserRepository;
import com.wisp.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final SessionService sessionService;
    private final SimpMessagingTemplate messagingTemplate;

    // ── Send a chat message ──────────────────────────────────────────────────

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload ChatMessageDTO chatMessage) {
        User sender = userRepository.findById(chatMessage.senderId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Message message = Message.builder()
                .sender(sender)
                .content(chatMessage.content())
                .roomId(chatMessage.roomId())
                .build();

        messageRepository.save(message);

        messagingTemplate.convertAndSend(
                "/topic/room." + chatMessage.roomId(),
                new ChatMessageDTO(sender.getId(), sender.getUsername(), chatMessage.content(), chatMessage.roomId())
        );
    }

    // ── Register a new session (first connect) ───────────────────────────────

    @MessageMapping("/chat.register")
    public void registerSession(@Payload ChatMessageDTO chatMessage, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        sessionService.registerSession(chatMessage.senderId(), sessionId);
    }

    // ── Heartbeat ping ────────────────────────────────────────────────────────

    @MessageMapping("/chat.heartbeat")
    public void heartbeat(SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        sessionService.recordHeartbeat(sessionId);
    }
}
