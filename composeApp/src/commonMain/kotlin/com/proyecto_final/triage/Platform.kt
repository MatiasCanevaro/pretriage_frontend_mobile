package com.proyecto_final.triage

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform