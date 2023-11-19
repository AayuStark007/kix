package dev.aayushgupta.kix

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.file.Paths


class KixTest {

    private val stdOut: PrintStream = System.out
    private val stdErr: PrintStream = System.err
    private val outStreamCaptor: ByteArrayOutputStream = ByteArrayOutputStream()
    private val errStreamCaptor: ByteArrayOutputStream = ByteArrayOutputStream()

    @BeforeEach
    fun setup() {
        System.setOut(PrintStream(outStreamCaptor))
        System.setErr(PrintStream(errStreamCaptor))
    }

    @AfterEach
    fun tearDown() {
        System.setOut(stdOut)
        System.setErr(stdErr)
    }

    private fun fromResource(fileName: String): String {
        val resourceDirectory = Paths.get("src", "test", "resources")
        val absolutePath = resourceDirectory.toFile().absolutePath

        return "$absolutePath/$fileName"
    }

    @Test
    fun test_runHelloWorld() {
        assertOutput("hello, world", "hello.kix")
    }

    @Test
    fun test_runArithmetic() {
        assertOutput(
            """
            0.75
            9
            6
            -4
        """.trimIndent(), "arithmetic.kix"
        )
    }

    @Test
    fun test_runStrings() {
        assertOutput("hello world", "strings.kix")
    }

    @Test
    fun test_runVars() {
        assertOutput("1\nhello\n2", "vars.kix")
    }

    @Test
    fun test_runScopes() {
        assertOutput(
            """
            inner a
            outer b
            global c
            outer a
            outer b
            global c
            global a
            global b
            global c
        """.trimIndent(), "scopes.kix"
        )
    }

    @Test
    fun test_runTernary() {
        assertOutput(
            """
            great
            100 great
            got true
            a gt 50
            gt
            gt
        """.trimIndent(), "ternary.kix"
        )
    }

    @Test
    fun test_runConditional() {
        assertOutput(
            """
            lt
            a1 is greater than 50
            a1 is less than 50
            a1 is 50
        """.trimIndent(), "conditional.kix"
        )
    }

    @Test
    fun test_runBinaryOp() {
        assertOutput(
            """
            hi
            yes
            true
        """.trimIndent(), "binaryop.kix"
        )
    }

    @Test
    fun test_runLoops() {
        assertOutput(
            """
            inside loop 4
            inside loop 3
            inside loop 2
            inside loop 1
            inside loop 0
            outside loop
            0
            1
            1
            2
            3
            5
            8
            13
        """.trimIndent(), "loops.kix"
        )
    }

    @Test
    fun test_runFunction() {
        assertOutput(
            """
            Hi, Dear Reader!
            Hi, Mr Banana!
        """.trimIndent(), "function.kix"
        )
    }

    @Test
    fun test_runRecurse() {
        assertOutput(
            """
            10
            9
            8
            7
            6
            5
            4
            3
            2
            1
            0
            0.0
            1.0
            1.0
            2.0
            3.0
            5.0
            8.0
            13.0
            21.0
            34.0
            55.0
            89.0
            144.0
            233.0
            377.0
            610.0
            987.0
            1597.0
            2584.0
            4181.0
        """.trimIndent(), "recurse.kix"
        )
    }

//    @Test
//    fun test_runShadow() {
//        assertOutput("local", "shadow.kix")
//    }

    @Test
    fun test_runClosure() {
        assertOutput(
            """
            1
            2
            global
            global
            block
        """.trimIndent(), "closure.kix"
        )
    }

//    @Test
//    fun test_runUndefinedVar() {
//        assertErr("""
//
//        """.trimIndent(), "undefined_var.kix")
//    }

    private fun assertOutput(expected: String, fileName: String) {
        runFile(fromResource(fileName))
        assertEquals(expected, outStreamCaptor.toString().trim().replace("\r\n", "\n"))
    }

    private fun assertErr(expected: String, fileName: String) {
        runFile(fromResource(fileName))
        assertEquals(expected, errStreamCaptor.toString().trim().replace("\r\n", "\n"))
    }
}