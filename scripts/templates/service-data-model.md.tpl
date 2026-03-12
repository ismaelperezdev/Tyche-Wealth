# {{ title }}

## Overview

- Consolidate what used to be split across entity pages, table pages, and schema pages.
- Explain the persistence model implemented for the service.

## Implemented Entities

- List real entities visible in code.
- For each entity summarize:
  table name
  purpose in the domain
  important fields
  lifecycle or audit fields when present

## Relationships

- Describe actual relationships between entities.
- Explain the role of parent-child, ownership, or token-link relationships when visible.
- Include a Mermaid ER diagram.

## Persistence and Schema Notes

- Mention Liquibase or schema-management tooling when present.
- Explain important constraints such as uniqueness, nullability, token revocation flags, or foreign keys when visible.
- Prefer code and changelogs over older Markdown.

## Related Documentation

- Point to service overview and runtime docs when persistence behavior depends on them.
