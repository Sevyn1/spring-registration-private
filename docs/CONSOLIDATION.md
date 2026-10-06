# Consolidation — October 6, 2026

## Active repository

`Sevyn1/spring-registration-private` is the maintained home of the updated account app. The root contains one Spring Security configuration, JDBC account store, Flyway migration history and responsive browser interface. The earlier `demo/` project remains available in historical commits, rather than as a second active application.

The merge retains both Spring Registration and Account Access Lab commit histories. Neither history was rewritten or backdated. Complete-history Git bundles were verified before the repository changes. The newer Account Access Lab repository is superseded; deletion, if completed, does not remove its history from this repository or the saved backup.

## What was combined

Spring Registration offered validated email registration, CSRF-protected sessions and an authenticated dashboard. Account Access Lab supplied explicit JDBC boundaries, migrations, normalized username uniqueness, private API responses and a separate JavaScript interface. The maintained app implements email validation and storage afresh, adds profile editing and password changes, and uses one backend rather than competing JPA/JDBC stacks.

## Provenance

The earlier source derived from `Alanlands1/springbootBackend`. Favour Ojo reports contributing; sole authorship is not claimed. The current root implementation and maintenance were developed with Codex assistance in October 2026. Historical shared source is not relicensed by the current application's MIT license. The repository remains private because no redistribution license was confirmed for that historical source.
