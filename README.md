# API Attendance System

## Overview
A RESTful API for managing courses, schedules, attendance, and user profiles with role-based access control (Admin, Teacher, Student).

## Authentication
- JWT-based authentication
- Login endpoint returns token
- All endpoints (except login) require `Authorization: Bearer <token>` header

## Endpoints

### Auth
- `POST /api/auth/login` — Login, returns JWT
- `POST /api/auth/logout` — Logout, revokes token

### User Profile
- `GET /api/user/profile` — Get current user profile
- `PUT /api/user/profile` — Update profile fields

### Course
- `GET /api/courses` — List courses (admin/teacher)
- `POST /api/courses` — Create course (admin only)
- `GET /api/courses/{id}` — Get course details
- `PUT /api/courses/{id}` — Update course (admin only)
- `DELETE /api/courses/{id}` — Delete course (admin only)

### Schedule
- `GET /api/schedules?courseId=...` — List schedules for course
- `POST /api/schedules` — Create schedule
- `PUT /api/schedules/{id}` — Update schedule
- `DELETE /api/schedules/{id}` — Delete schedule
- `POST /api/schedules/{id}/cancel` — Cancel schedule
- `POST /api/schedules/{id}/complete` — Mark schedule as completed

### Attendance
- `POST /api/attendance/checkin` — Student check-in
- `POST /api/attendance/checkout` — Student check-out
- `POST /api/attendance/verify` — Teacher/admin verify attendance
- `GET /api/attendance?scheduleId=...` — List attendance for schedule
- `GET /api/attendance?studentId=...&date=...` — List attendance for student/date
- `GET /api/attendance?courseId=...&date=...` — List attendance for course/date

## Example Request: Login
```http
POST /api/auth/login
Content-Type: application/json
{
  "username": "admin",
  "password": "password"
}
```
Response:
```json
{
  "token": "<jwt-token>"
}
```

## Example Request: Get Profile
```http
GET /api/user/profile
Authorization: Bearer <jwt-token>
```
Response:
```json
{
  "id": "user-1",
  "username": "admin",
  "role": "ADMIN",
  "email": "admin@example.com"
}
```

## Error Handling
- All errors return JSON with `error` field and appropriate HTTP status code.

## Postman Collection
See `postman_collection.json` for ready-to-import API requests and examples.
