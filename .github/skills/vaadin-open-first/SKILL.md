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
