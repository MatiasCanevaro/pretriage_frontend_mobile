package com.proyecto_final.triage.network

import kotlinx.serialization.Serializable

@Serializable
data class Auth0LoginRequest(
    val grant_type: String = "http://auth0.com/oauth/grant-type/password-realm",
    val username: String,
    val password: String,
    val client_id: String,
    val audience: String = "http://localhost:8080",
    val realm: String = "Username-Password-Authentication",
    val scope: String = "openid profile email"
)

@Serializable
data class Auth0LoginResponse(
    val id_token: String? = null,
    val access_token: String? = null,
    val error: String? = null,
    val error_description: String? = null
)