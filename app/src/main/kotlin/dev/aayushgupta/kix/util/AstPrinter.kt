package dev.aayushgupta.kix.util

import dev.aayushgupta.kix.core.Expr
import dev.aayushgupta.kix.core.Expr.*
import dev.aayushgupta.kix.core.Stmt
import dev.aayushgupta.kix.core.Token
import dev.aayushgupta.kix.core.TokenType

class AstPrinter : Expr.Visitor<String>, Stmt.Visitor<String> {

    fun print(stmts: List<Stmt>) {
        stmts.forEach {
            println(it.accept(this@AstPrinter))
        }
    }

    fun print(expr: Expr) {
        println(expr.accept(this))
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

    override fun visitNullExpr(expr: Null): String {
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

            AstPrinter().print(expression)
        }
    }

    override fun visitVariableExpr(expr: Variable): String {
        return parenthesize("variable", Literal(expr.name))
    }

    override fun visitBlockStmt(stmt: Stmt.Block): String {
        val builder = StringBuilder().apply {
            append("{ ")
            stmt.statements.forEach { this@apply.append("${it.accept(this@AstPrinter)} ") }
            append(" }")
        }
        return builder.toString()
    }

    override fun visitExpressionStmt(stmt: Stmt.Expression): String {
        return stmt.expression.accept(this)
    }

    override fun visitPrintStmt(stmt: Stmt.Print): String {
        return parenthesize("print", stmt.expression)
    }

    override fun visitVarStmt(stmt: Stmt.Var): String {
        return parenthesize("var ${stmt.name}", stmt.initializer)
    }

    override fun visitNullStmt(stmt: Stmt.Null): String {
        return parenthesize("null")
    }
}
