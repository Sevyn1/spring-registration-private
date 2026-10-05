# Spring registration project — private working copy

Independent private snapshot of a Java 17 / Spring Boot registration and login project that Favour Ojo reports contributing to. This is not a claim of sole authorship or a production-ready authentication service.

## Provenance

Source recovered from a local checkout whose original remote was `https://github.com/Alanlands1/springbootBackend.git`. Existing source credits remain intact. Original Git history, collaborator settings, IDE metadata and saved connection credentials were excluded. No license was found; keep this copy private pending permission for any public redistribution.

## Running

Install Java 17 and Maven, create a local PostgreSQL database, and set `DB_URL`, `DB_USERNAME` and `DB_PASSWORD` in the process environment. Run `mvn spring-boot:run` from `demo/`. Never commit credentials.

## Review status

This snapshot preserves the recovered implementation. It has not been certified for deployment. Before using it as a public portfolio example, review CSRF handling, registration validation, duplicate users, password-hash exposure in responses, and integration tests. A new repository history records this recovery now; it does not backdate work or establish individual authorship of every file.
