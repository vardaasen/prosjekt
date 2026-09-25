---
name: intent-grounding
description: Ground every change in Havbruksbrukt's domain, userflows and architecture decisions before writing code, and stop fix-loops. Use at the start of any feature, bug fix or config change in this repo; whenever a fix touches routing, security, Vaadin/Spring integration or Keycloak; when the same files or symptom have been "fixed" more than once; and whenever a fix disables a framework feature, adds an exemption, forwards/redirects around the framework, or hand-rolls something the framework already provides.
---

# Intent grounding

The failure this skill prevents: treating each symptom as a fresh bug, stacking
workarounds, and never noticing that the real cause is an earlier decision that
contradicts the project's own architecture. Tokens go in circles; code gets
worse.

The rule: **no code before a grounding brief.** Documented intent wins over
current code. When they disagree, the disagreement *is* the finding.

## 1. Load intent (read, don't skim)

Read these in order. They are short; read them fully the first time in a
session and re-read the relevant parts before each change.

| Source | What it gives you |
| --- | --- |
| `CONTEXT.md` | Domain language. Use its terms; avoid its `_Avoid_` terms. |
| `docs/decisions/*.md` | Architecture decisions (ADRs). These are constraints, not suggestions. |
| `docs/userflows/seo-public-app.md` and `seo-public-app-diagrams.md` (+ `diagrams/*.mmd`) | Intended user journeys and the status table of what is built vs. not. |
| `AGENTS.md` | Process rules (TDD, `mvn test`, decision notes, pitfalls). |
| `docs/progress.md` | What is delivered and what is next. |
| `docs/security-baseline.md`, `docs/local-development.md` | Security rules and known local gotchas. |

`DesignMarketPlace/` is design reference only (untracked). Use it for intent
and UI, never copy it as production code.

## 2. Write the grounding brief (to the user, before code)

Keep it to a few lines each:

1. **Intent**: Which userflow and step does this serve? Which domain terms
   (from `CONTEXT.md`) are involved? What should the user experience be?
2. **Constraints**: Which ADRs and security rules apply? Quote the sentence.
3. **Conformance**: Does the *current* code/config match those ADRs in the
   area you are touching? Check the actual files (`application.properties`,
   `@Route`s, `SecurityConfiguration`, controllers), not your memory.
4. **History**: `git log --oneline -15 -- <files you will touch>`. List prior
   fixes in the same area.
5. **Plan**: The change, and why it fits the intent and constraints.
6. **What would prove this wrong**: The observation that would mean the
   plan is treating a symptom.

If the brief reveals a conflict, stop and present it to the user as a
decision. Do not quietly pick the workaround.

## 3. Loop detector (stop signals)

Stop and go back to step 2 (conformance) when any of these is true:

- This is the **second or later fix** for the same symptom, route, or file
  within recent history.
- The fix **disables** a framework feature (`vaadin.copilot.enable=false`,
  eager load toggles, CSRF off), **adds an exemption/ignore rule**, or
  **forwards/redirects around** the framework.
- The fix **re-implements** something the framework ships (Vaadin
  `RequestUtil`, `VaadinSecurityConfigurer`, Spring Security defaults such as
  POST logout with CSRF). Look for the native mechanism first; use Vaadin MCP
  or Vaadin/Spring docs for the installed versions (see `pom.xml`).
- The explanation of *why* the fix works needs "because X collides with Y".
  A collision is usually a boundary drawn in the wrong place.
- A security scanner (Aikido) flags the previous fix.

When a stop signal fires, the question is not "how do I make this work?" but
"which decision made this necessary, and does it match the ADRs?"

## 4. Architecture invariants to verify

These come from the ADRs. The ADRs are authoritative; re-check them if this
list and the ADRs ever disagree.

- **Public SEO surface** (`/`, `/utstyr`, `/utstyr/{slug}`, `/selg`,
  sitemap, robots) is server-rendered Spring MVC + Thymeleaf and must work
  without JavaScript. (0002, 0003)
- **Vaadin Flow is the logged-in app surface under `/app`**, including the
  seller workspace and marketplace administration (`/app/admin`). Userflow 4:
  "Vaadin Flow: `/app` og underliggende selger-/adminflyter." Vaadin must not
  own the public root, and admin is not part of the SEO surface.
  (0002, 0003, userflow 4)
- **Domain objects have no JPA annotations.** JPA entities and Spring Data
  repositories stay inside the persistence adapter behind a Data Mapper.
  Flyway is the only schema owner. (0004)
- **Open Vaadin Core components only.** Commercial components require a new
  decision note. (0001)
- **Roles and approval.** `ADMIN` is a marketplace role, not a Keycloak admin.
  Self-registration never grants `SELLER`. Approval grants `SELLER` via a
  least-privilege Keycloak service account and writes an audit entry. (0006)
- **Security and privacy by design and by default** (0009, GDPR art. 25):
  collect and store only what a function needs, give every kind of personal
  data a retention period, keep new functionality closed until explicitly
  opened, prefer the framework's own security mechanisms, choose the simplest
  solution that meets the requirements, and require a decision note for
  every external service (which personal data, where it is processed,
  licence).
- **Secrets never in Git.** Local secrets live in the git-ignored `.env`.
- **CSRF protection stays on** for every state-changing request outside
  genuine Vaadin-internal requests.

## 5. After the change

- Tests first, per `AGENTS.md` and the `tdd` skill. Regression tests should
  pin the *intent* (for example "a forged request is rejected"), not the
  workaround.
- If the change alters an architectural decision, update or add a decision
  note in `docs/decisions/`. If it fixes drift from an ADR, say so in the
  commit message.
- Update the userflow status table and `docs/progress.md` when a flow step
  changes status.

## Related skills

- `domain-modeling`: when a term or ADR itself needs to change.
- `diagnosing-bugs`: once grounding shows it really is a local bug.
- `codebase-design`: when the fix is about where a seam or boundary goes.
