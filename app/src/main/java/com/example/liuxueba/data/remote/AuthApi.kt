package com.example.liuxueba.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface AuthApi {
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequestDto): Response<UserDto>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequestDto): Response<TokenDto>

    @GET("auth/me")
    suspend fun currentUser(@Header("Authorization") authorization: String): Response<UserDto>
}

// These request classes deliberately avoid a generated toString containing the password.
class RegisterRequestDto(val username: String, val email: String, val password: String)
class LoginRequestDto(val email: String, val password: String)

data class UserDto(
    val id: String? = null,
    val username: String? = null,
    val email: String? = null,
    val nickname: String? = null,
    val role: String? = null
)

class TokenDto(
    @SerializedName("access_token") val accessToken: String? = null,
    @SerializedName("token_type") val tokenType: String? = null,
    @SerializedName("expires_in") val expiresIn: Int? = null
)
