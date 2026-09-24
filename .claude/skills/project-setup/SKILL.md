---
name: project-setup
description: Initialize, repair or extend the project foundation (Java 24, Maven, IntelliJ, Vaadin under /app, server-rendered SEO pages, local Compose demo).
---

# Prosjekt setup

Use this skill when initializing, repairing or extending the project foundation for `prosjekt`.

## Rules

- Java 24 is the project standard.
- IntelliJ IDEA is the primary IDE; VS Code is backup/agent reference.
- Vaadin Flow is used for app/workflow UI, mapped under `/app` (including `/app/admin`).
- Public SEO pages are server-rendered Java/Spring pages.
- Open components and services are default. Paid Vaadin Enterprise components require a decision note.
- `DesignMarketPlace/` is ignored design reference and must not be added to Git.
- TDD is mandatory for functional changes.

## Setup checklist

1. Confirm `.gitignore` excludes `DesignMarketPlace/`, `.idea/`, build outputs and local secrets.
2. Keep `AGENTS.md` aligned with commands and architecture.
3. Keep `.github/agents/tdd*.agent.md` aligned with Maven/JUnit.
4. Keep `.devcontainer/devcontainer.json` on Java 24 and Node 24.
5. Add or update `docs/decisions/` whenever a technology choice affects license, architecture, SEO, database or services.
6. Run `mvn test` after code changes.
