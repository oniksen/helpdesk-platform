package org.lpmti.helpdeskplatform

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform