package dev.aayushgupta.kix.core;

interface KixCallable {
    fun call(interpreter: Interpreter, args: List<Any>): Any
    fun arity(): Int
}
