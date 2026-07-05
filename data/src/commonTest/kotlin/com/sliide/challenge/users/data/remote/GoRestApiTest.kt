package com.sliide.challenge.users.data.remote

import com.sliide.challenge.users.domain.error.AppError
import com.sliide.challenge.users.domain.error.AppResult
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class GoRestApiTest {

    private fun jsonHeaders(vararg extra: Pair<String, String>) = headersOf(
        *(listOf(HttpHeaders.ContentType to listOf("application/json")) +
            extra.map { it.first to listOf(it.second) }).toTypedArray()
    )

    private fun usersJson(vararg ids: Long) = ids.joinToString(
        prefix = "[", postfix = "]"
    ) { """{"id":$it,"name":"User $it","email":"u$it@example.com","gender":"female","status":"active"}""" }

    @Test
    fun `fetches the last page as reported by pagination header`() = runTest {
        val requests = mutableListOf<HttpRequestData>()
        val engine = MockEngine { request ->
            requests += request
            when (requests.size) {
                1 -> respond(usersJson(1, 2), HttpStatusCode.OK, jsonHeaders("x-pagination-pages" to "42"))
                else -> respond(usersJson(830, 831, 832), HttpStatusCode.OK, jsonHeaders())
            }
        }
        val result = GoRestApi(createHttpClient(engine)).fetchLastPageUsers()

        val success = assertIs<AppResult.Success<List<UserDto>>>(result)
        assertEquals(listOf(830L, 831L, 832L), success.value.map { it.id })
        assertEquals(2, requests.size)
        assertEquals("42", requests[1].url.parameters["page"])
    }

    @Test
    fun `single page feed is fetched with one request`() = runTest {
        val requests = mutableListOf<HttpRequestData>()
        val engine = MockEngine { request ->
            requests += request
            respond(usersJson(1), HttpStatusCode.OK, jsonHeaders("x-pagination-pages" to "1"))
        }
        val result = GoRestApi(createHttpClient(engine)).fetchLastPageUsers()

        assertIs<AppResult.Success<List<UserDto>>>(result)
        assertEquals(1, requests.size)
    }

    @Test
    fun `missing pagination header falls back to page one`() = runTest {
        val engine = MockEngine { respond(usersJson(7), HttpStatusCode.OK, jsonHeaders()) }
        val result = GoRestApi(createHttpClient(engine)).fetchLastPageUsers()

        val success = assertIs<AppResult.Success<List<UserDto>>>(result)
        assertEquals(listOf(7L), success.value.map { it.id })
    }

    @Test
    fun `create maps 201 to the created user`() = runTest {
        val engine = MockEngine { request ->
            assertEquals(HttpMethod.Post, request.method)
            respond(
                """{"id":99,"name":"Ada","email":"ada@example.com","gender":"female","status":"active"}""",
                HttpStatusCode.Created,
                jsonHeaders(),
            )
        }
        val result = GoRestApi(createHttpClient(engine))
            .createUser(CreateUserRequest("Ada", "ada@example.com", "female", "active"))

        val success = assertIs<AppResult.Success<UserDto>>(result)
        assertEquals(99, success.value.id)
    }

    @Test
    fun `create maps 422 to field-keyed validation errors`() = runTest {
        val engine = MockEngine {
            respond(
                """[{"field":"email","message":"has already been taken"}]""",
                HttpStatusCode.UnprocessableEntity,
                jsonHeaders(),
            )
        }
        val result = GoRestApi(createHttpClient(engine))
            .createUser(CreateUserRequest("Ada", "taken@example.com", "female", "active"))

        val failure = assertIs<AppResult.Failure>(result)
        val validation = assertIs<AppError.Validation>(failure.error)
        assertEquals("has already been taken", validation.fieldErrors["email"])
    }

    @Test
    fun `create maps 401 to an auth http error`() = runTest {
        val engine = MockEngine {
            respond("""{"message":"Authentication failed"}""", HttpStatusCode.Unauthorized, jsonHeaders())
        }
        val result = GoRestApi(createHttpClient(engine))
            .createUser(CreateUserRequest("Ada", "ada@example.com", "female", "active"))

        val failure = assertIs<AppResult.Failure>(result)
        val http = assertIs<AppError.Http>(failure.error)
        assertTrue(http.isAuth)
        assertEquals("Authentication failed", http.serverMessage)
    }

    @Test
    fun `delete treats 204 and 404 as success`() = runTest {
        for (status in listOf(HttpStatusCode.NoContent, HttpStatusCode.NotFound)) {
            val engine = MockEngine { respond("", status, jsonHeaders()) }
            val result = GoRestApi(createHttpClient(engine)).deleteUser(1)
            assertIs<AppResult.Success<Unit>>(result, "expected success for $status")
        }
    }

    @Test
    fun `delete surfaces server errors`() = runTest {
        val engine = MockEngine { respond("""{"message":"boom"}""", HttpStatusCode.InternalServerError, jsonHeaders()) }
        val result = GoRestApi(createHttpClient(engine)).deleteUser(1)

        val failure = assertIs<AppResult.Failure>(result)
        assertEquals(500, assertIs<AppError.Http>(failure.error).code)
    }

    @Test
    fun `transport failure maps to Network error`() = runTest {
        val engine = MockEngine { throw IOException("socket closed") }
        val result = GoRestApi(createHttpClient(engine)).fetchLastPageUsers()

        val failure = assertIs<AppResult.Failure>(result)
        assertIs<AppError.Network>(failure.error)
    }
}
