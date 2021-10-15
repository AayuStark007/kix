package dev.aayushgupta.kix.core

sealed class Expr {
	interface Visitor<R> {
		fun visitBinaryExpr(expr: Binary): R
		fun visitGroupingExpr(expr: Grouping): R
		fun visitLiteralExpr(expr: Literal): R
		fun visitUnaryExpr(expr: Unary): R
	}

	class Binary(left: Expr, operator: Token, right: Expr): Expr() {
		override fun <R> accept(visitor: Visitor<R>): R {
			return visitor.visitBinaryExpr(this)
		}
	}

	class Grouping(expression: Expr): Expr() {
		override fun <R> accept(visitor: Visitor<R>): R {
			return visitor.visitGroupingExpr(this)
		}
	}

	class Literal(value: Any): Expr() {
		override fun <R> accept(visitor: Visitor<R>): R {
			return visitor.visitLiteralExpr(this)
		}
	}

	class Unary(operator: Token, right: Expr): Expr() {
		override fun <R> accept(visitor: Visitor<R>): R {
			return visitor.visitUnaryExpr(this)
		}
	}

	abstract fun <R> accept(visitor: Visitor<R>): R
}
