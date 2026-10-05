# Design decisions

## Sessions rather than a browser-stored bearer token

Spring Security authenticates form-encoded credentials and maintains an HTTP-only session cookie. The browser does not put credentials or bearer tokens in localStorage. A 15-minute inactive-session timeout and SameSite=Strict cookie setting suit this local demonstration. Login uses Spring's session-fixation protection; CSRF tokens are refreshed after authentication and logout.

## Explicit SQL and schema migrations

Flyway creates a small accounts table. JDBC makes the query and storage boundaries visible, with prepared-statement parameters rather than concatenating user input. A normalized username is the primary key. The application catches database duplicate-key errors instead of relying on a check-then-insert sequence that races.

## Password boundaries

BCrypt performs salted hashing; neither plaintext nor hashes are returned by the account API. The server validates both character length and UTF-8 byte length, because BCrypt limits inputs to 72 bytes. Validation runs on the server even when the browser already checked the input. No password complexity claim or password-reset feature is implied.

## CSRF and browser rendering

The browser first requests a CSRF token tied to its session, then includes that token in POST requests. The token is not a replacement for authentication. A 403 response refreshes the token and asks the user to submit again instead of automatically repeating a potentially consequential request.

A Content Security Policy restricts script and stylesheet sources to this application. No third-party fonts or scripts are used. Account text is rendered with `textContent`; display names are not interpreted as HTML. This protection depends on continuing to avoid unsafe rendering patterns.

## Deliberate limits

Username availability is visible at registration; login failures remain generic. Rate limits, account lockout and abuse controls are not implemented, so do not expose this demo as a public identity service. The SQL store is ephemeral H2, with no PostgreSQL portability claim. Automated tests verify functional safeguards, not a complete security audit.
