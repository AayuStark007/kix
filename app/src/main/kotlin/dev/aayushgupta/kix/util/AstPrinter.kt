package dev.aayushgupta.kix.util

import dev.aayushgupta.kix.core.Expr
import dev.aayushgupta.kix.core.Expr.*
import dev.aayushgupta.kix.core.Token
import dev.aayushgupta.kix.core.TokenType

class AstPrinter : Visitor<String> {

    fun print(expr: Expr): String {
        return expr.accept(this)
    }

    override fun visitAssignExpr(expr: Assign): String {
        return parenthesize("assign", Literal(expr.name), expr.value)
    }

    override fun visitTernaryExpr(expr: Ternary): String {
        return parenthesize("ternary", expr.condition, expr.expTrue, expr.expFalse)
    }

    override fun visitBinaryExpr(expr: Binary): String {
        return parenthesize(expr.operator.lexeme, expr.left, expr.right)
    }

    override fun visitGroupingExpr(expr: Grouping): String {
        return parenthesize("group", expr.expression)
    }

    override fun visitLiteralExpr(expr: Literal): String {
        if (expr.value == NULL) return "nil"
        return "${expr.value}"
    }

    override fun visitUnaryExpr(expr: Unary): String {
        return parenthesize(expr.operator.lexeme, expr.right)
    }

    override fun visitNullExpr(expr: Expr.Null): String {
        return "nil"
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
            val expression = // (-123 * (45.67)) ? ("positive") : ("negative")
            Ternary(
                Binary(
                    Binary(
                        Unary(
                            Token(TokenType.MINUS, "-", NULL, 1),
                            Literal(123),
                        ),
                        Token(TokenType.STAR, "*", NULL, 1),
                        Grouping(
                            Literal(45.67),
                        )
                    ),
                    Token(TokenType.GREATER, ">", NULL, 1),
                    Grouping(Literal(0))
                ),
                Grouping(Literal("positive")),
                Grouping(Literal("negative"))
            )

            println(AstPrinter().print(expression))
        }
    }

    override fun visitVariableExpr(expr: Variable): String {
        return parenthesize("variable", Literal(expr.name))
    }


}
