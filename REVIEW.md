# Review — 5 October 2026

## Completed

- `mvn verify`: 13 integration tests passed, no failures/errors/skips; executable Spring Boot JAR packaged successfully with Java 17.
- Replaced direct entity binding with a validated registration request and minimal response record. Password hashes and emails do not appear in successful registration responses.
- Added normalized usernames, database uniqueness and HTTP 409 handling.
- Restored CSRF protection, fixed form submission/feedback and login processing, and added token-bearing logout.
- Replaced placeholder pages with a focused local registration demo.
- Updated Spring Boot to 3.5.16, added the complete Maven wrapper, isolated the default H2 runtime, and provided environment-only PostgreSQL configuration.
- Added continuous-integration verification.

## Tested scenarios

Registration response privacy and encoded storage; CSRF rejection; case-normalized duplicate names; invalid emails, short and multibyte-overlong passwords, invalid usernames and malformed JSON; correct/incorrect login; protected dashboard; rendered CSRF tokens; logout CSRF.

## Limits

Tests exercise the Spring MVC, security, JPA and H2 layers. PostgreSQL and interactive browser behavior have not been tested in this review. No deployment, email sending or production readiness is claimed. Dependency version refresh is not a complete vulnerability assessment.

## Public release gate

The source derives from a shared project, and no redistribution license was found. Preserve provenance and confirm permission before making the repository public. Alternatively, prepare a separate implementation from a written functional specification. Public visibility would allow the original owner to read the repository.
