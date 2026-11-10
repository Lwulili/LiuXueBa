package com.example.liuxueba.di

import com.example.liuxueba.BuildConfig
import com.example.liuxueba.data.auth.RemoteAuthRepository
import com.example.liuxueba.data.remote.ApiClient
import com.example.liuxueba.domain.auth.AuthRepository

/** Production dependency assembly; presentation depends only on the domain interface. */
object AppContainer {
    val authRepository: AuthRepository by lazy {
        RemoteAuthRepository(ApiClient.createAuthApi(BuildConfig.API_BASE_URL))
    }
}
