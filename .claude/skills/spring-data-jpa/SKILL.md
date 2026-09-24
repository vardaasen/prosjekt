---
name: spring-data-jpa
description: Persistence rules for Spring Data JPA and Hibernate in this Spring Boot + Vaadin project. Use when creating or changing entities, associations, repositories, JPQL queries, projections, pagination, or services that load data for Vaadin grids and forms. Not for SQL schema design or migrations beyond entity mapping.
---

# Spring Data JPA for this project

**Project boundary (decision note 0004 wins over anything below):** domain
objects have no JPA annotations. `@Entity` classes and Spring Data
repositories are internal to the persistence adapter
(`marketplace/persistence`) and are translated by a Data Mapper. Services and
Vaadin views work with domain types through the seams (for example
`PublishedListingCatalogue`), never with entities or repositories directly.
Flyway is the only schema owner.

Goal: every query is predictable. No N+1 selects, no in-memory pagination,
no lazy loading surprises in the UI.

## 1. Required configuration

These belong in `application.properties` (or `.yml`). Check they are present
before writing persistence code; add them if missing.

```properties
# Views must not trigger lazy loading. Load what the view needs in the service.
spring.jpa.open-in-view=false

# Fail fast instead of silently paginating in memory.
spring.jpa.properties.hibernate.query.fail_on_pagination_over_collection_fetch=true

# Schema is owned by migrations, not Hibernate.
spring.jpa.hibernate.ddl-auto=validate
```

With `open-in-view=false`, a `LazyInitializationException` in a view is a
signal that the service did not load the data the view needs. Fix the service
query. Never re-enable open-in-view to hide it.

## 2. Entity mapping

- All associations are lazy. `@ManyToOne` and `@OneToOne` are EAGER by
  default in JPA, so always write `fetch = FetchType.LAZY` on them.
- Prefer unidirectional `@ManyToOne`. Add a `@OneToMany` collection only when
  the aggregate really owns its children.
- Collections that are fetched together should be `Set`, not `List`.
  Hibernate cannot `JOIN FETCH` two `List` collections in one query
  (`MultipleBagFetchException`).
- On bidirectional associations, keep both sides in sync with helper methods
  (`addComment`, `removeComment`) on the owning entity.
- IDs on PostgreSQL: prefer `GenerationType.SEQUENCE` over `IDENTITY`
  (IDENTITY disables JDBC insert batching).
- Add `@Version` to entities edited in Vaadin forms, so concurrent edits fail
  with an optimistic lock error instead of silently overwriting.

## 3. Querying

- Read-only lists (grids, dropdowns, reports): use a **record projection** or
  interface projection, not entities. Select only the columns the view shows.
- Loading an entity with its children: `JOIN FETCH` or `@EntityGraph` in one
  query. Never loop over parents and touch a lazy collection per parent.
- As a safety net against N+1 on lazy associations, set
  `spring.jpa.properties.hibernate.default_batch_fetch_size=50` (ADJUST).
  This reduces damage; it does not replace correct fetching.
- Never call `findAll()` without paging on tables that can grow.
- Prefer derived query methods for simple lookups, `@Query` with JPQL for
  anything with joins or projections. Native SQL only when JPQL cannot
  express it, and say why in a comment.
- Bulk updates or deletes: `@Modifying` `@Query` in a `@Transactional`
  service method. Remember that bulk queries bypass the persistence context.

## 4. Pagination with child collections

`JOIN FETCH` combined with paging makes Hibernate load every row and page in
memory (log warning `HHH000104`, or an exception with the config above).
Use two queries: page the parent ids, then fetch those parents with children.

```java
public interface PostRepository extends JpaRepository<Post, Long> {

    @Query("select p.id from Post p")
    Page<Long> findIds(Pageable pageable);

    @Query("select distinct p from Post p left join fetch p.comments where p.id in :ids")
    List<Post> findAllWithCommentsByIdIn(@Param("ids") Collection<Long> ids);
}
```

```java
@Service
public class PostService {

    private final PostRepository posts;

    public PostService(PostRepository posts) {
        this.posts = posts;
    }

    @Transactional(readOnly = true)
    public Page<Post> findWithComments(Pageable pageable) {
        Page<Long> ids = posts.findIds(pageable);
        if (ids.isEmpty()) {
            return Page.empty(pageable);
        }
        Map<Long, Post> byId = posts.findAllWithCommentsByIdIn(ids.getContent()).stream()
                .collect(Collectors.toMap(Post::getId, Function.identity()));
        // The second query does not keep the page order; restore it from the id page.
        List<Post> ordered = ids.getContent().stream().map(byId::get).toList();
        return new PageImpl<>(ordered, pageable, ids.getTotalElements());
    }
}
```

Use this only when the view really needs the children. For a grid that shows
parent columns plus a count, a projection with `count(...)` and `group by`
is simpler and faster.

## 5. Vaadin grids

Grids load data lazily through the service, one page at a time:

```java
grid.setItems(query -> postService
        .list(VaadinSpringDataHelpers.toSpringPageRequest(query))
        .stream());
```

- The service method takes a `Pageable` and returns a projection page or list.
- `toSpringPageRequest` carries the grid's sort order. Only allow sorting on
  columns backed by an indexed database column.
- Never `grid.setItems(repository.findAll())` on tables that can grow.

## 6. Transactions and saving

- Transactions live on service methods: `@Transactional` for writes,
  `@Transactional(readOnly = true)` for reads. Never in views or repositories.
- Managed entities are saved by dirty checking at commit. Calling `save()` on
  an entity loaded in the same transaction is unnecessary.
- Keep transactions short. No remote calls or user interaction inside them.
- Handle `ObjectOptimisticLockingFailureException` in the service or view with
  a clear message to the user (reload and retry), not a stack trace.

## 7. Verifying

- When adding or changing a query, check the generated SQL once in dev:
  `logging.level.org.hibernate.SQL=debug`.
- Watch the log for `HHH000104` and for repeated identical selects (N+1).
- Test repositories with `@DataJpaTest` against the real database engine
  (Testcontainers), not H2, when queries use database-specific features.

## 8. Review checklist (used by two-axis-review)

1. `open-in-view=false` and the pagination fail-fast setting are present.
2. `@ManyToOne`/`@OneToOne` explicitly lazy.
3. No lazy collection access in loops or in views.
4. No `JOIN FETCH` combined with `Pageable` on collections.
5. Grids use lazy `setItems(query -> ...)`, never `findAll()`.
6. Read-only lists use projections, not full entities.
7. Transactions on service methods only; reads are `readOnly = true`.
8. Form-edited entities have `@Version`.

Report only real problems. Label anything else as optional.