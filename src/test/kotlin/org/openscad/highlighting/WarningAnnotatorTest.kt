package org.openscad.highlighting

import org.junit.Test
import org.junit.Assert.*
import java.io.File

/**
 * Tests for OpenSCAD warning detection.
 * Based on OpenSCAD source code warning patterns:
 * - Variable reassignment warnings
 */
class WarningAnnotatorTest {

    // ========== Variable Reassignment Warnings ==========
    
    @Test
    fun testVariableReassignmentDetected() {
        val code = """
            x = 1;
            y = 2;
            x = 3;
        """.trimIndent()
        val warnings = OpenSCADWarnings.findWarnings(code)
        assertEquals("Should detect reassignment of x", 1, warnings.size)
        assertTrue("Should mention variable name", warnings[0].message.contains("x"))
        assertTrue("Should mention overwritten", warnings[0].message.lowercase().contains("overwritten"))
    }
    
    @Test
    fun testNoWarningForSingleAssignment() {
        val code = """
            x = 1;
            y = 2;
            z = 3;
        """.trimIndent()
        val warnings = OpenSCADWarnings.findWarnings(code)
        assertEquals("No warnings for unique assignments", 0, warnings.size)
    }
    
    @Test
    fun testMultipleReassignmentsDetected() {
        val code = """
            fn = 0;
            fs = 0.1;
            fn = 36;
            fs = 0.75;
        """.trimIndent()
        val warnings = OpenSCADWarnings.findWarnings(code)
        assertEquals("Should detect both reassignments", 2, warnings.size)
    }
    
    @Test
    fun testReassignmentShowsOriginalLine() {
        val code = """
            myvar = 10;
            other = 20;
            myvar = 30;
        """.trimIndent()
        val warnings = OpenSCADWarnings.findWarnings(code)
        assertEquals(1, warnings.size)
        assertTrue("Should reference original line", warnings[0].message.contains("line 1"))
    }
    
    @Test
    fun testLocalScopeVariablesNotWarned() {
        val code = """
            x = 1;
            module foo() {
                x = 2;
            }
        """.trimIndent()
        val warnings = OpenSCADWarnings.findWarnings(code)
        // Variables in different scopes should not warn
        assertEquals("Different scopes should not warn", 0, warnings.size)
    }
    
    @Test
    fun testMultipleModulesWithSameParameterName() {
        // This is a common pattern - multiple modules with same param names
        val code = """
            module grid_block(
              num_x=1,
              num_y=2) {
                echo(num_x);
            }
            
            module pad_oversize(
              num_x=1,
              num_y=1) {
                echo(num_x);
            }
        """.trimIndent()
        val warnings = OpenSCADWarnings.findWarnings(code)
        assertEquals("Module parameters in different modules should not warn", 0, warnings.size)
    }
    
    @Test
    fun testMultilineModuleParameters() {
        // Parameters can span multiple lines
        val code = """
            module foo(
              param1 = 1,
              param2 = 2,
              param3 = 3) {
                echo(param1);
            }
        """.trimIndent()
        val warnings = OpenSCADWarnings.findWarnings(code)
        assertEquals("Multiline parameters should not warn", 0, warnings.size)
    }
    
    @Test
    fun testFunctionParameters() {
        val code = """
            function calc(x=1, y=2) = x + y;
            function other(x=1, y=2) = x * y;
        """.trimIndent()
        val warnings = OpenSCADWarnings.findWarnings(code)
        assertEquals("Function parameters should not warn", 0, warnings.size)
    }
    
    @Test
    fun testModuleCallNamedParameters() {
        // Named parameters in module CALLS should not trigger warnings
        // This is different from variable assignment - it's passing arguments
        val code = """
            size = [10, 20, 30];
            CubeWithRoundedCorner(
              size=[size.z, size.y, size.x],
              cornerRadius = 5);
            CubeWithRoundedCorner(
              size=[size.z, size.y, size.x],
              cornerRadius = 5);
        """.trimIndent()
        val warnings = OpenSCADWarnings.findWarnings(code)
        assertEquals("Module call parameters should not warn", 0, warnings.size)
    }
    
    @Test
    fun testNestedModuleCallParameters() {
        // Parameters inside nested module calls
        val code = """
            x = 1;
            translate([0, 0, 0])
            rotate([0, 90, 0])
            cube(size=[x, x, x], center=true);
            rotate([0, 90, 270])
            cube(size=[x, x, x], center=true);
        """.trimIndent()
        val warnings = OpenSCADWarnings.findWarnings(code)
        assertEquals("Nested module call params should not warn", 0, warnings.size)
    }
    
    @Test
    fun testWarningContainsOffsets() {
        val code = "x = 1;\nx = 2;"
        val warnings = OpenSCADWarnings.findWarnings(code)
        assertEquals(1, warnings.size)
        assertTrue("Should have valid start offset", warnings[0].startOffset >= 0)
        assertTrue("Should have valid end offset", warnings[0].endOffset > warnings[0].startOffset)
    }
    
    // ========== Comment Handling ==========
    
    @Test
    fun testSingleLineCommentIgnored() {
        val code = """
            x = 1;
            // x = 2;
            y = 3;
        """.trimIndent()
        val warnings = OpenSCADWarnings.findWarnings(code)
        assertEquals("Commented assignment should be ignored", 0, warnings.size)
    }
    
    @Test
    fun testBlockCommentIgnored() {
        val code = """
            x = 1;
            /* x = 2; */
            y = 3;
        """.trimIndent()
        val warnings = OpenSCADWarnings.findWarnings(code)
        assertEquals("Block commented assignment should be ignored", 0, warnings.size)
    }
    
    @Test
    fun testMultilineBlockCommentIgnored() {
        val code = """
            x = 1;
            /*
            x = 2;
            x = 3;
            */
            y = 4;
        """.trimIndent()
        val warnings = OpenSCADWarnings.findWarnings(code)
        assertEquals("Multiline block comment should be ignored", 0, warnings.size)
    }
    
    @Test
    fun testInlineCommentAfterCode() {
        // Code before comment should still be checked
        val code = """
            x = 1;
            x = 2; // this is a comment
        """.trimIndent()
        val warnings = OpenSCADWarnings.findWarnings(code)
        assertEquals("Code before comment should still warn", 1, warnings.size)
    }
    
    // ========== Real-world Global Reassignment ==========
    
    @Test
    fun testGlobalVariablesReassignedAfterModules() {
        // Variables at global scope before modules, then reassigned after modules
        // This matches the pattern in test-use.scad: fa, fs, fn at lines 89-93
        // are reassigned at lines 1234-1240 after many module definitions
        val code = """
            fa = 6;
            fs = 0.1;
            fn = 0;
            
            module foo() {
                echo("in foo");
            }
            
            module bar() {
                x = 1;
                echo(x);
            }
            
            fn = 36;
            fs = 0.75;
            fa = 10;
        """.trimIndent()
        val warnings = OpenSCADWarnings.findWarnings(code)
        assertEquals("Should detect 3 global reassignments", 3, warnings.size)
    }
    
    @Test
    fun testTripleSlashCommentNotBlockComment() {
        // /// is a single-line comment, not special syntax
        val code = """
            x = 1;
            /// this is a comment
            x = 2;
        """.trimIndent()
        val warnings = OpenSCADWarnings.findWarnings(code)
        assertEquals("Should detect reassignment after /// comment", 1, warnings.size)
    }
    
    @Test
    fun testActualTestUseScadPattern() {
        // Exact pattern from test-use.scad lines 89-93 and 1234-1240
        val code = """
            /* [Model detail] */
            // minimum angle
            fa = 6;
            // minimum size
            fs = 0.1;
            // number of fragments
            fn = 0;
            
            module grid_block(
              num_x=1,
              num_y=2) {
                echo(num_x);
            }
            
            /// render with Hires
            hires=false;
            fn=${"$"}fn?${"$"}fn:${"$"}preview?36:
                                      hires?144:
                                            72;
            
            
            fs=${"$"}preview?.75:hires?.1:.2;
            fa=${"$"}preview?10:hires?.5:1;
        """.trimIndent()
        val warnings = OpenSCADWarnings.findWarnings(code)
        assertEquals("Should detect fa, fs, fn reassignments", 3, warnings.size)
    }
    
    @Test
    fun testActualTestUseScadFile() {
        // Test against the actual test-use.scad file
        val file = File("test-use.scad")
        if (!file.exists()) {
            // Skip if file doesn't exist in test environment
            return
        }
        val code = file.readText()
        val warnings = OpenSCADWarnings.findWarnings(code)
        
        // Find warnings for fa, fs, fn
        val faWarnings = warnings.filter { it.message.contains("'fa'") }
        val fsWarnings = warnings.filter { it.message.contains("'fs'") }
        val fnWarnings = warnings.filter { it.message.contains("'fn'") }
        
        assertTrue("Should detect fa reassignment (line 89 -> 1240)", faWarnings.isNotEmpty())
        assertTrue("Should detect fs reassignment (line 91 -> 1239)", fsWarnings.isNotEmpty())
        assertTrue("Should detect fn reassignment (line 93 -> 1234)", fnWarnings.isNotEmpty())
    }
}
