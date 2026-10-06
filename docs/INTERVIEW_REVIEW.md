# Interview review

Explain this as an October 2026 learning project built with AI assistance. Review the implementation before describing it as work you understand. Do not claim professional identity-platform ownership or a production deployment.

- Trace registration from `AccountApi` through validation and `Accounts` to `AccountStore` and the migration.
- Explain why database uniqueness is necessary even if an application checks whether a username exists.
- Explain the difference between password hashing and encryption, and why BCrypt's byte limit matters.
- Trace a real session from `/api/csrf` to signup, login, token refresh, `/api/me` and logout.
- Explain why a session cookie needs CSRF protection and why HTTP-only does not solve every browser security issue.
- Show which tests exercise the real security filters and SQL store. Describe what remains untested.
- Explain how AI output was reviewed: check API responses for secrets, add invalid-input cases, run the integration suite, and verify the browser flow.
- Discuss what you would add before deployment: throttling, recovery, HTTPS, secure cookies, persistent storage, operations and abuse monitoring.

- Explain the consolidation decision: one backend and schema, with email registration reimplemented and profile/password workflows added.
- Trace a profile edit from authenticated principal through password confirmation to parameterized SQL.
- Explain how V2 preserves V1 accounts and why stale password writes are rejected.
- Explain the difference between changing a password, account recovery and revoking every existing session. Only the first is implemented.
