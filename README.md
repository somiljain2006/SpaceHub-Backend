# SpaceHub - Backend Application

A robust collaborative backend platform built with Spring Boot, featuring organized chat rooms, real-time voice rooms, and secure file sharing within workspaces. It supports multiple workspaces, allowing users to manage different projects efficiently in one place.

## Features

### Authentication & Security
- JWT-based authentication
- OTP verification for registration and password reset
- Role-based access control with Spring Security
- Rate limiting using Bucket4j and Redis

### Real-time Communication
- **Chat Rooms**: Real-time messaging using WebSockets
- **Voice/Video Rooms**: WebRTC integration via Janus Gateway
- **Notifications**: Real-time push notifications for invites, messages, and events

### Collaboration & Management
- **Workspaces & Local Groups**: Manage different projects efficiently
- **File Sharing**: Secure file upload and sharing using AWS S3
- **Friends & Communities**: Add friends, manage communities, and presence tracking

## Project Structure

```
SpaceHub-Backend/
├── src/main/java/org/spacehub/
│   ├── configuration/ - Security, WebSocket, and Bean configurations
│   ├── controller/ - REST API Endpoints (Auth, Users, ChatRooms, etc.)
│   ├── DTO/ - Data Transfer Objects
│   ├── entities/ - Database models (JPA/Hibernate)
│   ├── ExceptionHandler/ - Global exception handling
│   ├── mapper/ - Object mapping
│   ├── repository/ - Database access layer
│   ├── security/ - JWT and Authentication logic
│   ├── service/ - Business logic
│   └── utils/ - Helper utilities
├── src/main/resources/
│   ├── application.properties - Core configuration
│ 
└── pom.xml - Maven dependencies
```

## API Configuration

**Base URL**: `http://localhost:8080` (Local Development)

### Endpoints (Examples)

1. **User Login**
   - Route: `/api/v1/login`
   - Method: POST
   - Body: `{ "email": "user@example.com", "password": "yourpassword" }`

2. **User Registration**
   - Route: `/api/v1/registration`
   - Method: POST
   - Body: `{ "email": "user@example.com", "username": "user", "password": "yourpassword" }`

3. **Verify OTP**
   - Route: `/api/v1/validateregisterotp`
   - Method: POST
   - Body: `{ "email": "user@example.com", "otp": "123456" }`

## Setup

1. Clone the repository and navigate to the project directory
2. Create a `.env` file in the root directory and configure environment variables (DB, AWS, Redis, Email)
3. Ensure PostgreSQL and Redis are running locally (or update the `.env` with your remote instances)
4. Open the project in your preferred IDE (IntelliJ IDEA, Eclipse)
5. Run the application using Maven: `./mvnw spring-boot:run` or via your IDE's Run configuration

## Infrastructure & Security

- **Database**: PostgreSQL with Hibernate Spatial support
- **Caching**: Redis for performance optimization
- **File Storage**: AWS S3 for scalable and secure file storage
- **Email Service**: Spring Boot Mail for OTP and notifications
- **WebRTC**: Janus Gateway for handling complex audio/video routing

## Requirements

- Java 17+
- Maven 3.8+
- PostgreSQL
- Redis

## Notes

- The app uses Spring Boot 3.5.x
- WebSocket endpoints are secured and require valid JWT tokens
- Voice and Video rooms rely on a deployed Janus WebRTC Gateway
- Uses Redisson for distributed locks and rate-limiting
- Use standard REST conventions; all main APIs are under `/api/v1/`
