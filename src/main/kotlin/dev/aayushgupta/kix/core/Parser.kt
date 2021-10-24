package dev.aayushgupta.kix.core

import dev.aayushgupta.kix.core.TokenType.*
import dev.aayushgupta.kix.util.Null

class Parser(private val tokens: List<Token>) {

    private class ParseError : RuntimeException()

    private var current: Int = 0

    fun parse(): Expr {
        return try {
            expression()
        } catch (error: ParseError) {
            Expr.Literal(Null) // return null
        }
    }

    private fun expression(): Expr = equality()
    private fun equality(): Expr = parseLeftAssociative(::comparison, BANG_EQUAL, EQUAL_EQUAL)
    private fun comparison(): Expr = parseLeftAssociative(::term, GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)
    private fun term(): Expr = parseLeftAssociative(::factor, MINUS, PLUS)
    private fun factor(): Expr = parseLeftAssociative(::unary, SLASH, STAR)

    private fun unary(): Expr {
        if (match(BANG, MINUS)) {
            val operator = previous()
            val right = unary()
            return Expr.Unary(operator, right)
        }
        return primary()
    }

    private fun primary(): Expr {
        return when {
            match(FALSE) -> Expr.Literal(false)
            match(TRUE) -> Expr.Literal(true)
            match(NIL) -> Expr.Literal(Null)
            match(NUMBER, STRING) -> Expr.Literal(previous().literal)
            match(LEFT_PAREN) -> {
                val expr = expression()
                consume(RIGHT_PAREN, "Expect ')' after expression.")
                Expr.Grouping(expr)
            }
            else -> throw error(peek(), "Expect expression.")
        }
    }

    // TODO: Use for above use cases
    private inline fun parseLeftAssociative(handle: () -> Expr, vararg types: TokenType): Expr {
        var expr = handle()
        while (match(*types)) {
            val operator = previous()
            val right = handle()
            expr = Expr.Binary(expr, operator, right)
        }
        return expr
    }

    private fun match(vararg types: TokenType): Boolean {
        types.forEach { type ->
            if (check(type)) {
                advance()
                return true
            }
        }
        return false
    }

    private fun consume(type: TokenType, message: String): Token {
        if (check(type)) return advance()
        throw error(peek(), message)
    }

    private fun check(type: TokenType): Boolean {
        if (isAtEnd()) return false
        return peek().type == type
    }

    private fun advance(): Token {
        if (!isAtEnd()) current++
        return previous()
    }

    private fun isAtEnd(): Boolean = peek().type == EOF
    private fun peek(): Token = tokens[current]
    private fun previous(): Token = tokens[current - 1]

    private fun error(token: Token, message: String): ParseError {
        dev.aayushgupta.kix.error(token, message)
        return ParseError()
    }

    private fun synchronize() {
        advance()
        while (!isAtEnd()) {
            if (previous().type == SEMICOLON) return
            when (peek().type) {
                CLASS, FUN, VAR, FOR, IF, WHILE, PRINT, RETURN -> return
                else -> {
                }
            }
            advance()
        }
    }
}
