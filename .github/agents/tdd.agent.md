---
name: TDD
description: Orchestrate full TDD cycle for Java/Spring/Vaadin work
tools: ['agent', 'read', 'search', 'execute/runTests']
disable-model-invocation: true
agents: ['TDD Red', 'TDD Green', 'TDD Refactor']
---

Drive every feature or bug fix through red-green-refactor:

1. Invoke **TDD Red** to write failing Maven/JUnit tests only.
2. Invoke **TDD Green** to implement the minimal Java/Spring/Vaadin code and run the relevant Maven tests.
3. Review the test report. If tests fail, ask whether to revise or abort.
4. Invoke **TDD Refactor** only after tests pass.
5. Summarize changed tests, implementation and verification command.

Respect `AGENTS.md`: open components are the default, SEO public pages stay server-rendered unless a decision note says otherwise, and commercial dependencies require a decision note.
