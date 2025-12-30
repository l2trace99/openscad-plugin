package org.openscad

import org.junit.Assert.*
import org.junit.Test
import org.openscad.references.OpenSCADGotoDeclarationHandler

/**
 * Unit tests for OpenSCAD plugin functionality
 * Tests pure logic without requiring IntelliJ platform initialization
 */
class OpenSCADPluginStartupTest {

    /**
     * Test that GotoDeclarationHandler can be instantiated
     */
    @Test
    fun testGotoDeclarationHandlerInstantiable() {
        val handler = OpenSCADGotoDeclarationHandler()
        assertNotNull("Handler should be instantiable", handler)
    }

    /**
     * Test identifier pattern matching for valid identifiers
     */
    @Test
    fun testValidIdentifiers() {
        val identPattern = Regex("^[a-zA-Z_][a-zA-Z0-9_]*$")
        
        assertTrue("my_module".matches(identPattern))
        assertTrue("MyModule".matches(identPattern))
        assertTrue("_private".matches(identPattern))
        assertTrue("module123".matches(identPattern))
        assertTrue("a".matches(identPattern))
    }

    /**
     * Test identifier pattern matching for invalid identifiers
     */
    @Test
    fun testInvalidIdentifiers() {
        val identPattern = Regex("^[a-zA-Z_][a-zA-Z0-9_]*$")
        
        assertFalse("123module".matches(identPattern))
        assertFalse("my-module".matches(identPattern))
        assertFalse("".matches(identPattern))
        assertFalse("my module".matches(identPattern))
    }
}
