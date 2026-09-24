---
name: vaadin-open-first
description: Vaadin UI decisions for Havbruksbrukt. Use when adding Vaadin views, routes or dependencies: open Vaadin Core components first, Vaadin lives under /app, commercial components need a decision note.
---

# Vaadin open-first

Use this skill for Vaadin UI decisions.

- Use Vaadin Flow as the default app UI model.
- Use Vaadin Core/open components first.
- Do not add `com.vaadin:vaadin` or commercial add-ons by default.
- If an Enterprise component is considered, create a decision note with:
  - the user need;
  - open alternatives considered;
  - license/runtime consequence;
  - final decision.
- Keep public SEO pages outside pure client-side Vaadin routes unless crawlability is explicitly proven.
- Vaadin is the logged-in app surface and lives under `/app` (seller workspace and marketplace administration alike, e.g. `/app/admin`). It must not own the public root; see decision notes 0002/0003 and userflow 4.
- Prefer Vaadin's own Spring Security integration (for example `RequestUtil`, `VaadinSecurityConfigurer`) over hand-rolled exemptions or forwarding.
