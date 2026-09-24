---
name: effective-java
description: Java class and API design standard for this Spring Boot + JPA + Vaadin project. Use when creating or changing Java domain classes, value objects, records, DTOs, enums, services, exceptions or public methods, and as the coding standard when two-axis-review checks Java code. Not for Vaadin UI layout or styling (use the Vaadin skills) and not for test process (use tdd).
---

# Effective Java for this project

Modern Java design rules inspired by Effective Java, adapted to Java 21+,
Spring Boot 4, JPA and Vaadin 25. Goals, in order: correctness, clarity,
then performance where it matters.

Be practical, not dogmatic. Apply a rule where it clearly helps. When a
simpler solution works, use it.

## 1. Project exceptions (these override every rule below)

- **JPA entities** (`@Entity`): not `final`, keep a protected no-arg
  constructor, mutable fields allowed. Never apply immutability or
  `final class` to entities. No Lombok `@Data` on entities. Base
  `equals`/`hashCode` on the id only if the project already does so;
  otherwise leave them unimplemented.
- **Spring beans** (`@Service`, `@Component`, `@Repository`,
  `@Configuration`): never `final`, no `final` public methods (CGLIB proxies
  break `@Transactional`, `@Cacheable`, `@Async`). Constructor injection.
- **Vaadin forms**: beans edited through `Binder` need getters and setters.
  Use immutable records for read-only display data.
- **Vaadin threading**: UI changes from background threads go through
  `ui.access(...)`. Never touch components outside the UI lock.
- **Serialization**: ignore serialization advice, unless Vaadin session
  replication or persistence is enabled. Then keep view state `Serializable`
  and avoid holding non-serializable references in views.
- **Nullability**: use JSpecify (`@NullMarked` on packages, `@Nullable` where
  needed). Do not mix in other `@Nullable` annotations.

## 2. Creating objects

- Value objects and DTOs: use a `record`. Validate in the compact constructor.
- Use a static factory (`of`, `from`, `create`) when a name clarifies intent
  or when returning a cached or subtype instance.
- Use a Builder only when there are more than about 5 optional parameters.
  Never write telescoping constructors.
- No hand-written singletons. Spring beans are singletons already.

## 3. Class design

- Minimize visibility: package-private by default, `public` only for what
  other packages use.
- Prefer immutability for non-entity classes: `final` fields, no setters,
  defensive copies of mutable inputs (`List.copyOf`, `Map.copyOf`).
- Composition over inheritance. Extend a class only if it was designed for it.
- Do not create an interface for every service. Add an interface when there
  are two or more real implementations, or at a real boundary (external
  system, port you want to swap in tests).
- Model closed variations with `sealed` interfaces and records, handled by
  `switch` with pattern matching. Prefer this over class hierarchies with
  `instanceof` chains or type flags.
- Use `enum` instead of int or String constants. Put behavior on the enum
  when each constant behaves differently. Use `EnumSet`/`EnumMap`.

## 4. Methods

- Validate parameters at public boundaries: `Objects.requireNonNull`,
  Bean Validation on DTOs, clear `IllegalArgumentException` messages.
- Return empty collections, never `null`.
- Use `Optional` only as a return type for "maybe absent". Not for fields,
  parameters or collections. Never call `Optional.get()`; use `orElseThrow`,
  `orElse`, `map`.
- Keep parameter lists short (max ~4). Group related parameters into a record.
- Avoid overloads with the same number of parameters and different meanings.

## 5. Generics, lambdas and streams

- No raw types. Use bounded wildcards for flexible APIs
  (`? extends T` in, `? super T` out).
- Prefer lambdas and method references over anonymous classes. Use standard
  functional interfaces (`Function`, `Supplier`, `Predicate`) before inventing
  new ones.
- Use streams where they read better than a loop. Keep them free of side
  effects. A plain loop is fine.
- Return `List`/`Collection` from public methods, not `Stream`.

## 6. Exceptions

- Exceptions are for exceptional conditions, not control flow.
- Unchecked exceptions for programming errors and business rule violations.
  Checked exceptions only when the caller can realistically recover.
- Translate exceptions at layer boundaries (e.g. a persistence exception
  becomes a domain exception in the service). Keep the cause.
- Include useful context in messages (ids, values), never secrets.
- Never catch `Exception` or `Throwable` broadly and swallow it. Never leave
  an empty `catch` block.

## 7. General

- Money: `BigDecimal` (with explicit scale and rounding) or `long` minor
  units. Never `double`/`float`.
- Minimize variable scope. Use `var` when the type is obvious from the
  right-hand side.
- Use `StringBuilder` or `String.join` instead of `+` in loops.
- Use the standard library before writing utilities.
- Concurrency: prefer `java.util.concurrent` types and Spring's executors.
  No `wait`/`notify`. Rely on bean scopes instead of hand-rolled lazy
  initialization.

## 8. Design patterns in this stack

Use the mechanism the framework already provides. Do not hand-write the
classic pattern next to it. Name classes after domain concepts, not pattern
roles (no `PaymentStrategyContext`).

| Need | Use in this project |
|---|---|
| Single instance | Spring bean (default scope) |
| Proxy, cross-cutting concerns | Spring AOP: `@Transactional`, `@Cacheable`, `@PreAuthorize` |
| Observer, events | Vaadin listeners; Spring `ApplicationEvent` between features |
| Strategy, Command | Functional interface + lambda, or several beans of one interface injected as `List<T>` / `Map<String, T>` |
| Template Method | Callbacks (`JdbcClient`, `RestClient`, `TransactionTemplate`) |
| Factory | Static factory method, or `@Bean` method in configuration |
| State | `enum` with behavior, or `sealed` interface |
| Visitor | `sealed` interface + `switch` with pattern matching |
| Composite | Vaadin component tree |
| Builder | `record` first; Builder only above ~5 optional params |

Only introduce a pattern when there is a concrete, present need. "It might
change later" is not a need.

## 9. Standards checklist (used by two-axis-review)

When `two-axis-review` checks Java code against this standard, check these and
report only real problems. Label anything else as optional.

1. Section 1 exceptions respected (entities, beans, Binder beans, `ui.access`).
2. No field injection; transactions on service methods, not views.
3. Records for value objects and DTOs; no telescoping constructors.
4. Visibility minimized; no unnecessary interfaces or abstraction layers.
5. No `null` collections returned; `Optional` used only as return type.
6. No raw types; no `Optional.get()`.
7. Exceptions not swallowed; translated at boundaries with the cause kept.
8. No `double`/`float` for money.
9. No hand-written singletons or pattern structures duplicating framework features.
10. Names follow `CONTEXT.md` vocabulary.

If the code is correct, say so first. Do not invent problems to seem thorough.