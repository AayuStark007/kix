package dev.aayushgupta.kix.tool

import java.io.IOException
import java.io.PrintWriter
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    if (args.size != 1) {
        System.err.println("Usage: generate_ast <output_directory>")
        exitProcess(64)
    }
    val outputDir = args[0]
    defineAst(
        outputDir, "Expr", listOf(
            "Assign    -> val name: Token, val value: Expr",
            "Ternary   -> val condition: Expr, val expTrue: Expr, val expFalse: Expr",
            "Binary    -> val left: Expr, val operator: Token, val right: Expr",
            "Grouping  -> val expression: Expr",
            "Literal   -> val value: Any",
            "Unary     -> val operator: Token, val right: Expr",
            "Variable  -> val name: Token",
            "Null      -> none"
        )
    )

    defineAst(
        outputDir, "Stmt", listOf(
            "Block      -> val statements: List<Stmt>",
            "Expression -> val expression: Expr",
            "Print      -> val expression: Expr",
            "Var        -> val name: Token, val initializer: Expr",
            "Null       -> none"
        )
    )
}

@Throws(IOException::class)
private fun defineAst(outputDir: String, baseName: String, types: List<String>) {
    val path = "$outputDir/$baseName.kt"
    val writer = PrintWriter(path, "UTF-8")

    //println("${GenerateAst::class.java.`package`?.name}")
    writer.println("package dev.aayushgupta.kix.core")
    writer.println()
    writer.println("sealed class $baseName {")

    defineVisitor(writer, baseName, types)
    writer.println()

    // AST classes
    types.forEach { type ->
        val className = type.split("->")[0].trim()
        val fields = type.split("->")[1].trim()
        defineType(writer, baseName, className, fields)
        writer.println()
    }

    // The base accept() method
    writer.println("\tabstract fun <R> accept(visitor: Visitor<R>): R")

    writer.println("}")
    writer.close()
}

fun defineVisitor(writer: PrintWriter, baseName: String, types: List<String>) {
    writer.println("\tinterface Visitor<R> {")

    types.forEach { type ->
        val typeName = type.split("->")[0].trim()
        writer.println("\t\tfun visit$typeName$baseName(${baseName.lowercase()}: $typeName): R")
    }

    writer.println("\t}")
}

private fun defineType(
    writer: PrintWriter, baseName: String,
    className: String, fields: String
) {
    if (fields == "none") {
        writer.println("\tobject $className: $baseName() {")
    } else {
        writer.println("\tclass $className($fields): $baseName() {")
    }

    // Visitor pattern
    writer.println("\t\toverride fun <R> accept(visitor: Visitor<R>): R {")
    writer.println("\t\t\treturn visitor.visit$className$baseName(this)")
    writer.println("\t\t}")

    writer.println("\t}")
}
