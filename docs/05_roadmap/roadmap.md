# Strategic Roadmap - SIS-IE (Enterprise Path)
Last Updated: September 20, 2026

## 1. Vision
Transform the current secure backend into a full-stack enterprise application that meets the requirements of large German industrial corporations (e.g., SAP, Siemens). The focus is on **scalability**, **modernity**, and **maintainability**.

## 2. Phase 4: Frontend Development (The Angular Shift)
Following the latest market analysis for 2016/2017, the project will migrate to a modern Angular architecture.

### 2.1 Technical Stack
- **Framework**: Angular 22+
- **State Management**: Angular Signals (Modern approach replacing complex RxJS streams).
- **Data Fetching**: Resource API for optimized backend communication.
- **UI Library**: Angular Material (Enterprise Design System).
- **Testing**: Vitest (Replacing Karma/Jasmine for faster execution).

### 2.2 Implementation Goals
- [ ] Initialize Angular project with strict TypeScript settings.
- [ ] Implement a secure Login Page integrated with the JWT Backend.
- [ ] Create a Dashboard for real-time stock overview using Signals.
- [ ] Build "Goods Receipt" and "Issue" forms with Signal-based validation.

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
