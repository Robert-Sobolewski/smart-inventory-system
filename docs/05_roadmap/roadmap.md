# Strategic Roadmap - SIS-IE (Enterprise Path)
Last Updated: October 2, 2026

## 1. Vision
Transform the current secure backend into a full-stack enterprise application that meets the requirements of large German industrial corporations (e.g., SAP, Siemens). The focus is on **scalability**, **modernity**, and **maintainability**.

## 2. Phase 4: Frontend Development (The Angular Shift)
Following the latest market analysis for 2026, the project has migrated to a modern Angular architecture.

### 2.1 Technical Stack
- **Framework**: Angular 22+
- **State Management**: Angular Signals (Modern approach replacing complex RxJS streams).
- **Data Fetching**: Resource API / HttpClient with interceptors for optimized backend communication.
- **UI Library**: Plain semantic HTML5 + SCSS so far; Angular Material to be evaluated when the UI grows.
- **Testing**: Jasmine/Karma out of the box (Vitest migration still open — see 2.3).

### 2.2 Implementation Goals
- [x] Initialize Angular project with strict TypeScript settings.
- [x] Implement a secure Login Page integrated with the JWT Backend (JWT interceptor + route guard on `/products`).
- [x] Product list with signal-based "add product" form (`features/inventory`), served via dev proxy (`proxy.conf.json`).
- [ ] Create a Dashboard for real-time stock overview using Signals.
- [ ] Build "Goods Receipt" and "Issue" forms with Signal-based validation (backend endpoints `/api/logistics/receive` / `/issue` already exist).

### 2.3 Open Decisions
- [ ] Frontend test strategy: component tests for login / product-list / status are missing; decide between Karma/Jasmine (default) and Vitest.

## 3. Phase 5: Infrastructure & DevOps (The Cloud Path)
To ensure the application is deployable in corporate environments, we will implement modern infrastructure tools.

### 3.1 Containerization
- **Docker**: Create a multi-stage Dockerfile for the Spring Boot backend and an Nginx-based image for the Angular frontend.
- **Docker Compose**: Orchestrate the Backend, Frontend, and PostgreSQL database into a single command (`docker-compose up`).

### 3.2 CI/CD Pipeline
- **GitHub Actions / GitLab CI**: Automate the build process.
- **Pipeline Stages**: `Build` $\rightarrow$ `Test (JUnit/JaCoCo)` $\rightarrow$ `Security Scan` $\rightarrow$ `Docker Build` $\rightarrow$ `Deploy`.

## 4. Phase 6: Advanced Features & Final Polish
- [ ] **Audit Logging**: Implement a system to track every change in the inventory for compliance (GDPR/ISO).
- [ ] **Low-Stock Alerts**: Automated notifications when stock falls below a threshold.
- [ ] **API Documentation**: Integration of Swagger/OpenAPI 3.0 for professional API specs.
