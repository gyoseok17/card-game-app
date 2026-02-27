package com.onecard.domain.gameroom;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GameRoomRepository extends JpaRepository<GameRoom, Long> {
    List<GameRoom> findByStatus(RoomStatus status);

    @Query("SELECT r, COUNT(m) FROM GameRoom r JOIN FETCH r.createdBy LEFT JOIN GameRoomMember m ON m.room = r WHERE r.status = :status GROUP BY r")
    List<Object[]> findByStatusWithMemberCount(@Param("status") RoomStatus status);
}
