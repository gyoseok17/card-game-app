package com.onecard.domain.gameroom;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GameRoomRepository extends JpaRepository<GameRoom, Long> {
    List<GameRoom> findByStatus(RoomStatus status);
}
