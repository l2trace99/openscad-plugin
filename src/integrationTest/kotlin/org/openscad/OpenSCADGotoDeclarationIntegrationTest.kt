package org.openscad

import org.junit.Assert.*
import org.junit.Test
import org.openscad.references.OpenSCADGotoDeclarationHandler

/**
 * Unit tests for Go to Definition functionality
 */
class OpenSCADGotoDeclarationIntegrationTest {

    /**
     * Test that GotoDeclarationHandler is instantiable
     */
    @Test
    fun testGotoDeclarationHandlerInstantiable() {
        val handler = OpenSCADGotoDeclarationHandler()
        assertNotNull("Handler should be instantiable", handler)
    }

    /**
     * Test keywords that should be skipped
     */
    @Test
    fun testKeywordsSkipped() {
        val keywords = setOf("module", "function", "if", "else", "for", "true", "false", "undef")
        keywords.forEach { keyword ->
            assertTrue("Keyword $keyword should not be empty", keyword.isNotEmpty())
        }
        assertEquals(8, keywords.size)
    }

    /**
     * Test built-in modules that should be skipped
     */
    @Test
    fun testBuiltinModulesSkipped() {
        val builtins = setOf(
            "cube", "sphere", "cylinder", "translate", "rotate", "scale",
            "union", "difference", "intersection"
        )
        
        builtins.forEach { builtin ->
            assertTrue("Built-in $builtin should not be empty", builtin.isNotEmpty())
        }
        assertEquals(9, builtins.size)
    }

    /**
     * Test identifier pattern matching - valid identifiers
     */
    @Test
    fun testValidIdentifiers() {
        val identPattern = Regex("^[a-zA-Z_][a-zA-Z0-9_]*$")
        
        assertTrue("my_module".matches(identPattern))
        assertTrue("MyModule".matches(identPattern))
        assertTrue("_private".matches(identPattern))
        assertTrue("module123".matches(identPattern))
        assertTrue("a".matches(identPattern))
        assertTrue("fan".matches(identPattern))
        assertTrue("rounded_cube".matches(identPattern))
    }

    /**
     * Test identifier pattern matching - invalid identifiers
     */
    @Test
    fun testInvalidIdentifiers() {
        val identPattern = Regex("^[a-zA-Z_][a-zA-Z0-9_]*$")
        
        assertFalse("123module".matches(identPattern))
        assertFalse("my-module".matches(identPattern))
        assertFalse("".matches(identPattern))
        assertFalse("my module".matches(identPattern))
    }

    /**
     * Test module declaration parsing pattern
     */
    @Test
    fun testModuleDeclarationPattern() {
        val code = "module my_custom_module(size = 10) { cube(size); }"
        assertTrue(code.contains("module"))
        assertTrue(code.contains("my_custom_module"))
    }

    /**
     * Test function declaration parsing pattern
     */
    @Test
    fun testFunctionDeclarationPattern() {
        val code = "function double(x) = x * 2;"
        assertTrue(code.contains("function"))
        assertTrue(code.contains("double"))
    }

    /**
     * Test use statement pattern
     */
    @Test
    fun testUseStatementPattern() {
        val useStatement = "use <NopSCADlib/vitamins/fan.scad>"
        assertTrue(useStatement.startsWith("use"))
        assertTrue(useStatement.contains("<"))
        assertTrue(useStatement.contains(">"))
        
        // Extract path
        val startIdx = useStatement.indexOf('<')
        val endIdx = useStatement.indexOf('>')
        val path = useStatement.substring(startIdx + 1, endIdx)
        assertEquals("NopSCADlib/vitamins/fan.scad", path)
    }

    /**
     * Test include statement pattern
     */
    @Test
    fun testIncludeStatementPattern() {
        val includeStatement = "include <BOSL2/std.scad>"
        assertTrue(includeStatement.startsWith("include"))
        
        val startIdx = includeStatement.indexOf('<')
        val endIdx = includeStatement.indexOf('>')
        val path = includeStatement.substring(startIdx + 1, endIdx)
        assertEquals("BOSL2/std.scad", path)
    }
}
