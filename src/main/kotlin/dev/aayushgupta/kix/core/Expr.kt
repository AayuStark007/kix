package dev.aayushgupta.kix.core

sealed class Expr {
	interface Visitor<R> {
		fun visitTernaryExpr(expr: Ternary): R
		fun visitBinaryExpr(expr: Binary): R
		fun visitGroupingExpr(expr: Grouping): R
		fun visitLiteralExpr(expr: Literal): R
		fun visitUnaryExpr(expr: Unary): R
	}

	class Ternary(val condition: Expr, val expTrue: Expr, val expFalse: Expr): Expr() {
		override fun <R> accept(visitor: Visitor<R>): R {
			return visitor.visitTernaryExpr(this)
		}
	}

	class Binary(val left: Expr, val operator: Token, val right: Expr): Expr() {
		override fun <R> accept(visitor: Visitor<R>): R {
			return visitor.visitBinaryExpr(this)
		}
	}

	class Grouping(val expression: Expr): Expr() {
		override fun <R> accept(visitor: Visitor<R>): R {
			return visitor.visitGroupingExpr(this)
		}
	}

	class Literal(val value: Any): Expr() {
		override fun <R> accept(visitor: Visitor<R>): R {
			return visitor.visitLiteralExpr(this)
		}
	}

	class Unary(val operator: Token, val right: Expr): Expr() {
		override fun <R> accept(visitor: Visitor<R>): R {
			return visitor.visitUnaryExpr(this)
		}
	}

	abstract fun <R> accept(visitor: Visitor<R>): R
}
