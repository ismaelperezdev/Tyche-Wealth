# {{ title }}

## Overview

- Consolidate what used to be split across authentication, API, flow, and sequence pages.
- Explain the real API surface implemented by the service.
- State clearly if the service exposes only a limited subset of its domain through HTTP today.

## Base Paths and API Surface

- Document the main base path or base paths visible in code.
- Explain which API contracts or controllers define the exposed surface.
- Explicitly mark missing or planned endpoints as not implemented.

## Implemented Endpoints

- For each implemented endpoint include:
  method
  path
  purpose
  request DTO or payload type
  response DTO or payload type
- Prefer concrete, source-backed descriptions over route inventories with no explanation.

## Validation, Errors, and Constraints

- Summarize request validation, business validation, and important constraints visible in code.
- Mention auth-related restrictions, rate limiting, or token rules when they are implemented.
- Include important error-handling notes only when backed by code.

## Flows and Sequence Diagrams

- Include compact descriptions of the main implemented flows.
- Embed Mermaid sequence or flow diagrams for the important operations.
- Prefer login, refresh, register, logout, or equivalent flows only when visible in code.

## Notes

- Keep this page aligned with implemented controllers and contracts.
- Do not infer CRUD or domain APIs that are not visible in code.
