# Calendar Availability API

REST API quản lý lịch hẹn và kiểm tra khung giờ trống, xây dựng bằng **Spring Boot 4** + **PostgreSQL**.

## Tech Stack

| Layer       | Technology                                  |
|-------------|---------------------------------------------|
| Language    | Java 21                                     |
| Framework   | Spring Boot 4.0.3 (Web MVC, Data JPA, Validation) |
| Database    | PostgreSQL 16                               |
| API Docs    | SpringDoc OpenAPI 2.8.6 (Swagger UI)        |
| Testing     | JUnit 5, Mockito, MockMvc, AssertJ          |
| Build       | Maven                                       |
| Deploy      | Docker, Render                              |

## Project Structure

```
src/main/java/com/example/calendar_availability/
├── config/
│   ├── AppConfig.java            # Seed data khi khởi động
│   └── SwaggerConfig.java        # Cấu hình OpenAPI / Swagger
├── event/
│   ├── EventController.java      # REST Controller (3 endpoints)
│   ├── EventService.java         # Service interface
│   ├── EventServiceImpl.java     # Business logic + mapping
│   ├── EventRepository.java      # Spring Data JPA Repository
│   ├── Event.java                # JPA Entity
│   ├── EventType.java            # Enum: APPOINTMENT | BLOCK
│   ├── CreateEventRequest.java   # DTO – input tạo event
│   ├── EventResponse.java        # DTO – output trả về client
│   ├── AvailabilityRequest.java  # DTO – query khung giờ trống
│   └── TimeSlot.java             # DTO – 1 khung giờ trống
├── exception/
│   ├── CustomException.java      # Business exception + HttpStatus
│   ├── ErrorResponse.java        # Cấu trúc response lỗi
│   └── GlobalExceptionHandler.java  # Xử lý lỗi tập trung
└── utils/
    └── AppUtils.java             # Config properties binding
```

## API Endpoints

### 1. Tạo sự kiện — `POST /api/events`

Tạo event mới. Nếu event loại `APPOINTMENT` trùng giờ với appointment khác cùng owner → trả `409 CONFLICT`.

**Request body:**
```json
{
  "title": "Họp Daily Scrum",
  "startAt": "2026-03-12T09:00:00+07:00",
  "endAt": "2026-03-12T09:30:00+07:00",
  "timezone": "Asia/Ho_Chi_Minh",
  "type": "APPOINTMENT",
  "ownerId": 1,
  "notes": "Cập nhật tiến độ",
  "location": "Google Meet",
  "attendees": "team@company.com"
}
```

**Response:** `201 Created`
```json
{
  "id": 1,
  "title": "Họp Daily Scrum",
  "startAt": "2026-03-12T09:00:00+07:00",
  "endAt": "2026-03-12T09:30:00+07:00",
  "timezone": "Asia/Ho_Chi_Minh",
  "type": "APPOINTMENT",
  "ownerId": 1,
  "notes": "Cập nhật tiến độ",
  "location": "Google Meet",
  "attendees": "team@company.com"
}
```

### 2. Lấy sự kiện theo khoảng thời gian — `GET /api/events`

Truy vấn tất cả event của 1 owner trong khoảng `[from, to]`.

**Query params:**

| Param     | Bắt buộc | Mô tả                           |
|-----------|----------|----------------------------------|
| `ownerId` | ✅        | ID người sở hữu lịch            |
| `from`    | ✅        | Mốc thời gian bắt đầu (ISO 8601) |
| `to`      | ✅        | Mốc thời gian kết thúc (ISO 8601) |

**Ví dụ:**
```
GET /api/events?ownerId=1&from=2026-03-12T00:00:00+07:00&to=2026-03-12T23:59:59+07:00
```

**Response:** `200 OK` — mảng JSON các event overlap với khoảng truy vấn.

### 3. Kiểm tra khung giờ trống — `POST /api/availability/query`

Tính toán các khung giờ trống (không bị chiếm bởi bất kỳ event nào — cả `APPOINTMENT` lẫn `BLOCK`).

**Request body:**
```json
{
  "ownerId": 1,
  "from": "2026-03-12T09:00:00+07:00",
  "to": "2026-03-12T17:00:00+07:00"
}
```

**Response:** `200 OK`
```json
[
  { "startAt": "2026-03-12T09:30:00+07:00", "endAt": "2026-03-12T10:00:00+07:00" },
  { "startAt": "2026-03-12T11:30:00+07:00", "endAt": "2026-03-12T12:00:00+07:00" },
  { "startAt": "2026-03-12T15:00:00+07:00", "endAt": "2026-03-12T17:00:00+07:00" }
]
```

## Error Handling

Tất cả lỗi trả về cùng 1 cấu trúc:

```json
{
  "timestamp": "2026-03-12T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Start time must be strictly before end time",
  "path": "/api/events"
}
```

| HTTP Status | Khi nào                                        |
|-------------|------------------------------------------------|
| `400`       | Thiếu field bắt buộc, time range không hợp lệ |
| `404`       | Owner ID không tồn tại                         |
| `409`       | APPOINTMENT trùng giờ với appointment khác     |
| `500`       | Lỗi server không xác định                     |

## Business Rules

- **Time range**: `startAt` phải **nhỏ hơn nghiêm ngặt** `endAt` (bằng nhau cũng không được).
- **Overlap detection**: Chỉ áp dụng cho `APPOINTMENT`. Event loại `BLOCK` không trigger overlap check.
- **Overlap scope**: Chỉ check trong cùng 1 `ownerId`. Khác owner → cho phép trùng giờ.
- **Availability**: Cả `APPOINTMENT` lẫn `BLOCK` đều chiếm thời gian → giảm khung giờ trống.
- **Overlapping events**: Thuật toán xử lý đúng các trường hợp event chồng lấn, event lồng nhau, event nối liền (back-to-back).

## Getting Started

### Yêu cầu

- Java 21+
- Docker & Docker Compose (cho PostgreSQL)

### 1. Khởi động database

```bash
docker compose up -d
```

### 2. Chạy ứng dụng

```bash
./mvnw spring-boot:run
```

Ứng dụng chạy tại: **http://localhost:8080**

Khi khởi động lần đầu, hệ thống tự động seed **10 event demo** (3 ngày, 3 owner).

### 3. Swagger UI

Mở trình duyệt: **http://localhost:8080/swagger-ui.html**

### 4. Chạy tests

```bash
./mvnw test
```

## Tests

**43 test cases**, chia thành 4 nhóm logic:

| Nhóm | Số test | Mô tả |
|------|---------|--------|
| 1. Validation | 7 | Thiếu field, time range sai, owner không tồn tại |
| 2. Range Overlap Logic | 5 | Event nằm trong / ngoài / chồng lấn khoảng truy vấn |
| 3. Overlap Rejection | 5 | APPOINTMENT trùng giờ → 409, BLOCK bỏ qua, khác owner → cho phép |
| 4. Availability | 10 | Tính khung giờ trống: event ở giữa, fully booked, chồng lấn, lồng nhau, nối liền |
| Controller Tests | 16 | Validation 400, Overlap 409, GET/POST đúng response |

```
Tests run: 43, Failures: 0, Errors: 0, Skipped: 0 — BUILD SUCCESS
```

## Deploy

### Docker

```bash
docker build -t calendar-api .
docker run -p 8080:8080 \
  -e DATABASE_URL=jdbc:postgresql://host:5432/db \
  -e DATABASE_USERNAME=admin \
  -e DATABASE_PASSWORD=password \
  calendar-api
```

### Render

Project có sẵn `render.yaml` — Blueprint tự động tạo PostgreSQL + Web Service khi kết nối repository với Render.

## Environment Variables

| Variable            | Default                              | Mô tả                    |
|---------------------|--------------------------------------|--------------------------|
| `PORT`              | `8080`                               | Port ứng dụng           |
| `DATABASE_HOST`     | `localhost`                          | PostgreSQL host          |
| `DATABASE_PORT`     | `5432`                               | PostgreSQL port          |
| `DATABASE_NAME`     | `db`                                 | Tên database             |
| `DATABASE_USERNAME` | `admin`                              | DB username              |
| `DATABASE_PASSWORD` | `password`                           | DB password              |
| `JPA_DDL_AUTO`      | `create-drop`                        | Hibernate DDL strategy   |
| `SWAGGER_ENABLE`    | `true`                               | Bật/tắt Swagger UI       |

