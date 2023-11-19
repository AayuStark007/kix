package dev.aayushgupta.kix.core

class Environment(private val enclosing: Environment? = null) {
    private val values = mutableListOf<Any>()

    fun define(value: Any) {
        values.add(value)
    }

    private fun ancestor(distance: Int): Environment {
        var environment = this
        for (i in 0..<distance) {
            environment = environment.enclosing!!
        }
        return environment
    }

    fun getAt(distance: Int, slot: Int): Any {
        return ancestor(distance).values[slot]
    }

    fun assignAt(distance: Int, slot: Int, value: Any) {
        ancestor(distance).values[slot] = value
    }
}
