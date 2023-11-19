package dev.aayushgupta.kix.core

import java.util.*

/**
 * Resolver keeps track of the distance between the current scope and scope where the variable was defined (when current scope was created)
 * This helps us overcome bugs where a closure refers to a variable that was redefined in a different scope potentially causing
 * the closure to refer to the redefined variable name. (eg: test in closure.kix demonstrates the issue)
 * Maintaining this distance measure helps the interpreter to resolve the variable using the initially defined scope.
 */
class Resolver(private val interpreter: Interpreter) :
    Expr.Visitor<Unit>, Stmt.Visitor<Unit> {

    private val scopes: Stack<MutableMap<String, Info>> = Stack()
    private var currentFunction: FunctionType = FunctionType.NONE

    private class Info(
        var defined: Boolean = false,
        var used: Boolean = false,
        var token: Token,
        var functionType: FunctionType = FunctionType.NONE
    )

    private enum class FunctionType {
        NONE, FUNCTION
    }

    override fun visitAssignExpr(expr: Expr.Assign) {
        resolve(expr.value)
        resolveLocal(expr, expr.name, isRead = false)
    }

    override fun visitTernaryExpr(expr: Expr.Ternary) {
        resolve(expr.condition)
        resolve(expr.expTrue)
        resolve(expr.expFalse)
    }

    override fun visitBinaryExpr(expr: Expr.Binary) {
        resolve(expr.left)
        resolve(expr.right)
    }

    override fun visitCallExpr(expr: Expr.Call) {
        resolve(expr.callee)
        expr.args.forEach(::resolve)
    }

    override fun visitGroupingExpr(expr: Expr.Grouping) {
        resolve(expr.expression)
    }

    override fun visitLiteralExpr(expr: Expr.Literal) {
    }

    override fun visitLogicalExpr(expr: Expr.Logical) {
        resolve(expr.left)
        resolve(expr.right)
    }

    override fun visitUnaryExpr(expr: Expr.Unary) {
        resolve(expr.right)
    }

    override fun visitVariableExpr(expr: Expr.Variable) {
        if ((scopes.isNotEmpty()
                    && scopes.peek().containsKey(expr.name.lexeme)) && !scopes.peek()[expr.name.lexeme]?.defined!!
        ) {
            dev.aayushgupta.kix.error(expr.name, "Can't read local variable in its own initializer")
        }
        resolveLocal(expr, expr.name, isRead = true)
    }

    override fun visitNullExpr(expr: Expr.Null) {
    }

    override fun visitBlockStmt(stmt: Stmt.Block) {
        beginScope()
        resolve(stmt.statements)
        endScope()
    }

    override fun visitExpressionStmt(stmt: Stmt.Expression) {
        resolve(stmt.expression)
    }

    override fun visitFunctionStmt(stmt: Stmt.Function) {
        declare(stmt.name, FunctionType.FUNCTION)
        define(stmt.name, FunctionType.FUNCTION)

        resolveFunction(stmt, FunctionType.FUNCTION)
    }

    override fun visitIfStmt(stmt: Stmt.If) {
        resolve(stmt.condition)
        resolve(stmt.thenBranch)
        if (stmt.elseBranch != Stmt.Null) resolve(stmt.elseBranch)
    }

    override fun visitPrintStmt(stmt: Stmt.Print) {
        resolve(stmt.expression)
    }

    override fun visitReturnStmt(stmt: Stmt.Return) {
        if (currentFunction == FunctionType.NONE) {
            dev.aayushgupta.kix.error(stmt.keyword, "Can't return from outside function.")
        }
        if (stmt.value != Expr.Null) {
            resolve(stmt.value)
        }
    }

    override fun visitVarStmt(stmt: Stmt.Var) {
        declare(stmt.name)
        if (stmt.initializer != Expr.Null) {
            resolve(stmt.initializer)
        }
        define(stmt.name)
    }

    override fun visitWhileStmt(stmt: Stmt.While) {
        resolve(stmt.condition)
        resolve(stmt.body)
    }

    override fun visitNullStmt(stmt: Stmt.Null) {
    }

    fun resolve(statements: List<Stmt>) {
        statements.forEach(::resolve)
    }

    // TODO: Stmt and Expr can implement some interface like Traversable then only need to call accept once on traversables
    private fun resolve(statement: Stmt) {
        statement.accept(this)
    }

    private fun resolve(expr: Expr) {
        expr.accept(this)
    }

    private fun beginScope() {
        scopes.push(mutableMapOf())
    }

    private fun endScope() {
        checkForUnusedVariables()
        scopes.pop()
    }

    // check for unused variables in current scope
    private fun checkForUnusedVariables() {
        val popped = scopes.peek()
        popped.forEach { entry ->
            if (entry.value.functionType == FunctionType.NONE && !entry.value.used) {
                dev.aayushgupta.kix.error(entry.value.token, "Variable defined but not used.")
            }
        }
    }

    private fun declare(name: Token, functionType: FunctionType = FunctionType.NONE) {
        if (scopes.empty()) return
        val scope = scopes.peek()
        if (scope.containsKey(name.lexeme)) {
            dev.aayushgupta.kix.error(name, "Variable with this name already declared in this scope.")
        }
        scope[name.lexeme] = Info(defined = false, used = false, token = name, functionType = functionType)
    }

    private fun define(name: Token, functionType: FunctionType = FunctionType.NONE) {
        if (scopes.empty()) return
        scopes.peek()[name.lexeme] = Info(defined = true, used = false, token = name, functionType = functionType)
    }

    private fun resolveLocal(expr: Expr, name: Token, isRead: Boolean) {
        for (i in scopes.size - 1 downTo 0) {
            if (scopes[i].containsKey(name.lexeme)) {
                interpreter.resolve(expr, scopes.size - 1 - i)
                if (isRead) scopes[i][name.lexeme]?.used = true
                return
            }
        }
    }

    private fun resolveFunction(function: Stmt.Function, type: FunctionType) {
        val enclosingFunction = currentFunction
        currentFunction = type

        beginScope()
        function.params.forEach {
            declare(it)
            define(it)
        }
        resolve(function.body)
        endScope()

        currentFunction = enclosingFunction
    }


}