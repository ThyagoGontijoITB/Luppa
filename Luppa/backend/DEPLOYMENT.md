# Luppa TCC Deployment Notes

This document covers the initial TCC deployment only. It does not enable Flyway and does not modify the SQL Server schema.

## Production profile

Run the API with:

```bash
SPRING_PROFILES_ACTIVE=prod
```

The `prod` profile fails startup when required production settings are missing or unsafe.

For Render Docker deployment, the image defaults to `SPRING_PROFILES_ACTIVE=prod`
and reads Render's `PORT` environment variable. If `PORT` is not set, the API
keeps the local default port `8081`.

Generate `JWT_SECRET` with at least 32 random bytes, for example:

```bash
openssl rand -base64 32
```

Never commit the generated value.

## Required production environment

Use `backend/.env.example` as the checklist. Production values must use real HTTPS public URLs and must not point to localhost:

- `PORT` is supplied by Render automatically.
- `DB_HOST`
- `DB_PORT`
- `DB_NAME`
- `DB_USERNAME`
- `DB_PASSWORD`
- `CLOUDINARY_CLOUD_NAME`
- `CLOUDINARY_API_KEY`
- `CLOUDINARY_API_SECRET`
- `JWT_SECRET`
- `MAIL_HOST`
- `MAIL_PORT`
- `MAIL_USERNAME`
- `MAIL_PASSWORD`
- `LUPPA_EMAIL_FROM`
- `LUPPA_SUPPORT_EMAIL`
- `FRONTEND_URL`
- `APP_BASE_URL`
- `LUPPA_TERMS_URL`
- `LUPPA_PRIVACY_URL`
- `IMAGE_PUBLIC_BASE_URL`
- `PROFILE_IMAGE_PUBLIC_BASE_URL`
- `VITE_API_BASE_URL` in the frontend build environment, unless frontend and API are intentionally served from the same origin.

The Docker image sets these internal defaults, but production should mount them
on persistent Render storage if uploads must survive deploys and restarts:

- `IMAGE_STORAGE_DIR=/app/uploads/events`
- `PROFILE_IMAGE_STORAGE_DIR=/app/uploads/profile`

Optional production settings:

- `JWT_EXPIRATION_MINUTES`
- `MAIL_SMTP_AUTH`
- `MAIL_SMTP_STARTTLS_ENABLE`
- `LUPPA_EMAIL_LOGO_URL`
- `RATE_LIMIT_ENABLED`
- `TRUSTED_PROXY_CIDRS`
- `GEOCODING_PROVIDER`
- `GEOCODING_BASE_URL`
- `CLOUDINARY_ROOT_FOLDER`

## Cloudinary image storage

Profile images and event cover images are uploaded to Cloudinary. The existing
SQL Server columns keep storing a string reference, now the Cloudinary
`public_id`, so no schema migration is required.

Do not delete existing local upload files during deployment. Existing database
rows that still contain legacy filenames continue to be resolved through
`IMAGE_PUBLIC_BASE_URL` and `PROFILE_IMAGE_PUBLIC_BASE_URL`; new uploads use
Cloudinary delivery URLs derived from `CLOUDINARY_CLOUD_NAME`.

## SQL Server

Production JDBC uses:

```text
encrypt=true;trustServerCertificate=false
```

The SQL Server certificate must validate against the JVM trust store. Do not disable encryption or set `trustServerCertificate=true` in production.

Network requirements:

- Do not expose SQL Server directly to the public internet.
- Allow inbound SQL Server traffic only from the API host or private network.
- Keep database credentials in the hosting provider secret manager or environment variable store.

## Existing database provisioning

Hibernate remains `ddl-auto=validate`; Flyway remains disabled.

Apply the existing schema and reference data before starting the app. The repository currently includes these additive scripts:

- `backend/sql/2026-10-04-create-cadastro-pendente.sql`
- `backend/sql/2026-10-06-add-evento-destaque-principal.sql`
- `backend/sql/2026-10-07-add-luppa-admin-role.sql`

The live database must already include the original Luppa schema, reference rows for roles/statuses/categories/report categories, and the SQL Server audit triggers using `SESSION_CONTEXT`. This repository does not currently include a complete baseline schema export or trigger creation script; keep the approved database backup/export procedure as the source of truth for the initial deployment.

## Upload storage

The API stores event and profile images on the filesystem. In production:

- `IMAGE_STORAGE_DIR` and `PROFILE_IMAGE_STORAGE_DIR` must be absolute paths.
- Both paths must be mounted on persistent storage.
- `IMAGE_PUBLIC_BASE_URL` and `PROFILE_IMAGE_PUBLIC_BASE_URL` must point to the deployed API `/uploads/...` paths over HTTPS.

Do not deploy uploads to ephemeral instance storage. If the selected host cannot guarantee persistence for those directories, this blocks the TCC deployment until a persistent volume or equivalent storage is configured.

## Rate limiting

The initial deployment uses in-memory rate limiting for public endpoints. It protects login, password recovery/reset, verification-code flows and public support requests by IP and, where available, e-mail.

Limits are per API instance and reset on restart. They are not shared across multiple instances. For a multi-instance commercial deployment, replace or back this with distributed storage.

`X-Forwarded-For` is ignored unless the remote address matches `TRUSTED_PROXY_CIDRS`.

## Swagger/OpenAPI

Swagger UI and OpenAPI docs are enabled in local development and disabled in `prod`:

```text
springdoc.api-docs.enabled=false
springdoc.swagger-ui.enabled=false
```

The Spring Security chain also denies those paths when the properties are disabled.

## Frontend

For a domain-root deployment:

```text
VITE_APP_BASE_PATH=/
```

For GitHub Pages compatibility:

```text
VITE_APP_BASE_PATH=/Luppa/
VITE_DEPLOY_TARGET=github-pages
```

Configure the hosting provider for React Router SPA fallback so unknown routes serve `index.html`.

Production builds fail if `VITE_API_BASE_URL` points to localhost.

## Tests

Maven tests run with `spring.profiles.active=test`. The test profile defaults to an invalid SQL Server host so integration tests cannot accidentally modify `luppaBD`.

To run database integration tests, provide a separate disposable database with the required schema:

```bash
TEST_DB_URL=jdbc:sqlserver://<test-host>:1433;databaseName=luppa_test;encrypt=true;trustServerCertificate=false
TEST_DB_USERNAME=<test-user>
TEST_DB_PASSWORD=<test-password>
mvn test
```

Automated tests mock e-mail sending and must not use real SMTP credentials.
