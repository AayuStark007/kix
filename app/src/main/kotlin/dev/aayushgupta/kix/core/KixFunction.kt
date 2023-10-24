package dev.aayushgupta.kix.core

class KixFunction(
    private val declaration: Stmt.Function,
    private val closure: Environment): KixCallable {

    override fun call(interpreter: Interpreter, args: List<Any>): Any {
        val environment = Environment(closure)
        for (i in 0..<declaration.params.size) {
            environment.define(declaration.params[i].lexeme, args[i])
        }

        return try {
            interpreter.executeBlock(declaration.body, environment)
        } catch (returnValue: Return) {
            returnValue.value
        }
    }

    override fun arity(): Int {
        return declaration.params.size
    }

    override fun toString(): String {
        return "<fn ${declaration.name.lexeme}>"
    }
}