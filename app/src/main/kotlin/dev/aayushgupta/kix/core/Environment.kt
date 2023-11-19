package dev.aayushgupta.kix.core

class Environment(private val enclosing: Environment? = null) {
    private val values = hashMapOf<String, Any>()

    fun define(name: String, value: Any) {
        values[name] = value
    }

    private fun ancestor(distance: Int): Environment {
        var environment = this
        for (i in 0..<distance) {
            environment = environment.enclosing!!
        }
        return environment
    }

    fun getAt(distance: Int, name: String): Any {
        return ancestor(distance).values[name]!!
    }

    fun assignAt(distance: Int, name: Token, value: Any) {
        ancestor(distance).values[name.lexeme] = value
    }
}
