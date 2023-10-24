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
            if (match(FUN)) return function("function")
            if (match(VAR)) return varDeclaration()
            return statement()
        } catch (error: ParseError) {
            synchronize()
            return Stmt.Null
        }
    }

    private fun function(kind: String): Stmt.Function {
        val name = consume(IDENTIFIER, "Expect $kind name.")
        consume(LEFT_PAREN, "Expect '(' after $kind name.")
        val params = mutableListOf<Token>()
        if (!check(RIGHT_PAREN)) {
            do {
                if (params.size >= 255) {
                    error(peek(), "Can't have more than 255 parameters.")
                }
                params.add(consume(IDENTIFIER, "Expect parameter name."))
            } while (match(COMMA))
        }
        consume(RIGHT_PAREN, "Expect ')' after parameters.")
        consume(LEFT_BRACE, "Expect '{' before $kind body.")
        return Stmt.Function(name, params, body = block())
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
        match(FOR) -> forStatement()
        match(IF) -> ifStatement()
        match(PRINT) -> printStatement()
        match(WHILE) -> whileStatement()
        match(LEFT_BRACE) -> Stmt.Block(block())
        else -> expressionStatement()
    }

    private fun forStatement(): Stmt {
        consume(LEFT_PAREN, "Expect '(' after 'for'.")

        val initializer = when {
            match(SEMICOLON) -> Stmt.Null
            match(VAR) -> varDeclaration()
            else -> expressionStatement()
        }

        val condition: Expr = if (!check(SEMICOLON)) {
            expression()
        } else {
            Expr.Literal(true) // if condition is not given then we evaluate as true
        }
        consume(SEMICOLON, "Expect ';' after loop condition.")

        val increment: Expr = if (!check(RIGHT_PAREN)) {
            expression()
        } else {
            Expr.Null
        }
        consume(RIGHT_PAREN, "Expect ')' after for clauses.")

        var body = statement()

        // de-sugar the 'for' construct into 'while'

        // insert the increment op after the body
        body = Stmt.Block(listOf(body, Stmt.Expression(increment)))

        // setup while loop using the condition and the body
        body = Stmt.While(condition, body)

        // finally, place the init expr before the while loop
        body = Stmt.Block(listOf(initializer, body))

        return body
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
        return call()
    }

    private fun call(): Expr {
        var expr = primary()

        while (true) {
            if (match(LEFT_PAREN)) {
                expr = finishCall(expr)
            } else {
                break
            }
        }
        return expr
    }

    private fun finishCall(callee: Expr): Expr {
        val args = mutableListOf<Expr>()
        if (!check(RIGHT_PAREN)) {
            do {
                if (args.size >= 255) {
                    error(peek(), "Can't have more than 255 arguments.")
                }
                args.add(expression())
            } while (match(COMMA))
        }
        val paren = consume(RIGHT_PAREN, "Expect ')' after arguments.")
        return Expr.Call(callee, paren, args)
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
