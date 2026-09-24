---
name: java-development
description: Baseline Java 24, Spring Boot and Maven rules for this project. Use for any Java/Maven change together with effective-java and tdd.
---

# Java development

Use this skill for Java, Spring Boot and Maven work in this project.

- Target Java 24.
- Prefer small domain types with focused JUnit/AssertJ tests.
- Keep package structure feature-oriented.
- Use Spring integration tests only when Spring behavior is under test.
- Do not introduce dependencies without a clear use and, when relevant, a decision note.
- Run `mvn test` before finishing.
