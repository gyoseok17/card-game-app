package com.onecard.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CreateGameRoomRequest {
    @NotBlank
    private String name;

    @Min(2) @Max(4)
    private int maxPlayers = 4;
}
