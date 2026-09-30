package com.proyecto_final.triage.config

enum class Environment {
    DEV, PROD
}

object AppConfig {
    val environment: Environment = Environment.PROD

    val baseUrl: String
        get() = BackendConfig.BASE_URL

}

