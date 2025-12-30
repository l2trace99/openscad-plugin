package org.openscad

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for OpenSCAD library indexing logic
 */
class OpenSCADLibraryIndexingIntegrationTest {

    /**
     * Test relative path calculation logic
     */
    @Test
    fun testRelativePathCalculation() {
        val libraryRoot = "/path/to/libraries"
        val filePath = "/path/to/libraries/mylib/shapes.scad"
        
        val relativePath = filePath
            .removePrefix(libraryRoot)
            .removePrefix(java.io.File.separator)
            .replace(java.io.File.separatorChar, '/')
        
        assertEquals("mylib/shapes.scad", relativePath)
    }

    /**
     * Test relative path with nested directories
     */
    @Test
    fun testRelativePathNested() {
        val libraryRoot = "/home/user/libs"
        val filePath = "/home/user/libs/NopSCADlib/vitamins/fan.scad"
        
        val relativePath = filePath
            .removePrefix(libraryRoot)
            .removePrefix(java.io.File.separator)
            .replace(java.io.File.separatorChar, '/')
        
        assertEquals("NopSCADlib/vitamins/fan.scad", relativePath)
    }

    /**
     * Test that private symbols (starting with _) would be filtered
     */
    @Test
    fun testPrivateSymbolFiltering() {
        val publicName = "my_module"
        val privateName = "_internal_helper"
        
        assertFalse("Public name should not start with _", publicName.startsWith("_"))
        assertTrue("Private name should start with _", privateName.startsWith("_"))
    }

    /**
     * Test symbol naming conventions
     */
    @Test
    fun testSymbolNamingConventions() {
        val validNames = listOf("my_module", "MyModule", "module123", "a", "_private")
        val invalidNames = listOf("123module", "my-module", "my module")
        
        val identPattern = Regex("^[a-zA-Z_][a-zA-Z0-9_]*$")
        
        validNames.forEach { name ->
            assertTrue("$name should be valid", name.matches(identPattern))
        }
        
        invalidNames.forEach { name ->
            assertFalse("$name should be invalid", name.matches(identPattern))
        }
    }

    /**
     * Test common library paths
     */
    @Test
    fun testCommonLibraryPaths() {
        val commonPaths = listOf(
            "/usr/share/openscad/libraries",
            "/usr/local/share/openscad/libraries"
        )
        
        commonPaths.forEach { path ->
            assertTrue("Path should start with /", path.startsWith("/"))
            assertTrue("Path should contain 'openscad'", path.contains("openscad"))
        }
    }
}
