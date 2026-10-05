# Verification — 5 October 2026

## Automated checks

Java 17 `mvn verify`: **18 integration test cases passed**, with no errors, failures or skipped cases. The application packaged as an executable Spring Boot JAR. JavaScript passed `node --check`.

Tests cover encoded storage and response privacy; case-insensitive duplicate names; invalid usernames, blank display names, short and multibyte-overlong passwords, missing fields and malformed JSON; CSRF enforcement on signup and login; unauthenticated profile access; generic login failures; a full issued-token/signup/login/profile/token-refresh/logout sequence; protected data cache headers; public assets and browser security headers. SQL migrations run in the test context.

## Browser smoke check

The packaged application was launched on loopback port 8084. With fictional details, the browser created an account, displayed signup success, signed in, rendered the authenticated profile, signed out, and returned to the sign-in form. Registration and dashboard previews were inspected.

## Limits

The browser check is one successful flow, not exhaustive automation. No deployed environment, real accounts, concurrent load, penetration test, PostgreSQL compatibility or mobile-device coverage is claimed. The dependency refresh is not a full vulnerability assessment.
