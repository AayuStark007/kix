package dev.aayushgupta.kix.core

import dev.aayushgupta.kix.runtimeError
import dev.aayushgupta.kix.util.Null
import java.math.BigDecimal
import java.math.RoundingMode

class Interpreter : Expr.Visitor<Any> {

    fun interpret(expression: Expr) {
        try {
            val value = evaluate(expression)
            println(stringify(value))
        } catch (error: RuntimeError) {
            runtimeError(error)
        }
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
            else -> Null
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
            else -> Null
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

    private fun evaluate(expr: Expr): Any {
        return expr.accept(this)
    }

    // false and nil are false, and everything else is true
    private fun isTruthy(obj: Any): Boolean {
        if (obj is Null) return false
        if (obj == Null) return false
        if (obj is Boolean) return obj
        return true
    }

    private fun isEqual(a: Any, b: Any): Boolean {
        if (a == Null && b == Null) return true
        if (a == Null || a is Null) return false

        return a == b
    }

    private fun stringify(obj: Any): String {
        if (obj == Null || obj is Null) return "nil"

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