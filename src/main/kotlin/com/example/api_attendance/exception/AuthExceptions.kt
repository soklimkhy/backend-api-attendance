package com.example.api_attendance.exception

class UsernameExistsException(username: String) : 
    IllegalArgumentException("Username already exists: $username")

class InvalidCredentialsException : 
    IllegalArgumentException("Invalid username or password")

class UserNotFoundException(username: String) : 
    IllegalArgumentException("User not found: $username")