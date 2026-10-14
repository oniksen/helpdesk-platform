---
name: memory
description: Maintain a persistent knowledge graph for this Kotlin Multiplatform project (max-helpdesk). Use when you need to recall or record project structure, modules, targets (jvm/js), tech stack (Compose Multiplatform, Koin, kotlinx.serialization, expect/actual), architecture decisions, or cross-module dependencies. Trigger proactively at the start of a session and whenever a structural fact changes.
---

# Memory (Knowledge Graph)

Powered by the `memory` MCP server (`@modelcontextprotocol/server-memory`) and the
`memory://knowledge-graph` resource. It stores a persistent graph of entities
(nodes), their observations, and relations (edges).

## When to use
- At session start: read the graph to orient yourself on the project structure.
- When you learn a durable fact: a new module, target, dependency, pattern, or
  architectural decision.
- When answering questions about how pieces fit together.

## Rules
- **Entities** = distinct things: modules, targets, tech stack items, key
  components (e.g. `core:uiadaptive`, `jvm target`, `Koin DI`).
- **Observations** = concise factual notes attached to an entity.
- **Relations** = active-voice edges between entities: `depends on`,
  `uses`, `targets`, `part of`, `implements`.
- Keep names stable and reused. Prefer updating observations over creating
  near-duplicate entities.
- Do not store secrets, credentials, or transient/one-off details.

## Workflow
1. `memory_read_graph` to load the current graph.
2. Create or update entities (`memory_create_entities`,
   `memory_add_observations`, `memory_delete_observations`).
3. Link them (`memory_create_relations`, `memory_delete_relations`).
4. Remove obsolete nodes with `memory_delete_entities`.

## Project baseline (helpdesk-platform)
- Multi-module Kotlin Multiplatform + Compose Multiplatform app.
- Targets: `js { browser() }` (Web; entry points `webApp`, `webShell`) and
  `jvm()` (Desktop — previews and tests only).
- Sourcesets: `commonMain`, `jsMain`, `jvmMain` (+ `webMain` under `webApp`).
- Stack: Kotlin 2.4.20, Compose Multiplatform 1.12.0, Koin 4.0.2,
  kotlinx.serialization, Ktor 3.5.2, Navigation3, kotlin-wrappers, expect/actual
  (e.g. `QrCodeScannerFactory`, `CreateKtorClient`).
- Modules:
  - `webApp`, `webShell` — JS entry points (app / bootstrap shell).
  - `core`: `architecture`, `exception`, `di`, `di/infra`, `uiadaptive`,
    `authorization/{api,impl}`, `network/{api,impl}`, `navigation/{api,impl}`,
    `update/{api,impl}`.
  - `features`: `authorization`, `parking`, `tasks`, `settings`, `update`
    (each `api`/`impl`).
  - `maxminiappapi/{api,impl}` — bridge to the MAX platform.
