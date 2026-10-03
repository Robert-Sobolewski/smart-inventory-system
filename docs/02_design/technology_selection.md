# Technology Selection & Rationale - SIS-IE
Last Updated: October 2, 2026

This document records **why** each technology in the stack was chosen. In an enterprise context a technology decision is not "what I know" — it is a trade-off against explicit criteria (industry fit, ecosystem maturity, requirements match, team skill). Each entry below states the criterion that drove the choice and the main alternative that was considered and rejected.

## 1. Selection Criteria
Every decision in this project is evaluated against:
1. **Industry standard** — Is it what German industrial / enterprise teams actually run in production? (IHK relevance)
2. **Requirements fit** — Does it directly satisfy a functional or non-functional requirement from `01_analysis/requirements.md`?
3. **Ecosystem & longevity** — Active maintenance, large community, long-term support (LTS)?
4. **Maintainability** — Readable, testable, easy to hand over to another engineer.

## 2. Backend

### 2.1 Java 21 (LTS)
- **Why**: Long-Term Support release with modern language features (records for DTOs, pattern matching, virtual threads). LTS = a supported baseline an enterprise can commit to for years.
- **Alternative considered**: Kotlin — excellent and increasingly common, but the team's existing Java base and the larger pool of Java-experienced industrial engineers made Java 21 the lower-risk choice.

### 2.2 Spring Boot (4.x)
- **Why**: The de-facto standard for enterprise Java backends in Germany. It provides convention-over-configuration wiring, a mature security module (`spring-boot-starter-security`), and first-class JPA integration — directly satisfying the "secure, stateless REST API" requirement with minimal boilerplate.
- **Alternative considered**: Quarkus / Micronaut (faster startup, more cloud-native). Rejected for this scope: Spring's ecosystem, documentation depth, and hiring pool outweigh the cold-start advantage for a moderate-size internal service.

### 2.3 JWT (jjwt) — stateless authentication
- **Why**: The requirement is a scalable, stateless API. A signed token carries identity + roles in the `Authorization` header, so no server-side session store is needed → horizontal scaling without sticky sessions. This maps directly to NFR (scalability / cloud readiness).
- **Alternative considered**: Server-side sessions — simpler but requires shared session storage and limits scaling; OAuth2/OIDC provider — more appropriate when multiple services or external clients are involved, which is not the case for this single internal tool yet.

### 2.4 Spring Security + RBAC
- **Why**: Method-level `@PreAuthorize` gives fine-grained role enforcement (WAREHOUSE_WORKER vs MANAGER) at the business boundary, and BCrypt hashing satisfies the "no plain-text passwords" data-protection requirement out of the box.
- **Alternative considered**: Custom hand-rolled auth — rejected; re-implementing token validation / password hashing is a classic source of security bugs that a battle-tested library already solves.

### 2.5 SQLite (dev) → PostgreSQL (target)
- **Why**: SQLite gives zero-config, file-based persistence for local development and testing (fast feedback loop). The JPA/`spring.datasource` abstraction means the production target is PostgreSQL — the relational workhorse in enterprise environments — by changing configuration only, not code.
- **Alternative considered**: MongoDB / NoSQL — rejected; inventory data is strongly relational (batches, stock levels, users), so a relational DB with transactions fits the domain better than document storage.

## 3. Frontend

### 3.1 Angular 22+ (standalone components + signals)
- **Why**: A current stable major release with strict TypeScript by default and a component model that scales to large teams — matching the enterprise/IHK emphasis on structure and type safety. Signals provide reactive state without the boilerplate of legacy RxJS streams, keeping feature code readable.
- **Alternative considered**: React / Vue — both valid; Angular was chosen for its opinionated, batteries-included architecture (routing, forms, DI, HTTP client) that enforces consistency across a team and reduces "how do we structure this" decisions.

### 3.2 Feature-based architecture (`core` / `features` / `shared`)
- **Why**: Organizing by feature rather than file type keeps related code together and makes each module independently understandable — the same separation-of-concerns principle applied to the UI as to the backend layers (see `architecture.md`).
- **Alternative considered**: Flat folder-by-type (`components/`, `services/`) — simpler at first but degrades quickly as features grow; cross-cutting concerns get scattered.

### 3.3 Dev proxy (`proxy.conf.json`) instead of CORS / hard-coded host
- **Why**: Forwarding `/api/*` to the backend in development means frontend code uses relative URLs, so no host/port is baked into source and no CORS configuration is needed locally. The same contract carries into production behind a reverse proxy (Roadmap Phase 5).
- **Alternative considered**: Enabling CORS on the backend — works but couples the API to browser-specific headers and leaks environment details; hard-coding `http://localhost:8888` in components breaks portability.

## 4. Summary Table
| Layer | Choice | Primary criterion | Rejected alternative |
| :--- | :--- | :--- | :--- |
| Language (BE) | Java 21 LTS | Industry standard, team base | Kotlin |
| Framework (BE) | Spring Boot 4.x | Enterprise default, security + JPA built-in | Quarkus / Micronaut |
| Auth | JWT (jjwt), stateless | Scalability requirement | Server sessions / full OAuth2 |
| Security | Spring Security + RBAC | Fine-grained roles, BCrypt | Custom auth |
| Database | SQLite → PostgreSQL | Relational fit, zero-config dev | MongoDB |
| Framework (FE) | Angular 22 + signals | Type safety, team consistency | React / Vue |
| FE structure | Feature-based | Separation of concerns | Folder-by-type |
| Dev wiring | Proxy config | No hard-coded host, no CORS | Backend CORS |

> **Note on honesty**: "I know this technology" is a legitimate *secondary* factor (team skill reduces risk), but it is never the primary justification. The primary driver for every row above is an explicit criterion from Section 1 — that is what makes the decision defensible in review or interview.
