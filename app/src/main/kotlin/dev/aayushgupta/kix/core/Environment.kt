package dev.aayushgupta.kix.core

import dev.aayushgupta.kix.util.NULL

internal class Environment {
    private val values = hashMapOf<String, Any>()

    fun get(name: Token): Any {
        if (values.containsKey(name.lexeme)) {
            with (values.getOrDefault(name.lexeme, NULL)) {
                if (this == NULL) throw RuntimeError(name, "Undefined variable '${name.lexeme}'.")
                else return this
            }
        }

        throw RuntimeError(name, "Undefined variable '${name.lexeme}'.")
    }

    fun define(name: String, value: Any) {
        values[name] = value
    }

    fun assign(name: Token, value: Any) {
        if (values.containsKey(name.lexeme)) {
            values[name.lexeme] = value
            return
        }

        throw RuntimeError(name, "Undefined variable '${name.lexeme}'.")
    }
}
