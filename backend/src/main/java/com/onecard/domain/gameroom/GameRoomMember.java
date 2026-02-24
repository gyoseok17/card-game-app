package com.onecard.domain.gameroom;

import com.onecard.domain.user.User;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "game_room_members",
    uniqueConstraints = @UniqueConstraint(columnNames = {"room_id", "user_id"})
)
@Getter
@NoArgsConstructor
public class GameRoomMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private GameRoom room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private int seatOrder;

    @Column(nullable = false)
    private boolean isReady = false;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime joinedAt;

    @Builder
    public GameRoomMember(GameRoom room, User user, int seatOrder) {
        this.room = room;
        this.user = user;
        this.seatOrder = seatOrder;
    }

    public void toggleReady() {
        this.isReady = !this.isReady;
    }

    public void resetReady() {
        this.isReady = false;
    }
}
