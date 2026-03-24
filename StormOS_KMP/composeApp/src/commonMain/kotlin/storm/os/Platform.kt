package storm.os

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform