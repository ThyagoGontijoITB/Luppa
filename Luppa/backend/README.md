# Luppa API

Spring Boot REST API for Luppa.

## Required Environment

```text
DB_HOST=localhost
DB_PORT=1433
DB_NAME=luppaBD
DB_USERNAME=luppa_app
DB_PASSWORD=<configured in the local environment>
JWT_SECRET=replace-with-at-least-32-characters
FRONTEND_URL=http://localhost:5173,http://127.0.0.1:5173
IMAGE_STORAGE_DIR=uploads/events
IMAGE_PUBLIC_BASE_URL=http://localhost:8081/uploads/events
PROFILE_IMAGE_STORAGE_DIR=uploads/profile
PROFILE_IMAGE_PUBLIC_BASE_URL=http://localhost:8081/uploads/profile
ADMIN_EMAIL=admin@luppa.com
ADMIN_PASSWORD=change-this-password
ADMIN_NAME=Luppa Admin
ADMIN_PHONE=11999999999
ADMIN_DATE_OF_BIRTH=1990-01-01
```

## Run

```bash
mvn spring-boot:run
```

Swagger UI:

```text
http://localhost:8081/swagger-ui.html
```

## Notes

- SQL Server is configured through environment variables.
- The existing SQL Server schema is authoritative; Hibernate runs with `ddl-auto=validate`.
- Flyway is disabled for this live database integration.
- Passwords are stored as BCrypt hashes.
- Public registration allows only `USER` and `CREATOR`; `ADMIN` must be bootstrapped or created administratively.
- Event image binaries are stored in local filesystem for development. SQL Server stores `evento.pathImagemCapa`.
- Profile image binaries are stored in local filesystem for development. SQL Server stores `usuario.pathImagemPerfil`.
- Geocoding is behind a service abstraction. The current implementation is a no-op placeholder for a provider-specific implementation.
- Production deployment requirements are documented in `DEPLOYMENT.md`.
- In production, run with `SPRING_PROFILES_ACTIVE=prod`; Swagger/OpenAPI is disabled, SQL Server certificate validation is required, and unsafe localhost/placeholder settings fail startup.
- Public endpoint rate limiting is in-memory for the initial single-instance TCC deployment. It is reset on restart and is not shared between multiple API instances.
