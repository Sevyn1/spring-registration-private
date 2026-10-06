# Verification — October 6, 2026

## Automated checks

Java 17 Maven verification: **34 integration test cases passed**, no errors, failures or skipped cases. The application packaged as an executable Spring Boot JAR. JavaScript syntax was checked separately.

The original 18 cases cover password hashing and response privacy, normalized duplicate usernames, input boundaries, malformed JSON, signup/login CSRF enforcement, generic login errors, real issued-token login/logout, private data cache headers and public asset security headers.

The 16 added cases cover required/valid email, private authenticated email access, profile edits restricted to the authenticated principal, wrong-password and invalid-email rejection, authentication/CSRF requirements for both settings endpoints, password changes using a real issued token, invalidation of the current session, rejection of the old password and successful login with the new one, unchanged-password/weak/multibyte limits, stale-write rejection and preserving V1 accounts through V2 migration.

## Browser check

The packaged app ran on loopback port 8084. A synthetic account was created through the actual signup endpoint, then signed in through the browser. Its display name and email were edited with current-password confirmation. Refresh preserved both the session and saved details. The password-settings controls were inspected; the password-change flow itself was exercised by the integration suite. Desktop and phone-sized layouts were reviewed. Sign-out was checked.

## Persistence check

An isolated file-backed H2 instance was started with the `persistent` profile, a fictional account registered, then the process stopped and restarted. Login succeeded after restart and the name/email were preserved. This checks local persistence, not a production database or PostgreSQL deployment.

## Limits

The browser check is a smoke check, not exhaustive automation. Password changes invalidate the current session only; other sessions are not revoked. Email verification, account recovery, MFA, login throttling, production deployment, concurrent load and a security audit are not implemented or claimed. All verification accounts were fictional.

## Repository relocation

The tested application was moved to the root of the older Spring Registration repository, retaining both Git histories. Repository URL and provenance documentation were updated; application behavior was preserved.
