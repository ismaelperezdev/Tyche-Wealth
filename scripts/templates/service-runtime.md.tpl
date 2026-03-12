# {{ title }}

## Overview

- Consolidate what used to be spread across local setup, security notes, and runtime guidance.
- Explain how the service is configured and operated in the current repository.

## Requirements

- Describe the real runtime prerequisites visible in the repository:
  Java version or build tooling when visible
  database dependency
  local configuration files
  environment or property-based configuration

## Run Locally

- Provide the normal local start path backed by the repository files.
- Mention wrapper scripts, build commands, or service-specific startup expectations only when they exist.
- Keep secret handling out of the page.

## Local Configuration

- Summarize the important groups of configuration keys:
  datasource
  JWT or token settings
  rate limiting
  spring application setup
  imports or overrides
- Prefer grouped explanation over dumping raw keys without context.

## Security, Rate Limiting, and Observability

- Explain the concrete security setup visible in code.
- Explain rate limiting or interceptors when implemented.
- Mention metrics, logging, or monitoring hooks only if present.

## Operational Notes

- Call out implementation-backed caveats, current limitations, and local-only assumptions.
