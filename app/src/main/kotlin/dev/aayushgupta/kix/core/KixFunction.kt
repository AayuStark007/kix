package dev.aayushgupta.kix.core

class KixFunction(private val declaration: Stmt.Function): KixCallable {

    override fun call(interpreter: Interpreter, args: List<Any>): Any {
        val environment = Environment(interpreter.getGlobals())
        for (i in 0..<declaration.params.size) {
            environment.define(declaration.params[i].lexeme, args[i])
        }

        return interpreter.executeBlock(declaration.body, environment)
    }

    override fun arity(): Int {
        return declaration.params.size
    }

    override fun toString(): String {
        return "<fn ${declaration.name.lexeme}>"
    }
}