# Project instructions

<!-- Keep this file short: it is loaded on every request. Detailed rules live
     in skills. -->

## Stack

- Java 24, Maven wrapper (`./mvnw`)
- Spring Boot 4; Spring Data JPA / Hibernate introduseres med
  persistensskiven
- Vaadin 25 Flow: UI written in Java. Not Hilla, not React.
- Database: PostgreSQL; Flyway-migreringer introduseres med første
  databaseskjema
- Tests: JUnit 5, AssertJ, Spring MockMvc; Testcontainers med PostgreSQL når
  persistens introduseres

## Verify every change

- Run `./mvnw test` after changes and fix failures before reporting done.
- Run `./mvnw verify` when touching integration tests, security or configuration.
- Never report a task as done if the build or tests fail. Say what fails instead.

## Where to look things up

- Vaadin APIs, components and styling: use the Vaadin MCP server. Do not guess
  Vaadin APIs from memory; versions change.
- Domain terms: read `CONTEXT.md`. Use its vocabulary for class, method and
  variable names. Decisions are recorded in `docs/decisions/`.

## Architecture

- Package by feature (e.g. `no.fagskolen.prosjekt.marketplace`), not by
  technical layer.
- Flow: View -> application service -> catalogue seam -> persistence adapter.
  Views never call persistence adapters or Spring Data repositories directly.
- Services own transactions: `@Transactional` on write methods,
  `@Transactional(readOnly = true)` on read methods. No transactions in views.
- Keep business rules out of views. Views handle layout, binding and navigation.
- Keep domain objects independent of JPA annotations and persistence concerns.
  The persistence adapter owns JPA entities and maps them to and from domain
  objects; controllers, views and domain services never receive JPA entities.
- Introduce the catalogue seam before the database adapter: its interface
  exposes only the use cases that exist now—find published annonser, search
  published annonser and find one published annonse by slug. The current
  in-memory implementation and later JPA implementation are adapters at this
  seam.
- Use a Data Mapper at the persistence seam: a dedicated mapper converts
  between the domain model and persistence entities. Do not make a domain
  record double as a JPA entity and do not expose Spring Data repositories
  outside the persistence adapter.

## Vaadin rules

- Prefer `Composite<...>` over extending layout classes.
- Grids over large tables use lazy loading: `setItems(query -> ...)` with
  `VaadinSpringDataHelpers.toSpringPageRequest(query)`. Never load a whole table.
- UI updates from background threads must go through `ui.access(...)`.
- Secure every view with `@RolesAllowed`, `@PermitAll` or `@AnonymousAllowed`.
- Style with Lumo theme variables and utility classes; no hard-coded colors.
- When implementing Figma designs, map them to Vaadin components and Lumo
  variables. Do not produce HTML/CSS or React.
- Never make views singleton beans. Keep per-user state out of singletons.

## Spring rules

- Constructor injection only. No `@Autowired` on fields.
- Typed configuration with `@ConfigurationProperties` records.
- No secrets, passwords or environment-specific URLs in `application.properties`
  or `application.yml`. Use environment variables.
- In tests, use `@MockitoBean` (not the removed `@MockBean`).

## Working style

- Before larger changes (new feature, new entity, cross-package refactor),
  use the `grill-with-docs` skill to align on the design first.
- Implement with the `tdd` skill: failing test first, small vertical slices.
- Hard bugs: use the `diagnosing-bugs` skill.
- Reviews: use the `code-review` skill. The Java coding standard it checks
  against is the `effective-java` skill.
- Class and API design: follow the `effective-java` skill.
- Do not add new dependencies without asking first.
- Do not introduce design patterns, interfaces or abstraction layers without a
  concrete, present need. Simple code wins.
- Entities, repositories, queries and grid data loading: follow the `spring-data-jpa` skill.
- PostgreSQL schema, indexes, queries and database configuration: follow the
  `postgres-best-practices` skill alongside `database`.