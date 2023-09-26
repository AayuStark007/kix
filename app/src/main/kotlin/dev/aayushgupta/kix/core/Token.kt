package dev.aayushgupta.kix.core

import dev.aayushgupta.kix.util.NULL

data class Token(
    val type: TokenType,
    val lexeme: String,
    val literal: Any,
    val line: Int
) {
    override fun toString(): String {
        val lit = if (literal is NULL) "NULL" else literal
        return "[$type::$lexeme => $lit]"
    }
}