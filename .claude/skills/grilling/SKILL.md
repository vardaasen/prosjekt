---
name: grilling
description: Interview the user about a plan or design, one question at a time, until every branch of the decision tree is resolved. Use when the user says "grill me", when a plan has open decisions, or when another skill (grill-with-docs, triage) asks for a grilling session.
---

# Grilling

Goal: reach a shared, precise understanding of a plan before any code is
written. Unresolved decisions found now are cheap; the same decisions found
during implementation cause fix-loops.

## How to run it

1. **Ground first.** Read what the repo already decided before asking
   anything: `CONTEXT.md`, `docs/decisions/`, `docs/userflows/`, and the code
   in the area. Never ask a question the repo can answer; look it up and
   state what you found instead.
2. **Map the decision tree.** List the open decisions and which ones depend
   on others. Resolve roots before leaves.
3. **One question at a time.** Ask a single, concrete question. Give your
   recommended answer and the reason, so the user can simply agree or
   correct. Offer 2–4 real options when the choice is genuinely open.
4. **Push on vagueness.** Replace fuzzy words with the project's domain terms
   (`CONTEXT.md`). Test answers with concrete scenarios and edge cases
   ("what happens if an approved seller's application is later rejected?").
5. **Check against constraints.** If an answer contradicts a decision note
   or a userflow, say so and ask which one should change.
6. **Record as you go.** Keep a running "established so far" list. When used
   through `grill-with-docs`, update `CONTEXT.md` and decision notes inline
   (see `domain-modeling`).
7. **Stop when done.** Finish when every branch is resolved or explicitly
   deferred. Summarize the decisions, the deferred items, and the next step.
