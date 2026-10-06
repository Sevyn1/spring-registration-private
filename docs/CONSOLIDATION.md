# Consolidation — October 6, 2026

Account Access Lab is the active account application. The older private Spring Registration repository is superseded and retained as a recoverable source-history reference.

Both implementations were reviewed before consolidation. The shared project offered validated contact-email registration, Spring Security sessions, CSRF-protected forms, password hashing and an authenticated dashboard. The public application already provided explicit JDBC boundaries, Flyway migrations, private API responses, normalized username uniqueness, session-token refresh and a separate JavaScript interface.

The consolidated implementation uses one Spring Security configuration, one JDBC account store, one migration history and one browser interface. Email validation and private email storage were implemented afresh in this codebase. Profile editing and current-password-confirmed password changes extend the reviewed workflows. Duplicate controllers, JPA entities and competing login pages were not imported.

No shared-project source, recovered templates, styles, saved configurations or contributor history was copied into this public repository. The original shared project is credited as `Alanlands1/springbootBackend`; Favour Ojo reports contributing to it. Current maintenance and consolidation were implemented with Codex assistance. This does not establish sole authorship of the original shared project or backdate 2026 work.

A complete-history Git bundle of the private repository was verified before it was marked superseded. Archiving is reversible. The private repository stays private; no new redistribution claim is made.
