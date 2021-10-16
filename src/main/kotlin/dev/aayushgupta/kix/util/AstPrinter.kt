package dev.aayushgupta.kix.util

import dev.aayushgupta.kix.core.Expr
import dev.aayushgupta.kix.core.Token
import dev.aayushgupta.kix.core.TokenType

class AstPrinter : Expr.Visitor<String> {

    fun print(expr: Expr): String {
        return expr.accept(this)
    }

    override fun visitBinaryExpr(expr: Expr.Binary): String {
        return parenthesize(expr.operator.lexeme, expr.left, expr.right)
    }

    override fun visitGroupingExpr(expr: Expr.Grouping): String {
        return parenthesize("group", expr.expression)
    }

    override fun visitLiteralExpr(expr: Expr.Literal): String {
        if (expr.value == Null) return "nil"
        return "${expr.value}"
    }

    override fun visitUnaryExpr(expr: Expr.Unary): String {
        return parenthesize(expr.operator.lexeme, expr.right)
    }

    private fun parenthesize(name: String, vararg exprs: Expr): String {
        val builder = StringBuilder()

        builder.append("(").append(name)
        exprs.forEach { expr ->
            builder.append(" ")
            builder.append(expr.accept(this))
        }
        builder.append(")")

        return "$builder"
    }

    // optional main
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val expression = Expr.Binary(
                Expr.Unary(
                    Token(TokenType.MINUS, "-", Null, 1),
                    Expr.Literal(123),
                ),
                Token(TokenType.STAR, "*", Null, 1),
                Expr.Grouping(
                    Expr.Literal(45.67),
                )
            )

            println(AstPrinter().print(expression))
        }
    }
}
