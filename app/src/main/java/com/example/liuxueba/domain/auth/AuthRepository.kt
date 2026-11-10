package com.example.liuxueba.domain.auth

interface AuthRepository {
    suspend fun register(username: String, email: String, password: String): AuthUser
    suspend fun login(email: String, password: String): AuthSession
}

data class AuthUser(
    val id: String,
    val username: String,
    val email: String,
    val nickname: String,
    val role: String
)

/** Credentials stay in memory for this session; never include them in generated toString output. */
class AuthSession(val user: AuthUser, val accessToken: String, val expiresInSeconds: Int)

enum class AuthFailure { NETWORK, INVALID_CREDENTIALS, ACCOUNT_EXISTS, INVALID_INPUT, SERVICE_UNAVAILABLE }

class AuthException(val reason: AuthFailure) : Exception(reason.name)
