package com.onecard.domain.game;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class GameChatController {

    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/game/{roomId}/chat")
    public void handleChat(@DestinationVariable Long roomId,
                           @Payload Map<String, String> payload,
                           Principal principal) {
        String content = payload.get("content");
        if (content == null || content.isBlank() || content.length() > 50) return;

        Map<String, Object> message = Map.of(
                "type", "CHAT",
                "senderName", principal.getName(),
                "content", content,
                "sentAt", LocalDateTime.now().toString()
        );
        messagingTemplate.convertAndSend("/topic/game/" + roomId + "/chat", message);
    }
}
