# Database

Use this skill when adding persistence, schema migrations or database-backed tests.

- Choose open, locally runnable services first.
- Prefer PostgreSQL for production-like relational needs unless a decision note chooses otherwise.
- Use migrations from the first schema change.
- Use Testcontainers or a documented local service for integration tests.
- Keep repositories/services testable without UI dependencies.
