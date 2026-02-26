package com.onecard.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CreateGameRoomRequest {
    @NotBlank @Size(max = 20)
    private String name;

    @Min(2) @Max(4)
    private int maxPlayers = 4;
}
