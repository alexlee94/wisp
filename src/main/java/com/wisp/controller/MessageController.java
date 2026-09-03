package com.wisp.controller;

import com.wisp.entity.Message;
import com.wisp.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageRepository messageRepository;

    @GetMapping("/{roomId}")
    public ResponseEntity<List<Message>> getRoomMessages(@PathVariable String roomId) {
        return ResponseEntity.ok(messageRepository.findByRoomIdOrderBySentAtAsc(roomId));
    }
}
