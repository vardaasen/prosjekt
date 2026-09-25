---
name: domain-model-diagram
description: Build Havbruksbrukt's visual domain model before coding - linguistic analysis of the domain texts, a Mermaid class diagram with relationships and multiplicity, state diagrams for life cycles, and a personal-data inventory per entity (decision note 0009). Use when designing or changing domain entities, relationships or life cycles, before a feature touches the domain, when a ticket is labelled `domain`, or when grilling reveals unclear relationships ("who owns X?", "can one X have several Y?").
---

# Domain model diagram

`CONTEXT.md` is a glossary and nothing else (see `domain-modeling`). This
skill produces the *model* that the glossary words live in: which concepts
exist, how they relate, how many of each, how they change state, and which
personal data they carry. The result is `docs/domain/domenemodell.md`,
written in Norwegian with Mermaid diagrams.

Method after Thoughtworks' "Domain modeling: what you need to know before
coding": linguistic analysis, entities, relationships, multiplicity,
attributes and operations, then diagrams.

## 1. Gather the domain texts

Read fully: `CONTEXT.md`, `docs/decisions/*.md`, `docs/userflows/*`, and the
design handbook in `DesignMarketPlace/` if it is present. Note each source
so every modelling choice can be traced back to a sentence.

## 2. Linguistic analysis

Make two tables in the model file:

- **Nouns and noun phrases** → classify each as *entitet* (has identity and
  life of its own), *verdiobjekt* (defined only by its values, e.g. a price
  or an organisation number), *attributt*, *rolle* (a part something plays
  in a relationship, not a thing of its own), or *utenfor* (outside the
  domain, e.g. Keycloak, UI). Quote the source sentence.
- **Verbs and verb phrases** → candidate relationships or operations
  ("en person *handler for* en virksomhet", "administratoren *verifiserer*
  en tilknytning").

Check every noun against `CONTEXT.md`. A synonym listed under `_Avoid_` is
mapped to the canonical term, never modelled as its own concept. A noun that
is not in the glossary is either added through `domain-modeling` or marked
*utenfor*.

Roles are the most common modelling error in this project: *selger* and
*kjøper* are roles a Virksomhet plays in a listing or trade (decision note
0007), not entities or account types.

## 3. Class diagram

Write a Mermaid `classDiagram`:

- Classes are the entities and value objects, named with the glossary terms.
- Relationships are labelled with the verb from step 2.
- Every end has a **multiplicity** (`1`, `0..1`, `*`, `1..*`). Justify each
  non-obvious multiplicity with a concrete scenario in a note below the
  diagram ("én person kan ha tilknytning til to virksomheter når den er
  daglig leder i begge").
- Use composition (`*--`) only for true ownership where the part cannot
  exist without the whole.
- Attributes are domain-level (e.g. `organisasjonsnummer`, `status`), never
  database or framework types. Operations are the domain actions from the
  verbs (e.g. `verifiser()`, `trekkTilbake()`).
- External systems (Keycloak identity, e-posttjeneste) appear only as
  `<<ekstern>>` stereotypes where the domain refers to them.

## 4. State diagrams

For every entity with a status or life cycle (for example Tilknytning,
Annonse/Publiseringsstatus, Bud, Handel), write a Mermaid `stateDiagram-v2`
with the transitions, who triggers each one, and the rule that guards it.
Every status value must be a term from `CONTEXT.md`.

## 5. Personal-data inventory (decision note 0009)

One table row per entity: which personal data it holds, why it is needed
(the function), legal basis, retention period, and what triggers deletion.
Anything without a clear need is removed from the model (data
minimisation). Legal retention requirements (e.g. accounting rules for
trades) are recorded with their source or marked as open.

## 6. Open questions and hand-off

- List every uncertainty (multiplicity, ownership, state transitions) under
  "Åpne spørsmål" and resolve them with `grilling`, one at a time.
- New or changed terms go into `CONTEXT.md` through `domain-modeling`.
- Hard-to-reverse choices become decision notes.

## Checks before finishing

- Every class and status in the diagrams is a `CONTEXT.md` term, and every
  `CONTEXT.md` term is either in the model or explicitly *utenfor*.
- Every multiplicity has a source or a scenario.
- No implementation detail: no tables, JPA, URLs or UI.
- The model agrees with the decision notes; if not, the disagreement is a
  finding for the user (see `intent-grounding`).
