# Requirements Specification: Smart Inventory System for Industrial Electronics (SIS-IE)

## 1. Project Overview
The SIS-IE is a specialized Warehouse Management System designed for medium-sized companies dealing with industrial electronic components. The primary goal is to eliminate search times, prevent stockouts, and manage product lifecycles (FIFO).

## 2. Functional Requirements (FR)
| ID | Requirement | Description | Priority |
|:---|:------------|:------------|:---------|
| FR1 | Product Management | CRUD operations for products (SKU, Name, Category, Description). | High |
| FR2 | Location Mapping | Assigning products to specific warehouse locations (e.g., Shelf A, Level 2). | High |
| FR3 | Stock Tracking | Real-time tracking of quantities per product and location. | High |
| FR4 | Inventory Movements | Support for Goods Receipt, Goods Issue, and Internal Transfers. | High |
| FR5 | Low-Stock Alerts | Automatic notification when stock falls below a defined threshold. | Medium |
| FR6 | FIFO Tracking | Recording delivery dates to ensure First-In-First-Out processing. | Medium |
| FR7 | Defect Management | Ability to mark items as 'Defective' to prevent accidental shipping. | Medium |
| FR8 | Supplier Linking | Association of products with specific suppliers for easy reordering. | Low |

## 3. Non-Functional Requirements (NFR)
| ID | Requirement | Description | Priority |
|:---|:------------|:------------|:---------|
| NFR1 | Security & RBAC | Role-Based Access Control (e.g., Warehouse Worker vs. Manager). | High |
| NFR2 | Data Integrity | Use of database transactions for all stock movements to prevent data loss. | High |
| NFR3 | Audit Logging | Every change in inventory must be logged (Who, When, What). | Medium |
| NFR4 | Performance | Product search by SKU must respond in < 200ms. | Medium |
| NFR5 | Maintainability | Code must follow SOLID principles and Clean Architecture. | High |

## 4. Target Users
- **Warehouse Worker:** Manages movements, marks defects, checks locations.
- **Inventory Manager:** Manages products, suppliers, and monitors stock levels/alerts.

## 5. Assumptions & Scope Decisions (v1)
These decisions are intentional and documented so the scope of v1 is unambiguous:

| # | Decision | Rationale |
|:--|:---------|:----------|
| A1 | **Single warehouse.** The system manages ONE physical warehouse. There is no `Warehouse` entity in v1; a `Location` (e.g. `A-1-01`) is an address *inside* that single warehouse. | Matches the project goal (one WMS for one medium-sized company) and keeps the data model minimal. |
| A2 | **`locationCode` is globally unique** (`@Column(unique = true)`). Since there is only one warehouse, a code like `A-1-01` can exist at most once in the whole system. | Direct consequence of A1; enforced by the database and validated in the service layer (409/400 on duplicate). |
| A3 | **Location = master data.** Locations are physical addresses managed via full CRUD (`GET /api/locations`, `POST .../create`, `PUT /{id}`, `DELETE /{id}`). Deletion is guarded: a location that still holds stock batches cannot be deleted (referential integrity of the domain, not just the DB). | Managers need to create shelves on the fly during goods receipt and correct mistakes; the guard prevents orphaned stock. |
| A4 | **StockBatch = "what lies where".** A `StockBatch` links a product to a location with quantity + received date (FIFO basis). It is NOT a warehouse or a location — it is the intersection of *product × location*. | Keeps FIFO logic clean and makes per-location stock queries trivial. |

## 6. Future Development Proposals (Roadmap)
Candidates for v2+, in rough order of value:

1. **Multi-warehouse support.** Introduce a `Warehouse` entity (e.g. codes `WAW`, `BIA`) with `Location @ManyToOne Warehouse`. Consequences to plan for:
   - Uniqueness changes from global `locationCode` to the pair `(warehouse, locationCode)` — two warehouses may both have an `A-1-01`.
   - All stock queries and UI filters gain a warehouse dimension; existing data migrates into a default warehouse.
2. **Supplier Management** (FR8) — currently pending; needed for reordering workflows.
3. **Audit Logging** (NFR3) — who changed what, when (stock movements first).
4. **Low-Stock Alerts** (FR5) and **Defect Management** (FR7) — medium-priority FRs not yet implemented.
5. **API Documentation** — Swagger/OpenAPI for the REST contract.

