package com.onecard.domain.gameroom;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GameRoomMemberRepository extends JpaRepository<GameRoomMember, Long> {

    @Query("SELECT m FROM GameRoomMember m JOIN FETCH m.user WHERE m.room.id = :roomId ORDER BY m.seatOrder")
    List<GameRoomMember> findByRoomIdWithUser(@Param("roomId") Long roomId);

    boolean existsByRoomIdAndUserId(Long roomId, Long userId);

    Optional<GameRoomMember> findByRoomIdAndUserId(Long roomId, Long userId);

    long countByRoomId(Long roomId);

    void deleteByRoomIdAndUserId(Long roomId, Long userId);

    void deleteAllByRoomId(Long roomId);
}
