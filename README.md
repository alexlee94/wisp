# Wisp

A real-time chat and notification service built with Java, Spring Boot, WebSocket (STOMP), RabbitMQ, and React. Supports live messaging, presence tracking, and reliable online/offline status detection across multiple tabs and dropped connections.

## Tech Stack

**Backend:** Java, Spring Boot, WebSocket (STOMP), RabbitMQ, Spring Data JPA, MySQL  
**Frontend:** React, TypeScript, Material UI, STOMP.js  
**Tools:** Docker, Git

## Features

- **Real-Time Messaging** — instant message delivery using WebSocket with the STOMP protocol, relayed through RabbitMQ instead of an in-memory broker
- **Message Broker Architecture** — Spring Boot relays all STOMP messages to RabbitMQ using a broker relay, which handles delivering messages to subscribed clients, decoupling message routing from the application server
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
Spring Boot relays the STOMP session to RabbitMQ
        ↓
Session registered with a unique sessionId, tied to userId
        ↓
If this is the user's first active session → broadcast "online" via RabbitMQ
        ↓
Client sends a heartbeat every 15 seconds
        ↓
Backend scheduled job checks every 15 seconds for sessions
with no heartbeat in the last 30 seconds
        ↓
Stale sessions are removed
        ↓
If zero sessions remain for that user → broadcast "offline" via RabbitMQ
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

2. Start RabbitMQ with the STOMP plugin enabled:
```bash
docker run --name wisp-rabbitmq -p 5672:5672 -p 8090:61613 -p 15672:15672 -e RABBITMQ_DEFAULT_USER=guest -e RABBITMQ_DEFAULT_PASS=guest rabbitmq:3-management
docker exec wisp-rabbitmq rabbitmq-plugins enable rabbitmq_stomp
```

3. Configure `application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3307/wisp?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=password
server.port=8081
```

4. Run the Spring Boot app from IntelliJ or:
```bash
./mvnw spring-boot:run
```

RabbitMQ management UI available at `http://localhost:15672` (guest/guest)

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
| `/topic/room.{roomId}` | Server → Client (via RabbitMQ) | Broadcast messages to a chat room |
| `/topic/presence` | Server → Client (via RabbitMQ) | Broadcast online/offline status changes |