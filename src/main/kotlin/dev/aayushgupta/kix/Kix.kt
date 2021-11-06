package dev.aayushgupta.kix

import dev.aayushgupta.kix.core.*
import dev.aayushgupta.kix.core.KixScanner
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

fun main(args: Array<String>) {
    if (args.size > 1) {
        println("Usage: kix [script]")
        exitProcess(64)
    } else if (args.size == 1) {
        runFile(args[0])
    } else {
        runPrompt()
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
    val scanner = KixScanner(source)
    //val scanStart = System.nanoTime()
    val tokens = scanner.scanTokens()
    //val scanDurationNs = System.nanoTime() - scanStart
    //println("Took ${scanDurationNs}ns | ${scanDurationNs / 1e6}ms to scan")
    
    val parser = Parser(tokens)
    //val now = System.nanoTime()
    val expression = parser.parse()
    //val parseTimeNs = System.nanoTime() - now
    //println("Took ${parseTimeNs}ns | ${parseTimeNs / 1e6}ms to parse")

    if (hadError) return

    //println(AstPrinter().print(expression))
    interpreter.interpret(expression)
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
