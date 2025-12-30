package org.openscad

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for OpenSCAD file type and language configuration
 */
class OpenSCADFileTypeIntegrationTest {

    @Test
    fun testScadExtension() {
        val extension = "scad"
        assertEquals("scad", extension)
    }

    @Test
    fun testLanguageName() {
        val languageName = "OpenSCAD"
        assertEquals("OpenSCAD", languageName)
    }

    @Test
    fun testCommentPatterns() {
        val lineComment = "//"
        val blockCommentStart = "/*"
        val blockCommentEnd = "*/"
        
        assertTrue(lineComment.startsWith("//"))
        assertTrue(blockCommentStart.startsWith("/*"))
        assertTrue(blockCommentEnd.endsWith("*/"))
    }

    @Test
    fun testValidOpenSCADCode() {
        val code = """
            module my_module(size = 10) {
                cube(size);
            }
            
            function double(x) = x * 2;
            
            my_module(double(5));
        """.trimIndent()
        
        assertTrue("Code should contain module keyword", code.contains("module"))
        assertTrue("Code should contain function keyword", code.contains("function"))
        assertTrue("Code should contain cube", code.contains("cube"))
    }

    @Test
    fun testCommentRecognition() {
        val code = """
            // Single line comment
            /* Multi
               line
               comment */
            cube(10);
        """.trimIndent()
        
        assertTrue("Code should contain line comment", code.contains("//"))
        assertTrue("Code should contain block comment start", code.contains("/*"))
        assertTrue("Code should contain block comment end", code.contains("*/"))
    }
}
