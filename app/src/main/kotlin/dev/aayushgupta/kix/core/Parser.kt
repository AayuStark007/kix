package dev.aayushgupta.kix.core

import dev.aayushgupta.kix.core.TokenType.*
import dev.aayushgupta.kix.util.NULL

class Parser(private val tokens: List<Token>) {

    private class ParseError : RuntimeException()

    private var current: Int = 0

    fun parse(): List<Stmt> {
        val statements = mutableListOf<Stmt>()
        while (!isAtEnd()) {
            statements.add(declaration())
        }
        return statements
    }

    private fun declaration(): Stmt {
        try {
            if (match(VAR)) return varDeclaration()
            return statement()
        } catch (error: ParseError) {
            synchronize()
            return Stmt.Null
        }
    }

    private fun varDeclaration(): Stmt {
        val name = consume(IDENTIFIER, "Expect variable name.")

        var initializer: Expr = Expr.Null
        if (match(EQUAL)) {
            initializer = expression()
        }

        consume(SEMICOLON, "Expect ';' after variable declaration.")
        return Stmt.Var(name, initializer)
    }

    private fun statement(): Stmt = when {
        match(IF) -> ifStatement()
        match(PRINT) -> printStatement()
        match(WHILE) -> whileStatement()
        match(LEFT_BRACE) -> Stmt.Block(block())
        else -> expressionStatement()
    }

    private fun ifStatement(): Stmt {
        consume(LEFT_PAREN, "Expect '(' after 'if'.")
        val condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after if condition.")

        val thenBranch = statement()
        val elseBranch = if (match(ELSE)) statement() else Stmt.Null

        return Stmt.If(condition, thenBranch, elseBranch)
    }

    private fun printStatement(): Stmt {
        val value = expression()
        consume(SEMICOLON, "Expect ';' after value.")
        return Stmt.Print(value)
    }

    private fun whileStatement(): Stmt {
        consume(LEFT_PAREN, "Expect '(' after 'while'.")
        val condition = expression()
        consume(RIGHT_PAREN, "Expect ')' after condition.")
        val body = statement()

        return Stmt.While(condition, body)
    }

    private fun block(): List<Stmt> {
        val statements = mutableListOf<Stmt>()

        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            statements.add(declaration())
        }

        consume(RIGHT_BRACE, "Expect '}' after block.")
        return statements
    }

    private fun expressionStatement(): Stmt {
        val expr = expression()
        consume(SEMICOLON, "Expect ';' after expression.")
        return Stmt.Expression(expr)
    }

    private fun expression(): Expr = assignment()

    private fun assignment(): Expr {
        val expr = ternary()

        if (match(EQUAL)) {
            val equals = previous()
            val value = assignment()

            if (expr is Expr.Variable) {
                val name = expr.name
                return Expr.Assign(name, value)
            }

            error(equals, "Invalid assignment target.")
        }

        return expr
    }

    private fun ternary(): Expr {
        val expr = or()

        if (match(QUESTION_MARK)) {
            val trueBranch = expression()
            consume(COLON, "Invalid conditional, expected ':'")
            val falseBranch = expression()
            return Expr.Ternary(expr, trueBranch, falseBranch)
        }

        return expr
    }

    //TODO: move to parse left associative
    private fun or(): Expr {
        var expr = and();

        while (match(OR)) {
            val operator = previous()
            val right = and();
            expr = Expr.Logical(expr, operator, right)
        }

        return expr
    }

    //TODO: move to parse left associative
    private fun and(): Expr {
        var expr = equality()

        while (match(AND)) {
            val operator = previous();
            val right = equality();
            expr = Expr.Logical(expr, operator, right)
        }

        return expr
    }

    private fun _todo_block(): Expr = parseLeftAssociative(::equality, COMMA)
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

    private fun primary(): Expr = when {
        match(FALSE) -> Expr.Literal(false)
        match(TRUE) -> Expr.Literal(true)
        match(NIL) -> Expr.Literal(NULL)
        match(NUMBER, STRING) -> Expr.Literal(previous().literal)
        match(IDENTIFIER) -> Expr.Variable(previous())
        match(LEFT_PAREN) -> {
            val expr = expression()
            consume(RIGHT_PAREN, "Expect ')' after expression.")
            Expr.Grouping(expr)
        }
        else -> throw error(peek(), "Expect expression.")
    }

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
