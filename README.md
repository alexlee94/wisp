# Wisp

A real-time chat and notification service built with Java, Spring Boot, WebSocket (STOMP), and React. Supports live messaging, presence tracking, and reliable online/offline status detection across multiple tabs and dropped connections.

## Tech Stack

**Backend:** Java, Spring Boot, WebSocket (STOMP), Spring Data JPA, MySQL  
**Frontend:** React, TypeScript, Material UI, STOMP.js  
**Tools:** Docker, Git

## Features

- **Real-Time Messaging** — instant message delivery using WebSocket with the STOMP protocol
- **Presence Tracking** — live online/offline status broadcast to all connected users
- **Heartbeat Detection** — clients send a heartbeat every 15 seconds; sessions that miss two consecutive heartbeats (30 seconds) are automatically marked offline, solving the problem of users closing the browser without triggering a clean disconnect event
- **Multi-Tab Reference Counting** — opening multiple tabs for the same user creates multiple tracked sessions; the user is only marked offline once the last open tab disconnects, preventing false offline status while other sessions remain active
- **Message History** — chat history persisted in MySQL and loaded on room join

## How It Works

```
User opens a tab
        ↓
WebSocket connection established (STOMP over SockJS)
        ↓
Session registered with a unique sessionId, tied to userId
        ↓
If this is the user's first active session → broadcast "online"
        ↓
Client sends a heartbeat every 15 seconds
        ↓
Backend scheduled job checks every 15 seconds for sessions
with no heartbeat in the last 30 seconds
        ↓
Stale sessions are removed
        ↓
If zero sessions remain for that user → broadcast "offline"
```

## Running Locally

### Prerequisites
- Java 17+
- Maven
- Docker
- Node.js 18+

### Backend

1. Start MySQL with Docker:
```bash
docker run --name wisp-db -e MYSQL_ROOT_PASSWORD=password -e MYSQL_DATABASE=wisp -p 3307:3306 -d mysql:8
```

2. Configure `application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3307/wisp?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=password
server.port=8081
```

3. Run the Spring Boot app from IntelliJ or:
```bash
./mvnw spring-boot:run
```

### Frontend

```bash
cd frontend
npm install
npm start
```

App runs at `http://localhost:3000`  
API runs at `http://localhost:8081`

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/users/register` | Register or fetch a user by username |
| GET | `/api/users` | Get all users |
| GET | `/api/messages/{roomId}` | Get message history for a room |

## WebSocket Destinations

| Destination | Direction | Description |
|--------------|-----------|-------------|
| `/ws` | Connect | WebSocket handshake endpoint |
| `/app/chat.register` | Client → Server | Registers a new session for presence tracking |
| `/app/chat.heartbeat` | Client → Server | Sent every 15 seconds to keep session alive |
| `/app/chat.sendMessage` | Client → Server | Send a chat message |
| `/topic/room/{roomId}` | Server → Client | Broadcast messages to a chat room |
| `/topic/presence` | Server → Client | Broadcast online/offline status changes |