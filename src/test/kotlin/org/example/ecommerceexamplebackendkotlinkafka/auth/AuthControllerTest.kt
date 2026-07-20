package org.example.ecommerceexamplebackendkotlinkafka.auth

import com.fasterxml.jackson.databind.ObjectMapper
import org.example.ecommerceexamplebackendkotlinkafka.config.SecurityConfig
import org.junit.jupiter.api.Test
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie

@WebMvcTest(AuthController::class)
@EnableWebSecurity
@Import(SecurityConfig::class, JwtFilter::class, JwtUtil::class)
@TestPropertySource(properties = [
    "jwt.secret=test-secret-test-secret-test-secret-test-secret",
    "jwt.expiration-ms=60000",
    "cookie.secure=false"
])
class AuthControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    lateinit var userRepository: UserRepository

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    lateinit var cacheManager: org.springframework.cache.CacheManager

    @Autowired
    lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    lateinit var jwtUtil: JwtUtil

    val objectMapper = ObjectMapper()

    @Test
    fun `register creates a user, hashes the password, and sets an auth cookie`() {
        whenever(userRepository.existsByEmail("new@example.com")).thenReturn(false)
        whenever(userRepository.save(org.mockito.kotlin.any())).thenAnswer { it.arguments[0] }

        mockMvc.post("/auth/register") {
            contentType = org.springframework.http.MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(RegisterRequest("new@example.com", "password123"))
        }.andExpect {
            status { isCreated() }
            jsonPath("$.token") { exists() }
            cookie { exists("auth_token") }
        }

        val captor = org.mockito.kotlin.argumentCaptor<User>()
        verify(userRepository).save(captor.capture())
        assert(captor.firstValue.email == "new@example.com")
        assert(passwordEncoder.matches("password123", captor.firstValue.passwordHash))
    }

    @Test
    fun `register rejects a duplicate email with 409`() {
        whenever(userRepository.existsByEmail("taken@example.com")).thenReturn(true)

        mockMvc.post("/auth/register") {
            contentType = org.springframework.http.MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(RegisterRequest("taken@example.com", "password123"))
        }.andExpect {
            status { isConflict() }
        }
    }

    @Test
    fun `register rejects an invalid payload with 400 and field errors`() {
        mockMvc.post("/auth/register") {
            contentType = org.springframework.http.MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(RegisterRequest("not-an-email", "short"))
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.errors") { exists() }
        }
    }

    @Test
    fun `login succeeds with correct credentials and returns a token`() {
        val hash = passwordEncoder.encode("password123")
        whenever(userRepository.findByEmail("user@example.com")).thenReturn(
            User(id = "id-1", email = "user@example.com", passwordHash = hash!!)
        )

        mockMvc.post("/auth/login") {
            contentType = org.springframework.http.MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(LoginRequest("user@example.com", "password123"))
        }.andExpect {
            status { isOk() }
            jsonPath("$.token") { exists() }
            cookie { exists("auth_token") }
        }
    }

    @Test
    fun `login rejects wrong password with 401`() {
        val hash = passwordEncoder.encode("password123")
        whenever(userRepository.findByEmail("user@example.com")).thenReturn(
            User(id = "id-1", email = "user@example.com", passwordHash = hash!!)
        )

        mockMvc.post("/auth/login") {
            contentType = org.springframework.http.MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(LoginRequest("user@example.com", "wrong-password"))
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun `login rejects unknown email with 401`() {
        whenever(userRepository.findByEmail("ghost@example.com")).thenReturn(null)

        mockMvc.post("/auth/login") {
            contentType = org.springframework.http.MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(LoginRequest("ghost@example.com", "password123"))
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun `me is rejected without a token`() {
        mockMvc.get("/auth/me").andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun `me returns the current user when a valid bearer token is supplied`() {
        whenever(userRepository.findByEmail("user@example.com")).thenReturn(
            User(id = "id-1", email = "user@example.com", passwordHash = "irrelevant")
        )
        val token = jwtUtil.generate("user@example.com")

        mockMvc.get("/auth/me") {
            header("Authorization", "Bearer $token")
        }.andExpect {
            status { isOk() }
            jsonPath("$.email") { value("user@example.com") }
            jsonPath("$.userId") { value("id-1") }
        }
    }
}
