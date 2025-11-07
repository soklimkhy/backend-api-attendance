# API Attendance System — Example Requests

## Auth

### Login
**POST** `/api/auth/login`
```http
Content-Type: application/json

{
  "username": "admin",
  "password": "password"
}
```
**Response:**
```json
{
  "token": "<jwt-token>"
}
```

### Logout
**POST** `/api/auth/logout`
```http
Authorization: Bearer <jwt-token>
```

---

## User Profile

### Get Profile
**GET** `/api/user/profile`
```http
Authorization: Bearer <jwt-token>
```
**Response:**
```json
{
  "id": "user-1",
  "username": "admin",
  "role": "ADMIN",
  "email": "admin@example.com"
}
```

### Update Profile
**PUT** `/api/user/profile`
```http
Authorization: Bearer <jwt-token>
Content-Type: application/json

{
  "username": "newname",
  "email": "newemail@example.com"
}
```

---

## Course

### List Courses
**GET** `/api/courses`
```http
Authorization: Bearer <jwt-token>
```

### Create Course
**POST** `/api/courses`
```http
Authorization: Bearer <jwt-token>
Content-Type: application/json

{
  "code": "CS101",
  "name": "Intro to CS",
  "description": "Introduction to Computer Science",
  "instructorId": "teacher-1",
  "academicYear": "2025-2026",
  "semester": "Fall"
}
```

### Get Course Details
**GET** `/api/courses/{courseId}`
```http
Authorization: Bearer <jwt-token>
```

---

## Schedule

### Create Schedule
**POST** `/api/schedules`
```http
Authorization: Bearer <jwt-token>
Content-Type: application/json

{
  "courseId": "course-1",
  "dayOfWeek": 1,
  "startTime": "09:00",
  "endTime": "10:30",
  "room": "Room 101",
  "type": "REGULAR",
  "createdBy": "teacher-1"
}
```

---

## Attendance

### Check-in Attendance
**POST** `/api/attendance/checkin`
```http
Authorization: Bearer <jwt-token>
Content-Type: application/json

{
  "scheduleId": "schedule-1",
  "courseId": "course-1",
  "studentId": "student-1",
  "location": {
    "latitude": 1.0,
    "longitude": 1.0,
    "accuracy": 1.0
  }
}
```

### Verify Attendance
**POST** `/api/attendance/verify`
```http
Authorization: Bearer <jwt-token>
Content-Type: application/json

{
  "attendanceId": "attendance-1",
  "verifierId": "teacher-1"
}
```


{
    "message": "Login successful",
    "user": {
        "id": "waXclfkG4An4jvcC69JP",
        "fullName": "Teacher@123",
        "role": "STUDENT",
        "createdAt": 1761734499419
    },
    "accessToken": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ3YVhjbGZrRzRBbjRqdmNDNjlKUCIsImlhdCI6MTc2MTczNDcxOCwiZXhwIjoxNzYxNzM1NjE4fQ.LYXlY2pA6-S-n6QKTBM9h4nf2ziJv6FirBIU9LV31KQ",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ3YVhjbGZrRzRBbjRqdmNDNjlKUCIsImlhdCI6MTc2MTczNDcxOCwiZXhwIjoxNzYyMzM5NTE4fQ.E_x3NQHQFADjgOI1M0Zl4pW30WX61tINJTv9JByNv7E"
}