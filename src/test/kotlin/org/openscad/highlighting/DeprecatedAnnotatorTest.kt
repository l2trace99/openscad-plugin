package org.openscad.highlighting

import org.junit.Test
import org.junit.Assert.*
import java.io.File

/**
 * Tests for OpenSCAD deprecated syntax detection.
 * Based on OpenSCAD source code deprecation patterns:
 * - filename= parameter (use file= instead)
 * - layername= parameter (use layer= instead)
 * - triangles= parameter in polyhedron (use faces= instead)
 * - AMF file imports (use 3MF instead)
 */
class DeprecatedAnnotatorTest {

    // ========== Deprecated Parameter Names ==========
    
    @Test
    fun testFilenameParameterDeprecated() {
        val deprecations = OpenSCADDeprecations.findDeprecations("import(filename=\"test.stl\");")
        assertEquals("filename= should be flagged as deprecated", 1, deprecations.size)
        assertTrue("Should suggest file= as replacement", deprecations[0].message.contains("file="))
    }
    
    @Test
    fun testFileParameterNotDeprecated() {
        val deprecations = OpenSCADDeprecations.findDeprecations("import(file=\"test.stl\");")
        assertEquals("file= should not be flagged", 0, deprecations.size)
    }
    
    @Test
    fun testLayernameParameterDeprecated() {
        val deprecations = OpenSCADDeprecations.findDeprecations("import(file=\"test.dxf\", layername=\"cuts\");")
        assertEquals("layername= should be flagged as deprecated", 1, deprecations.size)
        assertTrue("Should suggest layer= as replacement", deprecations[0].message.contains("layer="))
    }
    
    @Test
    fun testLayerParameterNotDeprecated() {
        val deprecations = OpenSCADDeprecations.findDeprecations("import(file=\"test.dxf\", layer=\"cuts\");")
        assertEquals("layer= should not be flagged", 0, deprecations.size)
    }
    
    @Test
    fun testTrianglesParameterDeprecated() {
        val deprecations = OpenSCADDeprecations.findDeprecations(
            "polyhedron(points=[[0,0,0],[1,0,0],[0,1,0]], triangles=[[0,1,2]]);"
        )
        assertEquals("triangles= should be flagged as deprecated", 1, deprecations.size)
        assertTrue("Should suggest faces= as replacement", deprecations[0].message.contains("faces="))
    }
    
    @Test
    fun testFacesParameterNotDeprecated() {
        val deprecations = OpenSCADDeprecations.findDeprecations(
            "polyhedron(points=[[0,0,0],[1,0,0],[0,1,0]], faces=[[0,1,2]]);"
        )
        assertEquals("faces= should not be flagged", 0, deprecations.size)
    }
    
    // ========== Deprecated File Formats ==========
    
    @Test
    fun testAmfImportDeprecated() {
        val deprecations = OpenSCADDeprecations.findDeprecations("import(\"model.amf\");")
        assertEquals("AMF import should be flagged as deprecated", 1, deprecations.size)
        assertTrue("Should suggest 3MF as replacement", deprecations[0].message.contains("3MF"))
    }
    
    @Test
    fun test3mfImportNotDeprecated() {
        val deprecations = OpenSCADDeprecations.findDeprecations("import(\"model.3mf\");")
        assertEquals("3MF import should not be flagged", 0, deprecations.size)
    }
    
    @Test
    fun testStlImportNotDeprecated() {
        val deprecations = OpenSCADDeprecations.findDeprecations("import(\"model.stl\");")
        assertEquals("STL import should not be flagged", 0, deprecations.size)
    }
    
    // ========== Multiple Deprecations ==========
    
    @Test
    fun testMultipleDeprecationsInSameFile() {
        val code = """
            import(filename="old.stl");
            polyhedron(points=p, triangles=t);
            import("model.amf");
        """.trimIndent()
        val deprecations = OpenSCADDeprecations.findDeprecations(code)
        assertEquals("Should find all three deprecations", 3, deprecations.size)
    }
    
    // ========== Deprecation Info Structure ==========
    
    @Test
    fun testDeprecationContainsOffset() {
        val code = "import(filename=\"test.stl\");"
        val deprecations = OpenSCADDeprecations.findDeprecations(code)
        assertEquals(1, deprecations.size)
        assertTrue("Should have valid start offset", deprecations[0].startOffset >= 0)
        assertTrue("Should have valid end offset", deprecations[0].endOffset > deprecations[0].startOffset)
    }
    
    // ========== Identifiers Starting with Digits ==========
    
    @Test
    fun testVariableStartingWithDigitDeprecated() {
        val code = "2x = 10;"
        val deprecations = OpenSCADDeprecations.findDeprecations(code)
        assertEquals("Variable starting with digit should be deprecated", 1, deprecations.size)
        assertTrue("Should mention digit", deprecations[0].message.lowercase().contains("digit"))
    }
    
    @Test
    fun testVariableStartingWithLetterNotDeprecated() {
        val code = "x2 = 10;"
        val deprecations = OpenSCADDeprecations.findDeprecations(code)
        assertEquals("Variable starting with letter should not be flagged", 0, deprecations.size)
    }
    
    @Test
    fun testMultipleDigitStartingVariables() {
        val code = """
            1var = 1;
            2var = 2;
            3d = [1,2,3];
        """.trimIndent()
        val deprecations = OpenSCADDeprecations.findDeprecations(code)
        assertEquals("Should find all digit-starting variables", 3, deprecations.size)
    }
    
    @Test
    fun testModuleNameStartingWithDigitDeprecated() {
        val code = "module 2d_shape() { square(10); }"
        val deprecations = OpenSCADDeprecations.findDeprecations(code)
        assertEquals("Module name starting with digit should be deprecated", 1, deprecations.size)
        assertTrue("Should mention digit", deprecations[0].message.lowercase().contains("digit"))
    }
    
    @Test
    fun testFunctionNameStartingWithDigitDeprecated() {
        val code = "function 3d_calc(x) = x * 2;"
        val deprecations = OpenSCADDeprecations.findDeprecations(code)
        assertEquals("Function name starting with digit should be deprecated", 1, deprecations.size)
        assertTrue("Should mention digit", deprecations[0].message.lowercase().contains("digit"))
    }
    
    @Test
    fun testModuleNameStartingWithLetterNotDeprecated() {
        val code = "module shape2d() { square(10); }"
        val deprecations = OpenSCADDeprecations.findDeprecations(code)
        assertEquals("Module name starting with letter should not be flagged", 0, deprecations.size)
    }
    
    @Test
    fun testFunctionNameStartingWithLetterNotDeprecated() {
        val code = "function calc3d(x) = x * 2;"
        val deprecations = OpenSCADDeprecations.findDeprecations(code)
        assertEquals("Function name starting with letter should not be flagged", 0, deprecations.size)
    }
    
    @Test
    fun testDigitIdentifierInExpression() {
        // Line 13406 pattern: if(!2D) - identifier used in expression, not assignment
        val code = "if(!2D) cube(10);"
        val deprecations = OpenSCADDeprecations.findDeprecations(code)
        assertEquals("Digit-starting identifier in expression should be flagged", 1, deprecations.size)
    }
    
    @Test
    fun testDigitIdentifierInCondition() {
        val code = "x = 2D ? 1 : 0;"
        val deprecations = OpenSCADDeprecations.findDeprecations(code)
        // Should flag the 2D usage (but not x which starts with letter)
        assertEquals("Digit-starting identifier in ternary should be flagged", 1, deprecations.size)
    }
    
    @Test
    fun testDigitPatternInsideStringNotFlagged() {
        // Lines 3960-3961: strings like "#00FF00" contain "0FF00" which looks like identifier
        val code = """echo(str("🟢\t ", color=="#00FF00"));"""
        val deprecations = OpenSCADDeprecations.findDeprecations(code)
        assertEquals("Pattern inside string should not be flagged", 0, deprecations.size)
    }
    
    @Test
    fun testHexColorStringsNotFlagged() {
        val code = """
            if (color=="#0000FF"||color=="blue") echo("test");
            if (color=="#00FF00") cube(10);
        """.trimIndent()
        val deprecations = OpenSCADDeprecations.findDeprecations(code)
        assertEquals("Hex colors in strings should not be flagged", 0, deprecations.size)
    }
    
    @Test
    fun testScientificNotationNotFlagged() {
        // Lines 14788/14790: 1e-9 is scientific notation, not an identifier
        val code = "x = abs(floor(x*mult)-x*mult) < 1e-9 ? 0 : 1;"
        val deprecations = OpenSCADDeprecations.findDeprecations(code)
        assertEquals("Scientific notation should not be flagged", 0, deprecations.size)
    }
    
    @Test
    fun testVariousScientificNotationNotFlagged() {
        val code = """
            a = 1e5;
            b = 2E-10;
            c = 3e+5;
            d = 1.5e10;
        """.trimIndent()
        val deprecations = OpenSCADDeprecations.findDeprecations(code)
        assertEquals("Various scientific notations should not be flagged", 0, deprecations.size)
    }
    
    // ========== Comment Handling ==========
    
    @Test
    fun testSingleLineCommentIgnored() {
        val code = """
            // import(filename="test.stl");
            x = 1;
        """.trimIndent()
        val deprecations = OpenSCADDeprecations.findDeprecations(code)
        assertEquals("Commented deprecation should be ignored", 0, deprecations.size)
    }
    
    @Test
    fun testBlockCommentIgnored() {
        val code = """
            /* import(filename="test.stl"); */
            x = 1;
        """.trimIndent()
        val deprecations = OpenSCADDeprecations.findDeprecations(code)
        assertEquals("Block commented deprecation should be ignored", 0, deprecations.size)
    }
    
    @Test
    fun testMultilineBlockCommentIgnored() {
        val code = """
            /*
            import(filename="test.stl");
            2var = 10;
            */
            x = 1;
        """.trimIndent()
        val deprecations = OpenSCADDeprecations.findDeprecations(code)
        assertEquals("Multiline block comment should be ignored", 0, deprecations.size)
    }
    
    @Test
    fun testCodeBeforeCommentStillChecked() {
        val code = """
            import(filename="test.stl"); // this is a comment
        """.trimIndent()
        val deprecations = OpenSCADDeprecations.findDeprecations(code)
        assertEquals("Code before comment should still be flagged", 1, deprecations.size)
    }
    
    // ========== Duplicate Warning Prevention ==========
    
    @Test
    fun testSameDeprecationOnlyReportedOncePerLine() {
        // Multiple uses of same deprecated param on one line should only warn once
        val code = "import(filename=\"a.stl\", filename=\"b.stl\");"
        val deprecations = OpenSCADDeprecations.findDeprecations(code)
        assertEquals("Same deprecation should only be reported once per line", 1, deprecations.size)
    }
    
    @Test
    fun testDigitVariableUsedMultipleTimesOnLine() {
        // Variable like 2D used multiple times on same line - only warn once
        val code = "2D= is_undef(2D)?true:false;"
        val deprecations = OpenSCADDeprecations.findDeprecations(code)
        assertEquals("Same digit-starting var should only be reported once per line", 1, deprecations.size)
    }
    
    @Test
    fun testDigitParameterInModuleCall() {
        // Line 645 pattern: 2D=thickness inside a module call - should only warn once
        val code = "SBogen(\n  2D=thickness,\n  dist=10);"
        val deprecations = OpenSCADDeprecations.findDeprecations(code)
        val line2Deps = deprecations.filter { 
            val lineNum = code.substring(0, it.startOffset).count { c -> c == '\n' } + 1
            lineNum == 2
        }
        assertEquals("2D should only be reported once on line 2", 1, line2Deps.size)
    }
    
    @Test
    fun testExactLine645Pattern() {
        // Exact context from test-use.scad lines 644-645
        val code = """SBogen(
        2D=thickness,""".trimIndent()
        val deprecations = OpenSCADDeprecations.findDeprecations(code)
        // Should only have 1 deprecation for 2D on line 2
        val line2Deps = deprecations.filter { 
            val lineNum = code.substring(0, it.startOffset).count { c -> c == '\n' } + 1
            lineNum == 2
        }
        assertEquals("Should have exactly 1 deprecation on line 2", 1, line2Deps.size)
    }
    
    @Test
    fun testLine645InActualFile() {
        val file = File("test-use.scad")
        if (!file.exists()) return
        
        val code = file.readText()
        val deprecations = OpenSCADDeprecations.findDeprecations(code)
        
        // Check line 645 specifically
        val lines = code.split("\n")
        var offset = 0
        for (i in 0 until 644) {
            offset += lines[i].length + 1
        }
        val line645Start = offset
        val line645End = offset + lines[644].length
        
        val line645Deps = deprecations.filter { it.startOffset >= line645Start && it.startOffset < line645End }
        assertEquals("Line 645 should have exactly 1 deprecation", 1, line645Deps.size)
    }
    
    @Test
    fun testDifferentDeprecationsOnSameLineAllReported() {
        // Different deprecations on same line should all be reported
        val code = "import(filename=\"test.stl\", layername=\"layer1\");"
        val deprecations = OpenSCADDeprecations.findDeprecations(code)
        assertEquals("Different deprecations on same line should all be reported", 2, deprecations.size)
    }
    
    @Test
    fun testSameDeprecationOnDifferentLinesAllReported() {
        // Same deprecation on different lines should be reported for each line
        val code = """
            import(filename="a.stl");
            import(filename="b.stl");
        """.trimIndent()
        val deprecations = OpenSCADDeprecations.findDeprecations(code)
        assertEquals("Same deprecation on different lines should be reported", 2, deprecations.size)
    }
    
    // ========== Integration Tests - Simulating Annotator Behavior ==========
    
    @Test
    fun testNoExactDuplicateOffsets() {
        // Verify findDeprecations never returns two items with same (startOffset, endOffset)
        val file = File("test-use.scad")
        if (!file.exists()) return
        
        val code = file.readText()
        val deprecations = OpenSCADDeprecations.findDeprecations(code)
        
        val seen = mutableSetOf<Pair<Int, Int>>()
        val duplicates = mutableListOf<OpenSCADDeprecations.Deprecation>()
        
        for (dep in deprecations) {
            val key = Pair(dep.startOffset, dep.endOffset)
            if (seen.contains(key)) {
                duplicates.add(dep)
            } else {
                seen.add(key)
            }
        }
        
        if (duplicates.isNotEmpty()) {
            println("DUPLICATE OFFSETS FOUND:")
            duplicates.forEach { println("  ${it.startOffset}-${it.endOffset}: ${it.message}") }
        }
        
        assertEquals("Should have no duplicate offsets", 0, duplicates.size)
    }
    
    @Test
    fun testAnnotatorSimulation() {
        // Simulate exactly what the annotator does
        val file = File("test-use.scad")
        if (!file.exists()) return
        
        val code = file.readText()
        val deprecations = OpenSCADDeprecations.findDeprecations(code)
        
        // Simulate multiple PSI elements containing the same deprecation
        // Each deprecation should only be "annotated" once
        val appliedAnnotations = mutableSetOf<Pair<Int, Int>>()
        var totalAnnotations = 0
        
        // Simulate visiting each deprecation range as if it were in a PSI element
        for (dep in deprecations) {
            val key = Pair(dep.startOffset, dep.endOffset)
            if (!appliedAnnotations.contains(key)) {
                appliedAnnotations.add(key)
                totalAnnotations++
            }
        }
        
        assertEquals("Applied count should match unique deprecations", 
            deprecations.size, totalAnnotations)
        assertEquals("Applied set size should match deprecations size",
            deprecations.size, appliedAnnotations.size)
    }
}
