# {{ title }}

## Overview

- Consolidate database documentation into one global page.
- Explain the implemented database model that exists across the repository today.

## Implemented Service Schemas

- Group database information by service.
- For each service summarize the implemented tables or entities and what part of the domain they support.

## Relationships

- Describe the key relations and ownership structure visible in code.
- Include Mermaid ER diagrams for implemented services.

## Constraints and Persistence Notes

- Mention schema-management tooling, key flags, uniqueness, revocation markers, audit timestamps, and foreign keys when visible.
- Avoid generic database advice that is not backed by repository files.

## Source of Truth

- State that entities and changelogs win over conceptual docs when they diverge.
