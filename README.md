# 🔐 Password Manager — Spring Boot Backend

Secure, production-ready password manager backend built with Spring Boot, JWT authentication, and AES encryption.

---

## 🏗 Tech Stack

| Layer | Technology |
|-------|-----------|
| Framework | Spring Boot 3.2 |
| Security | Spring Security + JWT (jjwt) |
| Encryption | AES-128 CBC |
| Password Hashing | BCrypt |
| ORM | Spring Data JPA (Hibernate) |
| DB (local) | H2 in-memory |
| DB (prod) | PostgreSQL |
| API Docs | SpringDoc OpenAPI (Swagger UI) |
| Validation | Jakarta Validation |

---

## 📦 Package Structure

```
com.company.passwordmanager
 ├── config          # SecurityConfig, SwaggerConfig, AdminInitializer
 ├── controller      # AuthController, VaultController, PasswordController, AuditController
 ├── dto             # Request/Response DTOs
 ├── entity          # User, VaultItem, AuditLog
 ├── exception       # GlobalExceptionHandler + custom exceptions
 ├── repository      # Spring Data JPA repositories
 ├── security        # JwtAuthenticationFilter, CustomUserDetailsService
 ├── service         # AuthService, VaultService, PasswordService, AuditService
 └── util            # JwtUtil, EncryptionUtil
```

---

## 🚀 Running Locally

```bash
# Clone and run with local profile (H2 in-memory DB)
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

App starts at: `http://localhost:8080`

---

## 🔑 Default Admin Account

On startup, a superadmin is automatically created:

| Field | Value |
|-------|-------|
| Login | `superadmin` |
| Password | `Admin@2026` |
| Email | `superadmin@passwordmanager.com` |
| Role | `ADMIN` |

---

## 📖 API Documentation

| URL | Description |
|-----|-------------|
| `http://localhost:8080/swagger-ui.html` | Swagger UI |
| `http://localhost:8080/api-docs` | OpenAPI JSON |
| `http://localhost:8080/h2-console` | H2 DB Console (local only) |

---

## 🌐 API Endpoints

### Auth
| Method | Path | Description | Auth |
|--------|------|-------------|------|
| POST | `/auth/register` | Register new user | Public |
| POST | `/auth/login` | Login (email or login) | Public |

### Vault
| Method | Path | Description | Auth |
|--------|------|-------------|------|
| GET | `/vault` | List all items (no passwords) | JWT |
| POST | `/vault` | Create vault item | JWT |
| GET | `/vault/{id}` | Get item with decrypted password | JWT |
| PUT | `/vault/{id}` | Update vault item | JWT |
| DELETE | `/vault/{id}` | Delete vault item | JWT |
| POST | `/vault/{id}/copy` | Log copy event | JWT |

### Password Tools
| Method | Path | Description | Auth |
|--------|------|-------------|------|
| POST | `/password/generate` | Generate secure password | JWT |
| POST | `/password/strength` | Check strength + reuse detection | JWT |

### Audit
| Method | Path | Description | Auth |
|--------|------|-------------|------|
| GET | `/audit/my` | My audit logs | JWT |
| GET | `/audit/all` | All users' logs | ADMIN |
| GET | `/audit/vault/{id}` | Logs for vault item | JWT |

### Health
| Method | Path | Description |
|--------|------|-------------|
| GET | `/health` | Service health check |

---

## 🔐 Security Design

- **Master password** → BCrypt hashed, never stored plain
- **Vault passwords** → AES-128 CBC encrypted before DB storage
- **JWT tokens** → HS256, expire in 1 hour
- **Audit log** → Every VIEW, COPY, CREATE, UPDATE, DELETE is logged
- **Role-based access** → `ADMIN` can view all audit logs

---

## 🛠 Production Deployment

Set environment variables:

```bash
DB_URL=jdbc:postgresql://your-host:5432/passwordmanagerdb
DB_USERNAME=postgres
DB_PASSWORD=your_secure_password
```

Run with prod profile:

```bash
java -jar password-manager.jar --spring.profiles.active=prod
```

---

## 🧪 Tests

```bash
./mvnw test
```

Tests cover: context load, health check, admin login, user registration, and vault auth guard.
