package com.example.api_attendance.security

import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority

class JwtAuthentication(private val userId: String?) : Authentication {
    override fun getAuthorities(): MutableCollection<out GrantedAuthority>? = null
    override fun getCredentials(): Any? = null
    override fun getDetails(): Any? = null
    override fun getPrincipal(): Any? = userId
    override fun isAuthenticated(): Boolean = userId != null
    override fun setAuthenticated(isAuthenticated: Boolean) {}
    override fun getName(): String? = userId
}
