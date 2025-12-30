package org.openscad.references

import org.junit.Assert.*
import org.junit.Test
import java.io.File

/**
 * Unit tests for OpenSCAD library indexer
 */
class OpenSCADLibraryIndexerTest {
    
    /**
     * Test relative path calculation
     */
    @Test
    fun testRelativePathCalculation() {
        val libraryRoot = "/path/to/libraries"
        val filePath = "/path/to/libraries/mylib/shapes.scad"
        
        val relativePath = filePath
            .removePrefix(libraryRoot)
            .removePrefix(File.separator)
            .replace(File.separatorChar, '/')
        
        assertEquals("mylib/shapes.scad", relativePath)
    }
    
    /**
     * Test library file exists in test resources
     */
    @Test
    fun testLibraryFileExists() {
        val testLibFile = File("src/test/resources/testLibrary/shapes.scad")
        assertTrue("Test library file should exist", testLibFile.exists())
    }
    
    /**
     * Test library file contains expected content
     */
    @Test
    fun testLibraryFileContent() {
        val testLibFile = File("src/test/resources/testLibrary/shapes.scad")
        val content = testLibFile.readText()
        
        assertTrue("Should contain rounded_cube module", content.contains("module rounded_cube"))
        assertTrue("Should contain hollow_cylinder module", content.contains("module hollow_cylinder"))
        assertTrue("Should contain cube_volume function", content.contains("function cube_volume"))
        assertTrue("Should contain _internal_helper (private)", content.contains("module _internal_helper"))
    }
}
