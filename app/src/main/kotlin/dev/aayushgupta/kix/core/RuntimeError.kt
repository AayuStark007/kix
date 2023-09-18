package dev.aayushgupta.kix.core

class RuntimeError(val token: Token, override val message: String) : RuntimeException(message) {
}