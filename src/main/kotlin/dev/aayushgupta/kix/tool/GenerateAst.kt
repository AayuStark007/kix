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
    defineAst(outputDir, "Expr", listOf(
        "Binary    -> left: Expr, operator: Token, right: Expr",
        "Grouping  -> expression: Expr",
        "Literal   -> value: Any",
        "Unary     -> operator: Token, right: Expr"
    ))
}

@Throws(IOException::class)
private fun defineAst(outputDir: String, baseName: String, types: List<String>) {
    val path = "$outputDir/$baseName.kt"
    val writer = PrintWriter(path, "UTF-8")

    //println("${GenerateAst::class.java.`package`?.name}")
    writer.println("package dev.aayushgupta.kix.core")
    writer.println()
    writer.println("sealed class $baseName {")

    // AST classes
    types.forEach { type ->
        val className = type.split("->")[0].trim()
        val fields = type.split("->")[1].trim()
        defineType(writer, baseName, className, fields)
    }

    writer.println("}")
    writer.close()
}

private fun defineType(
    writer: PrintWriter, baseName: String,
    className: String, fields: String) {
    writer.println("\tclass $className($fields): $baseName()")
}
