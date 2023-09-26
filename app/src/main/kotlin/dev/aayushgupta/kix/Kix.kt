package dev.aayushgupta.kix

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import dev.aayushgupta.kix.core.*
import dev.aayushgupta.kix.util.AstPrinter
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.system.exitProcess

private val interpreter = Interpreter()

var hadError: Boolean = false
var hadRuntimeError: Boolean = false
var shouldPrintAst: Boolean = false

fun main(args: Array<String>) {
    ArgParser(args).parseInto(::KixArgs).run {
        shouldPrintAst = this.printAst
        if (this.sourceFile.isBlank()) runPrompt() else runFile(this.sourceFile)
    }
}

fun runPrompt() {
    val input = InputStreamReader(System.`in`)
    val reader = BufferedReader(input)

    while (true) {
        print("> ")
        val line = reader.readLine() ?: break
        run(line)
        hadError = false
    }
}

fun runFile(path: String) {
    val bytes = Files.readAllBytes(Paths.get(path))
    run(String(bytes, Charset.defaultCharset()))

    // Indicate error in exit code
    if (hadError) exitProcess(65)
    if (hadRuntimeError) exitProcess(70)
}

fun run(source: String) {
    val scanner = Scanner(source)
    //val scanStart = System.nanoTime()
    val tokens = scanner.scanTokens()
    //val scanDurationNs = System.nanoTime() - scanStart
    //println("Took ${scanDurationNs}ns | ${scanDurationNs / 1e6}ms to scan")

    val parser = Parser(tokens)
    //val now = System.nanoTime()
    val statements = parser.parse()
    //val parseTimeNs = System.nanoTime() - now
    //println("Took ${parseTimeNs}ns | ${parseTimeNs / 1e6}ms to parse")

    if (hadError) return

    if (shouldPrintAst) {
        AstPrinter().print(statements)
    }
    interpreter.interpret(statements)
}

fun error(line: Int, message: String) {
    report(line, "", message)
}

fun report(line: Int, where: String, message: String) {
    System.err.println("[line $line] Error${where}: $message")
    hadError = true
}

fun error(token: Token, message: String) {
    if (token.type == TokenType.EOF) {
        report(token.line, " at end", message)
    } else {
        report(token.line, " at '${token.lexeme}'", message)
    }
}

fun runtimeError(error: RuntimeError) {
    System.err.println("${error.message}\n[line ${error.token.line}]")
    hadRuntimeError = true
}


class KixArgs(parser: ArgParser) {
    val printAst by parser.flagging(
        "--print-ast",
        help = "print AST for parsed code"
    )

    val sourceFile by parser.positional("source filename, omit for REPL mode")
        .default("")
}