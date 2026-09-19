# Security Architecture Model: SIS-IE

## 1. Overview
The Smart Inventory System (SIS-IE) implements a modern, stateless security architecture based on **JWT (JSON Web Tokens)** and **RBAC (Role-Based Access Control)**. This ensures that the system is scalable, secure, and meets industrial standards for enterprise software.

## 2. Authentication Flow (Stateless)
Instead of traditional server-side sessions, SIS-IE uses a token-based approach:

1. **Request**: The client sends credentials (`username`, `password`) via a `LoginRequest` DTO to the `/api/auth/login` endpoint.
2. **Verification**: The `AuthService` verifies the user in the database and checks the password using **BCrypt** (one-way hashing).
3. **Token Generation**: Upon successful login, the `JwtTokenProvider` generates a signed JWT containing:
    - **Subject**: The username.
    - **Claims**: User roles (e.g., `ROLE_MANAGER`).
    - **Expiration**: Token validity period (24 hours).
4. **Response**: The server returns an `AuthResponse` DTO containing the token.
5. **Access**: For every subsequent request, the client includes the token in the HTTP Header: `Authorization: Bearer <token>`.

## 3. Authorization Model (RBAC)
The system uses Role-Based Access Control to restrict access to sensitive operations.

| Role | Permissions | Example Endpoints |
| :--- | :--- | :--- |
| **WAREHOUSE_WORKER** | Read stock, perform goods receipt/issue. | `GET /stock`, `POST /receive` |
| **MANAGER** | Full access: Manage products, users, and system settings. | `DELETE /products`, `POST /users` |

### Implementation Detail:
- **Global Security**: The `SecurityConfig` defines that `/api/auth/**` is public, while all other requests require authentication.
- **Method Level Security**: Using `@PreAuthorize("hasRole('...')")`, specific business logic is protected at the controller level.

## 4. Security Components
- **BCryptPasswordEncoder**: Ensures passwords are never stored in plain text.
- **JwtAuthenticationFilter**: A custom filter that intercepts every request, validates the JWT, and sets the `SecurityContext` for Spring Security.
- **Stateless Policy**: `SessionCreationPolicy.STATELESS` ensures the server does not store any user state, making it ready for cloud deployment (Horizontal Scaling).

## 5. Data Protection (GDPR/DSGVO)
To comply with data protection standards:
- Only necessary user data is stored.
- Passwords are salted and hashed.
- Access is strictly limited via RBAC to prevent unauthorized data exposure.
