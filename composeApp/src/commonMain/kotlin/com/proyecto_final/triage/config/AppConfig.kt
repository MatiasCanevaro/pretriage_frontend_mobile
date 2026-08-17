package com.proyecto_final.triage.config

enum class Environment {
    DEV, PROD
}

object AppConfig {
    val environment: Environment = Environment.PROD

    val baseUrl: String
        get() = when (environment) {
            Environment.DEV     -> "http://192.168.0.8:8080"
            Environment.PROD    -> "http://192.168.0.8:8080" //    ESTO DESPUES HAY QUE CAMBIARLO
        }

}

