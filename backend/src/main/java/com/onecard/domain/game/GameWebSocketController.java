package com.onecard.domain.game;

import com.onecard.dto.request.GameActionRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class GameWebSocketController {

    private final GameService gameService;

    @MessageMapping("/game/{roomId}/action")
    public void handleGameAction(@DestinationVariable Long roomId,
                                  @Payload GameActionRequest request,
                                  Principal principal) {
        gameService.handleAction(roomId, principal.getName(), request);
    }

    @MessageMapping("/game/{roomId}/rejoin")
    public void handleRejoin(@DestinationVariable Long roomId, Principal principal) {
        gameService.rejoinGame(roomId, principal.getName());
    }
}
