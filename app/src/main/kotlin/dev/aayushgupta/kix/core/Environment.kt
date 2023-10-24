package dev.aayushgupta.kix.core

import dev.aayushgupta.kix.util.NULL

class Environment(private val enclosing: Environment? = null) {

    private val values = hashMapOf<String, Any>()

    fun get(name: Token): Any {
        with(values.getOrDefault(name.lexeme, NULL)) {
            if (this == NULL) return enclosing?.get(name) ?: throw RuntimeError(
                name,
                "Undefined variable '${name.lexeme}'."
            )
            else return this
        }
    }

    fun define(name: String, value: Any) {
        values[name] = value
    }

    fun assign(name: Token, value: Any) {
        if (values.containsKey(name.lexeme)) {
            values[name.lexeme] = value
            return
        }

        enclosing?.assign(name, value) ?: throw RuntimeError(name, "Undefined variable '${name.lexeme}'.")
    }
}
