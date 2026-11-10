package com.example.liuxueba.data.auth

import com.example.liuxueba.data.remote.AuthApi
import com.example.liuxueba.data.remote.LoginRequestDto
import com.example.liuxueba.data.remote.RegisterRequestDto
import com.example.liuxueba.data.remote.UserDto
import com.example.liuxueba.domain.auth.AuthException
import com.example.liuxueba.domain.auth.AuthFailure
import com.example.liuxueba.domain.auth.AuthRepository
import com.example.liuxueba.domain.auth.AuthSession
import com.example.liuxueba.domain.auth.AuthUser
import com.google.gson.JsonParseException
import kotlinx.coroutines.CancellationException
import retrofit2.Response
import java.io.IOException

class RemoteAuthRepository(private val api: AuthApi) : AuthRepository {
    override suspend fun register(username: String, email: String, password: String): AuthUser =
        request { api.register(RegisterRequestDto(username, email, password)).bodyOrThrow().toDomain() }

    override suspend fun login(email: String, password: String): AuthSession = request {
        val token = api.login(LoginRequestDto(email, password)).bodyOrThrow()
        val accessToken = token.accessToken
        val expiresIn = token.expiresIn
        if (accessToken.isNullOrBlank() || token.tokenType != "bearer" || expiresIn == null || expiresIn <= 0) {
            throw AuthException(AuthFailure.SERVICE_UNAVAILABLE)
        }
        val user = api.currentUser("Bearer $accessToken").bodyOrThrow().toDomain()
        AuthSession(user, accessToken, expiresIn)
    }

    private suspend fun <T> request(block: suspend () -> T): T = try {
        block()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: JsonParseException) {
        throw AuthException(AuthFailure.SERVICE_UNAVAILABLE)
    } catch (_: IOException) {
        throw AuthException(AuthFailure.NETWORK)
    }

    private fun <T> Response<T>.bodyOrThrow(): T {
        if (!isSuccessful) {
            errorBody()?.close()
            throw AuthException(when (code()) {
                401 -> AuthFailure.INVALID_CREDENTIALS
                409 -> AuthFailure.ACCOUNT_EXISTS
                422 -> AuthFailure.INVALID_INPUT
                else -> AuthFailure.SERVICE_UNAVAILABLE
            })
        }
        return body() ?: throw AuthException(AuthFailure.SERVICE_UNAVAILABLE)
    }

    private fun UserDto.toDomain(): AuthUser {
        fun required(value: String?): String = value?.takeIf { it.isNotBlank() }
            ?: throw AuthException(AuthFailure.SERVICE_UNAVAILABLE)
        if (role != "user" && role != "admin") throw AuthException(AuthFailure.SERVICE_UNAVAILABLE)
        return AuthUser(required(id), required(username), required(email), required(nickname), required(role))
    }
}
