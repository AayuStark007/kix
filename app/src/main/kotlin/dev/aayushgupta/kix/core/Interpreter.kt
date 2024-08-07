package dev.aayushgupta.kix.core

import dev.aayushgupta.kix.runtimeError
import dev.aayushgupta.kix.util.NULL
import java.math.BigDecimal
import java.math.RoundingMode

// TODO: make environment immutable by passing it as arg to visitors
class Interpreter : Stmt.Visitor<Unit>, Expr.Visitor<Any> {

    private var environment: Environment? = null
    private val globals: MutableMap<String, Any> = mutableMapOf()
    private val locals: MutableMap<Expr, Local> = mutableMapOf()

    private data class Local(val distance: Int, val slot: Int)

    init {
        // clock: returns unix timestamp in millis
        globals["clock"] = object : KixCallable {
            override fun call(interpreter: Interpreter, args: List<Any>): Any {
                return System.currentTimeMillis().toDouble()
            }

            override fun arity() = 0

            override fun toString() = "<native fn>"
        }

        // print: print whatever is passed
        globals["print"] = object : KixCallable {
            override fun call(interpreter: Interpreter, args: List<Any>): Any {
                return print(args.joinToString(" "))
            }

            override fun arity() = 1 // TODO: varargs for now assume exact one arg is required

            override fun toString() = "<native fn>"
        }

        // println: print whatever is passed followed by a newline
        globals["println"] = object : KixCallable {
            override fun call(interpreter: Interpreter, args: List<Any>): Any {
                return println(args.joinToString(" "))
            }

            override fun arity() = 1 // TODO: varargs for now assume exact one arg is required

            override fun toString() = "<native fn>"
        }
    }

    fun interpret(statements: List<Stmt>) {
        try {
            statements.forEach { statement ->
                execute(statement)
            }
        } catch (error: RuntimeError) {
            runtimeError(error)
        }
    }

    // region statements
    override fun visitBlockStmt(stmt: Stmt.Block) {
        executeBlock(stmt.statements, Environment(environment))
    }

    override fun visitExpressionStmt(stmt: Stmt.Expression) {
        evaluate(stmt.expression)
    }

    override fun visitFunctionStmt(stmt: Stmt.Function) {
        val function = KixFunction(stmt, environment)
        define(stmt.name, function)
    }

    override fun visitIfStmt(stmt: Stmt.If) {
        if (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.thenBranch)
        } else execute(stmt.elseBranch)
    }

    override fun visitPrintStmt(stmt: Stmt.Print) {
        val value = evaluate(stmt.expression)
        println(stringify(value))
    }

    override fun visitReturnStmt(stmt: Stmt.Return) {
        val value = evaluate(stmt.value)
        throw Return(value)
    }

    override fun visitVarStmt(stmt: Stmt.Var) {
        var value: Any = NULL
        if (stmt.initializer != Expr.Null) {
            value = evaluate(stmt.initializer)
        }
        define(stmt.name, value)
    }

    override fun visitWhileStmt(stmt: Stmt.While) {
        while (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.body)
        }
    }

    override fun visitNullStmt(stmt: Stmt.Null) = Unit
    //endregion

    // region expressions
    override fun visitAssignExpr(expr: Expr.Assign) =
        evaluate(expr.value).also { value ->
            val local = locals[expr]
            if (local != null) {
                environment?.assignAt(local.distance, local.slot, value)
            } else {
                if (globals.containsKey(expr.name.lexeme)) {
                    globals[expr.name.lexeme] = value
                } else throw RuntimeError(expr.name, "Undefined variable '${expr.name.lexeme}'.")
            }
        }

    override fun visitTernaryExpr(expr: Expr.Ternary) =
        if (isTruthy(evaluate(expr.condition))) {
            evaluate(expr.expTrue)
        } else evaluate(expr.expFalse)

    override fun visitBinaryExpr(expr: Expr.Binary) =
        evaluateBinaryExpr(expr.operator, evaluate(expr.left), evaluate(expr.right))

    override fun visitCallExpr(expr: Expr.Call): Any {
        val callee = evaluate(expr.callee)
        val args = expr.args.map(::evaluate)
        if (callee !is KixCallable) {
            throw RuntimeError(
                expr.paren,
                "Can only call functions and classes."
            )
        }
        if (args.size != callee.arity()) {
            throw RuntimeError(
                expr.paren,
                "Expected ${callee.arity()} arguments but got ${args.size}."
            )
        }
        return callee.call(this, args)
    }

    override fun visitGroupingExpr(expr: Expr.Grouping) = evaluate(expr.expression)

    override fun visitLiteralExpr(expr: Expr.Literal) = expr.value

    override fun visitLogicalExpr(expr: Expr.Logical): Any {
        val left = evaluate(expr.left)
        return when {
            expr.operator.type == TokenType.OR && isTruthy(left) -> left
            expr.operator.type == TokenType.AND && !isTruthy(left) -> left
            else -> evaluate(expr.right)
        }
    }

    override fun visitUnaryExpr(expr: Expr.Unary): Any {
        val right = evaluate(expr.right)
        return evaluateUnaryExpr(expr, right)
    }

    override fun visitVariableExpr(expr: Expr.Variable) = lookUpVariable(expr.name, expr)

    override fun visitNullExpr(expr: Expr.Null) = NULL

    //endregion

    // region helpers
    private fun execute(stmt: Stmt) {
        stmt.accept(this)
    }

    fun resolve(expr: Expr, depth: Int, slot: Int) {
        // either we can store the depth info in the syntax tree node itself,
        // but it will require changes to the generator and require traversal to resolve
        // or, we store in a lookup table which will be much faster
        locals[expr] = Local(depth, slot)
    }

    private fun define(name: Token, value: Any) {
        environment?.define(value) ?: run {
            globals[name.lexeme] = value
        }
    }

    private fun lookUpVariable(name: Token, expr: Expr): Any {
        val local = locals[expr]
        return if (local != null) {
            environment?.getAt(local.distance, local.slot) ?: throw RuntimeError(
                name,
                "Unable to find variable '${name.lexeme}'"
            )
        } else {
            globals.getOrElse(name.lexeme) { throw RuntimeError(name, "Undefined variable '${name.lexeme}'.") }
        }
    }

    private fun evaluate(expr: Expr): Any {
        return expr.accept(this)
    }

    private fun evaluateBinaryExpr(operator: Token, left: Any, right: Any): Any {
        /**
         * TODO:
         *  - Support for comparison b/w strings
         *  - Explicit check for operand type mismatch to generate better runtime error messages
         *  - Detect invalid scenarios like, divide by 0, etc
         *  - Support for more operators like ^, %, etc
         */
        return when (operator.type) {
            TokenType.GREATER -> {
                checkNumberOperands(operator, left, right)
                (left as Double) > (right as Double)
            }

            TokenType.GREATER_EQUAL -> {
                checkNumberOperands(operator, left, right)
                (left as Double) >= (right as Double)
            }

            TokenType.LESS -> {
                checkNumberOperands(operator, left, right)
                (left as Double) < (right as Double)
            }

            TokenType.LESS_EQUAL -> {
                checkNumberOperands(operator, left, right)
                (left as Double) <= (right as Double)
            }

            TokenType.MINUS -> {
                checkNumberOperands(operator, left, right)
                (left as Double) - (right as Double)
            }

            TokenType.PLUS -> {
                evalPlusOperands(operator, left, right)
            }

            TokenType.SLASH -> {
                checkNumberOperands(operator, left, right)
                (left as Double) / (right as Double)
            }

            TokenType.STAR -> {
                checkNumberOperands(operator, left, right)
                (left as Double) * (right as Double)
            }

            TokenType.BANG_EQUAL -> !isEqual(left, right)
            TokenType.EQUAL_EQUAL -> isEqual(left, right)
            // Unreachable
            else -> NULL
        }
    }

    private fun evaluateUnaryExpr(expr: Expr.Unary, right: Any): Any {
        return when (expr.operator.type) {
            TokenType.BANG -> !isTruthy(right)
            TokenType.MINUS -> {
                checkNumberOperand(expr.operator, right)
                -(right as Double)
            }
            // Unreachable
            else -> NULL
        }
    }

    private fun evalPlusOperands(operator: Token, left: Any, right: Any): Any {
        return when {
            (left is String && right is String) -> left + right
            (left is String && right is Double) ->
                "$left${BigDecimal(right).setScale(0, RoundingMode.HALF_UP)}"

            (left is Double && right is String) ->
                "${BigDecimal(left).setScale(0, RoundingMode.HALF_UP)}$right"

            (left is Double && right is Double) -> left + right
            else -> throw RuntimeError(operator, "Invalid operands: $left and $right for '+'")
        }
    }

    private fun checkNumberOperand(operator: Token, operand: Any) {
        if (operand is Double) return
        throw RuntimeError(operator, "Operand must be a number.")
    }

    private fun checkNumberOperands(operator: Token, left: Any, right: Any) {
        if (left is Double && right is Double) return
        throw RuntimeError(operator, "Operand must be a number.")
    }

    fun executeBlock(statements: List<Stmt>, environment: Environment) {
        // save the current env beforehand
        val previous = this.environment
        try {
            // now replace our env with the one being passed (expectation: current instance env will be the parent of the local env)
            this.environment = environment
            statements.forEach(::execute)
        } finally {
            // finally restore the original environment
            this.environment = previous
        }
    }

    // false and nil are false, and everything else is true
    private fun isTruthy(obj: Any) = when (obj) {
        NULL, is NULL -> false
        is Boolean -> obj
        else -> true
    }

    private fun isEqual(a: Any, b: Any) = when {
        a == NULL && b == NULL -> true
        a == NULL || a is NULL -> false
        else -> a == b
    }

    private fun stringify(obj: Any) = when (obj) {
        NULL, is NULL -> "nil"
        is Double -> {
            var text = obj.toString()
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length - 2)
            }
            text
        }

        else -> obj.toString()
    }

    // endregion
}