# Spring Registration Lab

Java 17 / Spring Boot registration and session-based login demonstration. Includes Spring Security, BCrypt password hashing, Jakarta Validation, JPA, Thymeleaf and an isolated H2 database for local practice.

## Run locally

From `demo/`, run `./mvnw spring-boot:run` with Java 17 installed. Open `http://127.0.0.1:8083/req/signup`. Create an account with fictional information, sign in and visit the protected dashboard. The default in-memory database is cleared when the application stops.

Run `./mvnw verify` to execute integration tests and package the application. Windows users can use `mvnw.cmd`.

Optional PostgreSQL configuration: use the `postgres` Spring profile and supply `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` through the process environment. This profile validates an existing schema; no database migrations are provided, and this path has not been integration-tested against PostgreSQL.

## Behavior and engineering decisions

- Usernames contain 3–30 letters, numbers or underscores and are normalized to lowercase; a database constraint enforces uniqueness. Duplicate registration returns HTTP 409.
- Email and password are validated before persistence. Passwords must contain 12–72 characters and fit within BCrypt’s 72-byte input limit.
- Registration returns HTTP 201 with only the generated ID and username. Stored password hashes and email addresses are excluded from the response.
- CSRF protection applies to registration, login and logout. Signup sends the rendered token with its JSON request; login/logout use Thymeleaf form tokens.
- The browser prevents repeated submission, checks confirmation and reports success, validation, duplicate and connection failures.
- `/index` requires authentication. Login errors use a generic message.

## Scope

A local learning and portfolio demonstration, not a deployed identity service. It does not implement email verification, account recovery, rate limiting, MFA, production database migrations or an operations/security review. No real user data or credentials belong in this repository.

## Provenance and authorship

Recovered from a local shared-project checkout whose original remote was `https://github.com/Alanlands1/springbootBackend.git`. Favour Ojo reports contributing to that project; this does not establish sole authorship of the recovered implementation. Original history, collaborator settings, IDE files and saved connection credentials were excluded.

The 2026 review added validation, safe response DTOs, uniqueness handling, CSRF protection, corrected form behavior, an isolated local configuration, refreshed dependencies, automated tests and replacement demo pages/styles. Those changes were implemented with Codex assistance and verified through the checks documented below.

No license was found in the recovered files or through the upstream license endpoint. Keep this repository private until permission to redistribute the shared source is confirmed. A fresh commit history records recovery and maintenance now; it does not backdate work.

## Verification

See [review results](REVIEW.md). The integration suite covers registration, response privacy, BCrypt storage, duplicate usernames, invalid email/password/username, malformed JSON, CSRF enforcement, login success/failure, protected pages, rendered tokens and logout.
