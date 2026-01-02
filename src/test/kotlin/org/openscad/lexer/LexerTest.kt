package org.openscad.lexer

import com.intellij.psi.TokenType
import org.junit.Test
import org.openscad.parser.OpenSCADParserImpl
import java.io.File

class LexerTest {
    
    /**
     * Find project root by looking for build.gradle.kts
     */
    private fun findProjectRoot(): File? {
        var dir = File(System.getProperty("user.dir"))
        while (dir.parentFile != null) {
            if (File(dir, "build.gradle.kts").exists()) {
                return dir
            }
            dir = dir.parentFile
        }
        return null
    }
    
    /**
     * Find a test file relative to project root
     */
    private fun findTestFile(filename: String): File? {
        val projectRoot = findProjectRoot()
        if (projectRoot != null) {
            val file = File(projectRoot, filename)
            if (file.exists()) return file
        }
        // Fallback to current directory
        val local = File(filename)
        if (local.exists()) return local
        return null
    }
    
    @Test
    fun testLexTestUseScad() {
        val testFile = findTestFile("test-use.scad")
        if (testFile == null) {
            println("test-use.scad not found, skipping")
            return
        }
        
        val code = testFile.readText()
        val lexer = OpenSCADLexerImpl()
        lexer.reset(code, 0, code.length, 0)
        
        var lineNum = 1
        var colNum = 1
        val badChars = mutableListOf<String>()
        
        var tokenType = lexer.advance()
        while (tokenType != null) {
            val tokenText = lexer.yytext().toString()
            
            if (tokenType == TokenType.BAD_CHARACTER) {
                badChars.add("Line $lineNum, col $colNum: '$tokenText' (code: ${tokenText.firstOrNull()?.code})")
            }
            
            for (c in tokenText) {
                if (c == '\n') {
                    lineNum++
                    colNum = 1
                } else {
                    colNum++
                }
            }
            
            tokenType = lexer.advance()
        }
        
        if (badChars.isNotEmpty()) {
            println("BAD_CHARACTER tokens found:")
            badChars.forEach { println("  $it") }
            throw AssertionError("Found ${badChars.size} BAD_CHARACTER tokens")
        }
        
        println("✓ No lexer errors found")
    }
    
    @Test
    fun testLexTokensTestUseScad() {
        val testFile = findTestFile("test-use.scad")
        if (testFile == null) {
            println("test-use.scad not found")
            println("Current dir: ${System.getProperty("user.dir")}")
            println("Project root: ${findProjectRoot()?.absolutePath ?: "not found"}")
            return
        }
        println("Found test file at: ${testFile.absolutePath}")
        
        val code = testFile.readText()
        val lexer = OpenSCADLexerImpl()
        lexer.reset(code, 0, code.length, 0)
        
        println("=== Token Stream ===")
        var lineNum = 1
        var tokenCount = 0
        
        var tokenType = lexer.advance()
        while (tokenType != null) {
            val tokenText = lexer.yytext().toString()
            tokenCount++
            
            // Print tokens (skip whitespace for brevity)
            if (tokenType != TokenType.WHITE_SPACE) {
                val displayText = tokenText.replace("\n", "\\n").take(20)
                println("L$lineNum: $tokenType = '$displayText'")
            }
            
            for (c in tokenText) {
                if (c == '\n') lineNum++
            }
            
            tokenType = lexer.advance()
        }
        
        println("=== Total: $tokenCount tokens ===")
    }
    
    @Test
    fun testParseTestUseScad() {
        val testFile = findTestFile("test-use.scad")
        if (testFile == null) {
            println("test-use.scad not found, skipping")
            return
        }
        
        val code = testFile.readText()
        val lines = code.lines()
        
        // Use lightweight parser validation
        val errors = LightweightParserValidator.validate(code)
        
        if (errors.isEmpty()) {
            println("✓ No parser errors found in test-use.scad")
        } else {
            println("Parser errors found (${errors.size} total):")
            errors.take(20).forEach { error ->
                val lineNum = getLineNumber(code, error.offset)
                val lineContent = if (lineNum > 0 && lineNum <= lines.size) lines[lineNum - 1].trim().take(60) else ""
                println("  Line $lineNum: ${error.message} | $lineContent")
            }
            if (errors.size > 20) {
                println("  ... and ${errors.size - 20} more errors")
            }
        }
    }
    
    @Test
    fun testParseTestUseScadWithRealParser() {
        val testFile = findTestFile("test-use.scad")
        if (testFile == null) {
            println("test-use.scad not found, skipping")
            return
        }
        
        val code = testFile.readText()
        val lines = code.lines()
        
        // Use actual OpenSCAD parser
        val errors = org.openscad.parser.OpenSCADParserImpl.parseAndGetErrors(code)
        
        if (errors.isEmpty()) {
            println("✓ No parser errors found in test-use.scad (real parser)")
        } else {
            println("Real parser errors found (${errors.size} total):")
            errors.take(20).forEach { error ->
                val lineNum = getLineNumber(code, error.offset)
                val lineContent = if (lineNum > 0 && lineNum <= lines.size) lines[lineNum - 1].trim().take(60) else ""
                println("  Line $lineNum: ${error.message} | $lineContent")
            }
            if (errors.size > 20) {
                println("  ... and ${errors.size - 20} more errors")
            }
        }
    }
    
    private fun getLineNumber(code: String, offset: Int): Int {
        var line = 1
        for (i in 0 until minOf(offset, code.length)) {
            if (code[i] == '\n') line++
        }
        return line
    }
}
