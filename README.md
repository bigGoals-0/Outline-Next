# Outline Chat

Outline Chat is a Java 25 desktop chat MVP with a Spring Boot backend, JavaFX desktop client, SQLite storage, file uploads, token sessions, friend requests, direct messages, and a dark commercial-style UI.

## Build

```bash
./mvnw clean verify
```

The wrapper downloads Maven into `.mvn/` the first time it runs.

## Run Locally

Start the backend:

```bash
./mvnw -pl server spring-boot:run
```

Start the JavaFX client in a second terminal:

```bash
./mvnw -pl client javafx:run -Doutline.server=http://localhost:8080
```

By default the desktop client targets the production backend:

```text
https://outline-next-server.onrender.com
```

Override the backend URL for local development or staging with either:

```bash
OUTLINE_SERVER_URL=http://localhost:8080 ./mvnw -pl client javafx:run
./mvnw -pl client javafx:run -Doutline.server=http://localhost:8080
```

Use register from the login screen to create users, then add friends by username from the main search bar.

## API Smoke Flow

```bash
curl -s -X POST http://localhost:8080/api/auth/register -H 'Content-Type: application/json' -d '{"username":"ada","password":"password123","displayName":"Ada"}'
curl -s http://localhost:8080/actuator/health
```

Authenticated endpoints require `X-Session-Token`.

## Deployment

Render deployment is configured in root-level `render.yaml` and `render/render.yaml`. The backend Docker image uses `SPRING_PROFILES_ACTIVE=prod` and runs on SQLite for a single free Render Web Service.

Render environment variables:

- `SPRING_PROFILES_ACTIVE=prod`
- `OUTLINE_UPLOAD_DIR=/var/outline/uploads`

The configured service name is `outline-next-server`, which Render exposes at:

```text
https://outline-next-server.onrender.com
```

## Packaging

macOS app image:

```bash
scripts/package-macos.sh
```

Windows installer from PowerShell:

```powershell
scripts\package-windows.ps1
```

Both scripts require `jpackage`, included with the JDK.
