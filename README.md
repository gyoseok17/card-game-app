# OneCard - 실시간 멀티플레이어 원카드 게임

WebSocket(STOMP)을 활용한 실시간 2~4인 원카드 카드 게임 서비스

---

## 기술 스택

| 구분 | 기술 |
|---|---|
| **Backend** | Java 21, Spring Boot 3, Spring Security, Spring WebSocket (STOMP/SockJS), JPA/Hibernate |
| **Frontend** | React 18, TypeScript, Vite, Zustand, STOMP.js, TailwindCSS |
| **DB** | MySQL 8.0, Redis 7 |
| **Infra** | Docker, Docker Compose, Nginx |

---

## 주요 기능

- **실시간 멀티플레이** — WebSocket STOMP 기반 2~4인 동시 플레이
- **게임 룰 완전 구현** — ACE/2/조커(공격), JACK(스킵), QUEEN(방향 전환), KING(추가 턴), 7(문양 변경)
- **손패 은닉** — 공개 채널(`/topic`)과 개인 채널(`/user/queue`) 분리로 타인 손패 노출 차단
- **접속 끊김 유예** — 게임 중 15초, 대기방 3초 재접속 유예 후 자동 퇴장 처리
- **턴 타임아웃** — 30초 내 미응답 시 자동 카드 드로우
- **포인트 시스템** — 2/3/4인 인원별 등수에 따른 포인트 차등 지급
- **중복 로그인 방지** — Redis 토큰 블랙리스트 + 기존 세션 강제 종료

---

## 아키텍처

```
[React + Vite (TypeScript)]
        │
        ├─ HTTP REST (axios) ──────────────────┐
        └─ WebSocket STOMP/SockJS ─────────────┤
                                               ▼
                                    [Spring Boot (Java 21)]
                                               │
                               ┌───────────────┴──────────────┐
                           [MySQL 8.0]                    [Redis 7]
                        (users, rooms,               (JWT 블랙리스트,
                         members)                    현재 토큰 저장)
```

**WebSocket 채널 구조**

| 채널 | 방향 | 용도 |
|---|---|---|
| `/topic/game/{roomId}` | 브로드캐스트 | 공개 게임 상태 (탑카드, 플레이어 정보) |
| `/user/queue/hand` | 개인 | 본인 손패 (타인 열람 불가) |
| `/user/queue/notification` | 개인 | 오류 알림 |
| `/topic/game/{roomId}/chat` | 브로드캐스트 | 인게임 채팅 |
| `/topic/room/{roomId}` | 브로드캐스트 | 대기방 상태 |
| `/user/queue/force-disconnect` | 개인 | 중복 로그인 강제 종료 |

---

## ERD

```mermaid
erDiagram
    users {
        bigint      id          PK
        varchar     username    UK  "NOT NULL"
        varchar     email       UK  "NOT NULL"
        varchar     password        "NOT NULL"
        int         wins            "DEFAULT 0"
        int         losses          "DEFAULT 0"
        int         points          "DEFAULT 1000"
        datetime    created_at
    }

    game_rooms {
        bigint      id          PK
        varchar     name            "NOT NULL"
        varchar     status          "WAITING / PLAYING / FINISHED"
        int         max_players     "NOT NULL"
        bigint      created_by  FK  "→ users"
        bigint      winner_id   FK  "→ users (nullable)"
        datetime    created_at
        datetime    started_at      "nullable"
        datetime    finished_at     "nullable"
    }

    game_room_members {
        bigint      id          PK
        bigint      room_id     FK  "→ game_rooms"
        bigint      user_id     FK  "→ users"
        int         seat_order      "NOT NULL"
        boolean     is_ready        "DEFAULT false"
        datetime    joined_at
    }

    users         ||--o{ game_rooms         : "방 생성 (created_by)"
    users         |o--o{ game_rooms         : "우승 (winner_id)"
    users         ||--o{ game_room_members  : "참여"
    game_rooms    ||--o{ game_room_members  : "구성"
```

> 게임 진행 중 상태(손패, 덱, 공격 스택 등)는 DB에 저장하지 않고 서버 메모리(`ConcurrentHashMap<roomId, GameState>`)에서 관리. 게임 종료 시점에만 포인트 결과를 DB에 반영.

---

## 시퀀스 다이어그램

### 게임 시작 흐름

```mermaid
sequenceDiagram
    actor 방장
    actor 참가자
    participant REST as REST API
    participant WS  as WebSocket (STOMP)
    participant DB  as MySQL

    방장   ->> REST : POST /api/rooms (방 생성)
    REST   ->> DB   : INSERT game_rooms, game_room_members
    REST  -->> 방장  : GameRoomResponse

    참가자 ->> REST : POST /api/rooms/{id}/join
    REST   ->> DB   : INSERT game_room_members
    REST   ->> WS   : /topic/room/{id} 브로드캐스트
    WS    -->> 방장  : 실시간 멤버 업데이트
    REST  -->> 참가자: GameRoomResponse

    참가자 ->> REST : POST /api/rooms/{id}/ready
    REST   ->> DB   : UPDATE is_ready = true
    REST   ->> WS   : /topic/room/{id} 브로드캐스트
    WS    -->> 방장  : 준비 완료 표시

    방장   ->> REST : POST /api/rooms/{id}/start
    REST   ->> DB   : UPDATE game_rooms SET status = PLAYING
    Note over REST  : GameEngine.initializeGame()<br/>덱 54장 생성 → 셔플 → 7장 배분
    REST   ->> WS   : /topic/game/{id} 공개 상태 브로드캐스트
    WS    -->> 방장  : GameStateResponse (탑카드 · 플레이어 정보)
    WS    -->> 참가자: GameStateResponse
    REST   ->> WS   : /user/queue/hand 개인 손패 전송
    WS    -->> 방장  : PlayerHandResponse (본인 손패만)
    WS    -->> 참가자: PlayerHandResponse (본인 손패만)
```

### 게임 액션 & 턴 타임아웃

```mermaid
sequenceDiagram
    actor P1 as 현재 차례 플레이어
    actor P2 as 상대 플레이어
    participant WS    as WebSocket (STOMP)
    participant GS    as GameService
    participant GE    as GameEngine
    participant Timer as TurnTimerScheduler

    P1  ->> WS    : /app/game/{id}/action {PLAY_CARD, cardIndex}
    WS  ->> GS    : handleAction() [synchronized(roomLock)]
    GS  ->> GE    : isValidPlay() 유효성 검사
    GE -->> GS    : true
    GS  ->> GE    : applyPlayCard() 상태 변환
    Note over GE  : 일반 카드 → advanceTurn<br/>ACE·2·조커 → attackStack 누적<br/>JACK → 다음 턴 스킵<br/>QUEEN → 방향 전환<br/>KING → 추가 턴 유지<br/>7 → 문양 선택 대기
    GS  ->> WS    : /topic/game/{id} 전체 브로드캐스트
    WS -->> P1    : 갱신된 GameStateResponse
    WS -->> P2    : 갱신된 GameStateResponse
    GS  ->> WS    : /user/queue/hand 개별 손패 전송
    WS -->> P1    : 카드 제거된 손패
    WS -->> P2    : 손패 변경 없음
    GS  ->> Timer : scheduleTurnTimer(30초)

    Note over P2  : 30초 내 미응답
    Timer ->> GS  : handleTimeout()
    GS  ->> GE    : applyDrawCards() 자동 카드 뽑기
    GS  ->> WS    : /topic/game/{id} 브로드캐스트
    WS -->> P1    : 갱신된 GameStateResponse
    WS -->> P2    : 카드 추가된 손패
```

### 접속 끊김 유예 & 재접속

```mermaid
sequenceDiagram
    actor P     as 플레이어
    actor Other as 상대 플레이어
    participant WS   as WebSocket (STOMP)
    participant GS   as GameService
    participant Disc as DisconnectScheduler

    Note over P       : 네트워크 끊김 / 새로고침
    WS   ->> GS       : SessionDisconnectEvent
    GS   ->> GS       : connected = false · disconnectedAt = now()
    GS   ->> WS       : /topic/game/{id} 브로드캐스트
    WS  -->> Other    : 연결 끊김 표시 (아바타 빨간색)
    GS   ->> Disc     : scheduleLeave(userId, 15초)

    alt 15초 내 재접속
        P    ->> WS   : STOMP CONNECT (JWT 포함)
        WS   ->> GS   : SessionConnectedEvent → cancelLeave()
        P    ->> WS   : /app/game/{id}/rejoin
        WS   ->> GS   : rejoinGame()
        GS   ->> GS   : connected = true · disconnectedAt = null
        GS   ->> WS   : /topic/game/{id} 브로드캐스트
        WS  -->> P    : GameStateResponse + 손패 복구
        WS  -->> Other: 재접속 상태 업데이트
    else 15초 초과
        Disc ->> GS   : handlePlayerLeave(roomId, userId)
        GS   ->> GS   : 손패 → 덱 반납 · players 목록 제거
        Note over GS  : 남은 플레이어 1명이면<br/>자동 승리 → GAME_OVER
        GS   ->> WS   : /topic/game/{id} 브로드캐스트
        WS  -->> Other: 갱신된 GameStateResponse
    end
```

---

## 로컬 개발 환경 실행

### 사전 준비

- Docker, Docker Compose 설치
- `.env` 파일 생성 (프로젝트 루트)

```env
DB_PASSWORD=yourpassword
JWT_SECRET=your-secret-key-at-least-32-characters
```

### 실행

```bash
docker compose up
```

| 서비스 | URL |
|---|---|
| 프론트엔드 | http://localhost:5173 |
| 백엔드 API | http://localhost:8080 |

---

## 프로덕션 배포

```bash
# 이미지 빌드 및 배포
docker compose -f docker-compose.prod.yml up -d

# 저사양 서버 (1GB RAM 이하)
docker compose -f docker-compose-lightweight.yml up -d
```

> 저사양 구성: MySQL 350MB, Redis 100MB, Backend 450MB(`-Xmx200m`) 메모리 제한 적용

---

## 프로젝트 구조

```
card-game-app/
├── backend/
│   └── src/main/java/com/onecard/
│       ├── config/          # CORS, Security, WebSocket 설정
│       ├── domain/
│       │   ├── game/
│       │   │   ├── engine/  # Card, CardDeck, GameEngine (순수 게임 로직)
│       │   │   └── state/   # GameState, PlayerState
│       │   ├── gameroom/    # 대기방 도메인
│       │   └── user/        # 유저·인증 도메인
│       ├── dto/             # Request / Response DTO
│       └── security/        # JWT, 토큰 블랙리스트, WebSocket 인증
├── frontend/
│   └── src/
│       ├── components/game/ # GameBoard, CardComponent, TurnTimer 등
│       ├── hooks/           # useGameSocket, useWebSocket
│       ├── pages/           # LobbyPage, GamePage, LoginPage
│       ├── store/           # Zustand 전역 상태
│       └── types/           # TypeScript 타입 정의
├── docker-compose.yml               # 개발 환경
├── docker-compose.prod.yml          # 프로덕션
└── docker-compose-lightweight.yml   # 저사양 서버
```
