package org.example.ecommerceexamplebackendkotlinkafka.auth

import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/auth")
class AuthController(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtUtil: JwtUtil,
    @param:Value("\${cookie.secure}") private val cookieSecure: Boolean
) {

    @PostMapping("/register")
    fun register(
        @Valid @RequestBody request: RegisterRequest,
        response: HttpServletResponse
    ): ResponseEntity<AuthResponse> {
        if (userRepository.existsByEmail(request.email!!)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build()
        }
        val user = User(email = request.email!!, passwordHash = passwordEncoder.encode(request.password!!)!!)
        userRepository.save(user)
        val token = jwtUtil.generate(user.email)
        response.addCookie(authCookie(token))
        return ResponseEntity.status(HttpStatus.CREATED).body(AuthResponse(token))
    }

    @PostMapping("/login")
    fun login(
        @Valid @RequestBody request: LoginRequest,
        response: HttpServletResponse
    ): ResponseEntity<AuthResponse> {
        val user = userRepository.findByEmail(request.email!!)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        if (!passwordEncoder.matches(request.password, user.passwordHash)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }

        val token = jwtUtil.generate(user.email)
        response.addCookie(authCookie(token))
        return ResponseEntity.ok(AuthResponse(token))
    }

    @GetMapping("/me")
    fun me(@AuthenticationPrincipal email: String): ResponseEntity<MeResponse> {
        val user = userRepository.findByEmail(email)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        return ResponseEntity.ok(MeResponse(userId = user.id, email = user.email))
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationErrors(e: MethodArgumentNotValidException): ResponseEntity<Map<String, Any>> {
        val errors = e.bindingResult.fieldErrors.map { fieldError ->
            mapOf(
                "field" to fieldError.field,
                "message" to (fieldError.defaultMessage ?: "Invalid value")
            )
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            mapOf("errors" to errors)
        )
    }

    private fun authCookie(token: String) = Cookie("auth_token", token).apply {
        isHttpOnly = true
        secure = cookieSecure
        path = "/"
        maxAge = (jwtUtil.expirationMs / 1000).toInt()
    }
}
