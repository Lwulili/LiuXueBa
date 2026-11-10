package com.example.liuxueba.data.auth

import com.example.liuxueba.data.remote.ApiClient
import com.example.liuxueba.domain.auth.AuthException
import com.example.liuxueba.domain.auth.AuthFailure
import com.google.gson.JsonParser
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class RemoteAuthRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var repository: RemoteAuthRepository
    private val userJson = """{"id":"user-123","username":"student","email":"student@example.com","nickname":"同学","role":"user"}"""

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        repository = RemoteAuthRepository(ApiClient.createAuthApi(server.url("/").toString()))
    }

    @After
    fun tearDown() { server.shutdown() }

    @Test
    fun loginFetchesCurrentUserUsingReturnedBearerToken() = runTest {
        server.enqueue(MockResponse().setBody("""{"access_token":"signed-token","token_type":"bearer","expires_in":3600}"""))
        server.enqueue(MockResponse().setBody(userJson))
        val session = repository.login("student@example.com", " study2026 ")

        assertEquals("user-123", session.user.id)
        assertEquals("signed-token", session.accessToken)
        val login = server.takeRequest()
        assertEquals("/auth/login", login.path)
        assertEquals("POST", login.method)
        assertEquals(" study2026 ", JsonParser.parseString(login.body.readUtf8()).asJsonObject["password"].asString)
        val profile = server.takeRequest()
        assertEquals("/auth/me", profile.path)
        assertEquals("Bearer signed-token", profile.getHeader("Authorization"))
    }

    @Test
    fun registrationReturnsUserAndDoesNotSilentlyLogIn() = runTest {
        server.enqueue(MockResponse().setResponseCode(201).setBody(userJson))
        val user = repository.register("student", "student@example.com", "study2026")
        assertEquals("同学", user.nickname)
        assertEquals(1, server.requestCount)
        assertEquals("/auth/register", server.takeRequest().path)
    }

    @Test
    fun httpErrorsAreConvertedToDomainFailures() = runTest {
        val cases = listOf(401 to AuthFailure.INVALID_CREDENTIALS, 409 to AuthFailure.ACCOUNT_EXISTS,
            422 to AuthFailure.INVALID_INPUT, 503 to AuthFailure.SERVICE_UNAVAILABLE)
        cases.forEach { (status, expected) ->
            server.enqueue(MockResponse().setResponseCode(status).setBody("{}"))
            val error = runCatching { repository.login("student@example.com", "study2026") }.exceptionOrNull()
            assertTrue(error is AuthException)
            assertEquals(expected, (error as AuthException).reason)
        }
    }

    @Test
    fun malformedSuccessfulResponseCannotCreateSession() = runTest {
        server.enqueue(MockResponse().setBody("{}"))
        val error = runCatching { repository.login("student@example.com", "study2026") }.exceptionOrNull()
        assertEquals(AuthFailure.SERVICE_UNAVAILABLE, (error as AuthException).reason)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun invalidCurrentUserResponseCannotCreateSession() = runTest {
        server.enqueue(MockResponse().setBody("""{"access_token":"token","token_type":"bearer","expires_in":3600}"""))
        server.enqueue(MockResponse().setBody("{}"))
        val error = runCatching { repository.login("student@example.com", "study2026") }.exceptionOrNull()
        assertEquals(AuthFailure.SERVICE_UNAVAILABLE, (error as AuthException).reason)
    }

    @Test
    fun connectionFailureIsReportedAsNetworkFailure() = runTest {
        val unavailable = RemoteAuthRepository(ApiClient.createAuthApi("http://127.0.0.1:1/"))
        val error = runCatching { unavailable.login("student@example.com", "study2026") }.exceptionOrNull()
        assertEquals(AuthFailure.NETWORK, (error as AuthException).reason)
    }
}
