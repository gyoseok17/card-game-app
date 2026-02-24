package com.onecard.domain.gameroom;

import com.onecard.domain.user.User;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "game_rooms")
@Getter
@NoArgsConstructor
public class GameRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoomStatus status = RoomStatus.WAITING;

    @Column(nullable = false)
    private int maxPlayers;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_id")
    private User winner;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;

    @Builder
    public GameRoom(String name, int maxPlayers, User createdBy) {
        this.name = name;
        this.maxPlayers = maxPlayers;
        this.createdBy = createdBy;
    }

    public void start() {
        this.status = RoomStatus.PLAYING;
        this.startedAt = LocalDateTime.now();
    }

    public void finish(User winner) {
        this.status = RoomStatus.FINISHED;
        this.winner = winner;
        this.finishedAt = LocalDateTime.now();
    }

    public void reset() {
        this.status = RoomStatus.WAITING;
        this.winner = null;
        this.finishedAt = null;
    }
}
