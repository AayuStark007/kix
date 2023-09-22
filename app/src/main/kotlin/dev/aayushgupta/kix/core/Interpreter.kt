package dev.aayushgupta.kix.core

import dev.aayushgupta.kix.runtimeError
import dev.aayushgupta.kix.util.NULL
import java.math.BigDecimal
import java.math.RoundingMode

class Interpreter : Expr.Visitor<Any>, Stmt.Visitor<Unit> {

    private val environment = Environment()

    fun interpret(statements: List<Stmt>) {
        try {
            statements.forEach { statement ->
                execute(statement)
            }
        } catch (error: RuntimeError) {
            runtimeError(error)
        }
    }

    override fun visitAssignExpr(expr: Expr.Assign): Any {
        val value = evaluate(expr.value)
        environment.assign(expr.name, value)
        return value
    }

    override fun visitTernaryExpr(expr: Expr.Ternary): Any {
        return if (isTruthy(expr.condition)) {
            evaluate(expr.expTrue)
        } else evaluate(expr.expFalse)
    }

    override fun visitBinaryExpr(expr: Expr.Binary): Any {
        val left = evaluate(expr.left)
        val right = evaluate(expr.right)

        /**
         * TODO:
         *  - Support for comparison b/w strings
         *  - Explicit check for operand type mismatch to generate better runtime error messages
         *  - Detect invalid scenarios like, divide by 0, etc
         *  - Support for more operators like ^, %, etc
         */

        return when (expr.operator.type) {
            TokenType.GREATER -> {
                checkNumberOperands(expr.operator, left, right)
                (left as Double) > (right as Double)
            }
            TokenType.GREATER_EQUAL -> {
                checkNumberOperands(expr.operator, left, right)
                (left as Double) >= (right as Double)
            }
            TokenType.LESS -> {
                checkNumberOperands(expr.operator, left, right)
                (left as Double) < (right as Double)
            }
            TokenType.LESS_EQUAL -> {
                checkNumberOperands(expr.operator, left, right)
                (left as Double) <= (right as Double)
            }
            TokenType.MINUS -> {
                checkNumberOperands(expr.operator, left, right)
                (left as Double) - (right as Double)
            }
            TokenType.PLUS -> {
                evalPlusOperands(expr.operator, left, right)
            }
            TokenType.SLASH -> {
                checkNumberOperands(expr.operator, left, right)
                (left as Double) / (right as Double)
            }
            TokenType.STAR -> {
                checkNumberOperands(expr.operator, left, right)
                (left as Double) * (right as Double)
            }
            TokenType.BANG_EQUAL -> !isEqual(left, right)
            TokenType.EQUAL_EQUAL -> isEqual(left, right)
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

    override fun visitGroupingExpr(expr: Expr.Grouping): Any {
        return evaluate(expr.expression)
    }

    override fun visitLiteralExpr(expr: Expr.Literal): Any {
        return expr.value
    }

    override fun visitUnaryExpr(expr: Expr.Unary): Any {
        val right = evaluate(expr.right)

        return when(expr.operator.type) {
            TokenType.BANG -> !isTruthy(right)
            TokenType.MINUS -> {
                checkNumberOperand(expr.operator, right)
                -(right as Double)
            }
            // Unreachable
            else -> NULL
        }
    }

    override fun visitVariableExpr(expr: Expr.Variable): Any {
        return environment.get(expr.name)
    }

    override fun visitNullExpr(expr: Expr.Null): Any {
        return NULL
    }

    private fun checkNumberOperand(operator: Token, operand: Any) {
        if (operand is Double) return
        throw RuntimeError(operator, "Operand must be a number.")
    }

    private fun checkNumberOperands(operator: Token, left: Any, right: Any) {
        if (left is Double && right is Double) return
        throw RuntimeError(operator, "Operand must be a number.")
    }

    private fun evaluate(expr: Expr): Any {
        return expr.accept(this)
    }

    private fun execute(stmt: Stmt) {
        stmt.accept(this)
    }

    override fun visitExpressionStmt(stmt: Stmt.Expression) {
        evaluate(stmt.expression)
    }

    override fun visitPrintStmt(stmt: Stmt.Print) {
        val value = evaluate(stmt.expression)
        println(stringify(value))
    }

    override fun visitVarStmt(stmt: Stmt.Var) {
        var value: Any = NULL
        if (stmt.initializer != Expr.Null) {
            value = evaluate(stmt.initializer)
        }

        environment.define(stmt.name.lexeme, value)
    }

    override fun visitNullStmt(stmt: Stmt.Null) {
        // Do nothing
    }

    // false and nil are false, and everything else is true
    private fun isTruthy(obj: Any): Boolean {
        if (obj is NULL) return false
        if (obj == NULL) return false
        if (obj is Boolean) return obj
        return true
    }

    private fun isEqual(a: Any, b: Any): Boolean {
        if (a == NULL && b == NULL) return true
        if (a == NULL || a is NULL) return false

        return a == b
    }

    private fun stringify(obj: Any): String {
        if (obj == NULL || obj is NULL) return "nil"

        if (obj is Double) {
            var text = obj.toString()
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length - 2)
            }
            return text
        }

        return obj.toString()
    }
}