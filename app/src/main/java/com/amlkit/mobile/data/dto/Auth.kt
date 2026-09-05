package com.amlkit.mobile.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class OperatorDto(
    val operator_id: Int,
    val org_id: Int,
    val name: String,
    val role: String,
    val email: String,
)

@Serializable
data class AuthResponse(
    val token: String,
    val operator: OperatorDto,
)

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class RegisterOrgRequest(
    val org_name: String,
    val name: String,
    val email: String,
    val password: String,
)

/** What `register-organization` actually returns: the account is created but
 * not yet usable -- no session token until the emailed link is verified. */
@Serializable
data class RegisterOrgResponse(
    val status: String,
    val message: String,
    val email: String,
    val dev_verification_token: String? = null,
)

@Serializable
data class VerifyEmailRequest(val token: String)

@Serializable
data class ResendVerificationRequest(val email: String)

/** Body of every resend-verification response -- always the same generic
 * message regardless of whether the email exists, by server design. */
@Serializable
data class ResendVerificationResponse(val message: String)

@Serializable
data class SetupCheckResponse(val valid: Boolean, val org_name: String? = null)

@Serializable
data class SetupSubmitRequest(
    val token: String,
    val name: String,
    val email: String,
    val password: String,
)

@Serializable
data class MeResponse(val operator: OperatorDto)

/** Body of every non-2xx response from the API -- FastAPI's default HTTPException shape. */
@Serializable
data class ApiErrorBody(val detail: String? = null)
