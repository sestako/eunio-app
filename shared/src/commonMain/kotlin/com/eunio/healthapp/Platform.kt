package com.eunio.healthapp

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform