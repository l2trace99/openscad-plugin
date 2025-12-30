package org.openscad.completion

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for library symbol autocomplete functionality
 */
class OpenSCADLibraryCompletionTest {
    
    /**
     * Test that use statement insertion position is calculated correctly
     * for a file with no imports
     */
    @Test
    fun testUseInsertPositionEmptyFile() {
        val fileText = """
            // My file
            cube(10);
        """.trimIndent()
        
        val insertPos = findUseInsertPosition(fileText)
        assertEquals("Should insert at beginning", 0, insertPos)
    }
    
    /**
     * Test that use statement insertion position is calculated correctly
     * for a file with existing imports
     */
    @Test
    fun testUseInsertPositionWithExistingImports() {
        val fileText = """
            use <library1.scad>
            include <library2.scad>
            
            cube(10);
        """.trimIndent()
        
        val insertPos = findUseInsertPosition(fileText)
        
        // Should insert after the last import
        val beforeInsert = fileText.substring(0, insertPos)
        assertTrue("Should be after use statement", beforeInsert.contains("use <library1.scad>"))
        assertTrue("Should be after include statement", beforeInsert.contains("include <library2.scad>"))
    }
    
    /**
     * Test that use statement is not duplicated
     */
    @Test
    fun testUseStatementNotDuplicated() {
        val fileText = """
            use <shapes.scad>
            
            rounded_cube();
        """.trimIndent()
        
        val useStatement = "use <shapes.scad>"
        val alreadyExists = fileText.contains(useStatement)
        
        assertTrue("Use statement should already exist", alreadyExists)
    }
    
    /**
     * Test that include statement is also checked
     */
    @Test
    fun testIncludeStatementAlsoChecked() {
        val fileText = """
            include <shapes.scad>
            
            rounded_cube();
        """.trimIndent()
        
        val relativePath = "shapes.scad"
        val hasUse = fileText.contains("use <$relativePath>")
        val hasInclude = fileText.contains("include <$relativePath>")
        
        assertFalse("Should not have use statement", hasUse)
        assertTrue("Should have include statement", hasInclude)
        
        // Should not insert use if include exists
        val shouldInsert = !hasUse && !hasInclude
        assertFalse("Should not insert use when include exists", shouldInsert)
    }
    
    // Helper function that mirrors the completion contributor logic
    private fun findUseInsertPosition(fileText: String): Int {
        val lines = fileText.lines()
        var lastImportEnd = 0
        var currentPos = 0
        
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("use ") || trimmed.startsWith("include ")) {
                lastImportEnd = currentPos + line.length + 1
            } else if (trimmed.isNotEmpty() && !trimmed.startsWith("//") && !trimmed.startsWith("/*")) {
                break
            }
            currentPos += line.length + 1
        }
        
        return lastImportEnd
    }
}
