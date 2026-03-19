# 📚 BookstoreCMS

A full-featured **Bookstore Content Management System** built with Spring Boot 3. Designed for admin users to manage books, orders, members, loyalty points, and discount coupons through a clean, role-secured web interface.

---

## 🖼️ Preview

### Login Page
<img width="1917" height="1047" alt="Screenshot 2026-03-19 at 14 23 33" src="https://github.com/user-attachments/assets/fbc35b00-7b72-4a58-9987-c0d1031d183f" />

### Dashboard
<img width="1915" height="1049" alt="Screenshot 2026-03-19 at 14 47 08" src="https://github.com/user-attachments/assets/0ad3822d-4010-4e27-9917-51b93a7992b4" />

### User Management
<img width="1920" height="1045" alt="Screenshot 2026-03-19 at 14 50 11" src="https://github.com/user-attachments/assets/d125f691-efc6-404d-843a-ade8091657e3" />

### Book Management
<img width="1920" height="1048" alt="Screenshot 2026-03-19 at 14 47 54" src="https://github.com/user-attachments/assets/e4b435ea-b795-4e88-9de4-9138d81b8e65" />

### Order Management
<img width="1920" height="1046" alt="Screenshot 2026-03-19 at 14 48 12" src="https://github.com/user-attachments/assets/b40300c7-d805-46c8-a6d0-6f96006369fa" />

### Points Management
<img width="1918" height="1046" alt="Screenshot 2026-03-19 at 14 48 49" src="https://github.com/user-attachments/assets/0b0886e4-0aa1-4a50-b11d-ffbb89adec46" />

### Coupon Management
<img width="1920" height="1053" alt="Screenshot 2026-03-19 at 14 49 10" src="https://github.com/user-attachments/assets/4d4e8684-625c-4eb7-8163-f3c6b57c19ec" />

---

## 🚀 Features

### 📖 Book Management
- Add, edit, and delete book listings
- Upload and manage book cover images (up to 10 MB)
- Track book inventory and metadata

### 🛒 Order Management
- View and manage customer orders
- Update order statuses
- Paginated order listing with filtering

### 👥 User Management
- Admin dashboard for managing registered members
- Role-based access control (Admin / User)
- Secure password hashing via BCrypt

### 🎟️ Coupon System
- Create percentage-based or fixed-amount discount coupons
- Set coupon validity periods and usage limits
- Apply coupons during checkout with reservation locking

### 🏆 Loyalty Points System
- Configurable point earn rules (basis-point reward rate, cap limits)
- Point redemption against order totals
- Points expiry via batch-lot tracking
- Automated daily expiry job (runs at 02:30 AM)
- Full ledger history per user

### 📊 Admin Dashboard
- Monthly sales statistics
- Daily sales trend chart
- Order status distribution chart
- User and book count summaries

### 🔐 Security
- Spring Security with form login and HTTP Basic (for API/Postman access)
- All `/admin/**` routes require `ROLE_ADMIN`
- CSRF protection with selective exemptions for REST endpoints
- Audit log tracking for admin actions

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.5.4 |
| Security | Spring Security 6 |
| ORM | Spring Data JPA / Hibernate |
| Database | MySQL |
| Templating | Thymeleaf + Layout Dialect |
| Build Tool | Maven (with Maven Wrapper) |
| Utilities | Lombok, Jackson |

---

## 📁 Project Structure

```
src/main/java/com/example/bookstore/
├── config/           # Security, scheduling, static resources, bootstrap
├── controller/       # Web controllers (Admin, Order, Coupon, Points, User)
├── entity/           # JPA entities (Book, Order, User, Coupon, Points, etc.)
├── repository/       # Spring Data JPA repositories
├── service/          # Business logic services
└── BookstoreApplication.java

src/main/resources/
├── templates/
│   ├── admin/        # Thymeleaf admin panel templates
│   └── login.html
└── application.properties
```

---

## ⚙️ Getting Started

### Prerequisites

- Java 21+
- MySQL 8+
- Maven (or use the included `./mvnw` wrapper)

### 1. Clone the Repository

```bash
git clone https://github.com/your-username/BookstoreCMS.git
cd BookstoreCMS
```

### 2. Create the Database

```sql
CREATE DATABASE Bookstore CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 3. Configure `application.properties`

Edit `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/Bookstore?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Taipei&allowPublicKeyRetrieval=true&zeroDateTimeBehavior=CONVERT_TO_NULL
spring.datasource.username=your_db_username
spring.datasource.password=your_db_password
```

> **Note:** The default config points to port `8889` (MAMP default). Change to `3306` for a standard MySQL installation.

### 4. Run the Application

```bash
./mvnw spring-boot:run
```

The app will start at `http://localhost:8080`.

### 5. Login

On first startup, `AdminBootstrap` automatically seeds default credentials:

| Role | Email | Password |
|---|---|---|
| Admin | `admin@bookstore.com` | `admin123` |
| User | `user@bookstore.com` | `user123` |

> **Important:** Change these credentials before deploying to production.

---

## 🔌 API Endpoints (REST)

Selected endpoints support REST access (e.g., via Postman with HTTP Basic Auth):

| Method | Path | Description |
|---|---|---|
| GET | `/admin/dashboard` | Admin dashboard |
| GET/POST | `/admin/books` | Book listing and creation |
| GET/POST | `/admin/orders/**` | Order management |
| GET/POST | `/admin/points/**` | Points management |
| GET/POST | `/admin/coupons` | Coupon management |
| GET | `/admin/users` | User management |
| GET | `/admin/logs` | Admin audit logs |

> CSRF is disabled for `/admin/orders/**`, `/admin/points/**`, `/api/orders/**`, and `/api/points/**` to allow Postman access.

---

## 🕒 Scheduled Jobs

| Job | Schedule | Description |
|---|---|---|
| Points Expiry | Daily at 02:30 AM | Expires overdue point lots and updates user balances |

---

## 📦 File Uploads

Book cover images are stored under the `uploads/covers/` directory. File size limits:

```properties
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB
```

---

## 🧪 Running Tests

```bash
./mvnw test
```

---

## 📄 License

This project is for educational and demonstration purposes.
