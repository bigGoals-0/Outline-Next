# Outline Next

Outline Next is a Java 25 desktop chat MVP with a Spring Boot backend, JavaFX desktop client, SQLite development storage, PostgreSQL production configuration, file uploads, token sessions, friend requests, direct messages, and a dark commercial-style UI.

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
./mvnw -pl client javafx:run
```

Use register from the login screen to create users, then add friends by username from the main search bar.

## API Smoke Flow

```bash
curl -s -X POST http://localhost:8080/api/auth/register -H 'Content-Type: application/json' -d '{"username":"ada","password":"password123","displayName":"Ada"}'
curl -s http://localhost:8080/actuator/health
```

Authenticated endpoints require `X-Session-Token`.

## Deployment

Render deployment is configured in `render/render.yaml`; the backend Docker image uses `SPRING_PROFILES_ACTIVE=prod` and expects PostgreSQL environment variables:

- `DATABASE_URL`
- `DATABASE_USERNAME`
- `DATABASE_PASSWORD`
- `OUTLINE_UPLOAD_DIR`

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
