package org.example.ecommerceexamplebackendkotlinkafka.auth

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class RegisterRequest(
    @field:NotBlank(message = "email is required")
    @field:Email(message = "email must be a valid email address")
    val email: String?,

    @field:NotBlank(message = "password is required")
    @field:Size(min = 8, message = "password must be at least 8 characters")
    val password: String?
)

data class LoginRequest(
    @field:NotBlank(message = "email is required")
    @field:Email(message = "email must be a valid email address")
    val email: String?,

    @field:NotBlank(message = "password is required")
    val password: String?
)

data class AuthResponse(val token: String)
data class MeResponse(val userId: String, val email: String)
