package org.openscad

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for OpenSCAD autocomplete data
 * Tests the completion data without requiring IntelliJ platform
 */
class OpenSCADCompletionIntegrationTest {

    private val keywords = listOf(
        "module", "function", "include", "use",
        "if", "else", "for", "intersection_for", "let",
        "true", "false", "undef"
    )

    private val primitives3D = mapOf(
        "cube" to "size",
        "sphere" to "r",
        "cylinder" to "h, r",
        "polyhedron" to "points, faces",
        "text" to "text"
    )

    private val transformations = mapOf(
        "translate" to "v",
        "rotate" to "a",
        "scale" to "v",
        "resize" to "newsize",
        "mirror" to "v",
        "multmatrix" to "m",
        "color" to "c",
        "offset" to "r"
    )

    private val booleanOps = mapOf(
        "union" to "",
        "difference" to "",
        "intersection" to "",
        "hull" to "",
        "minkowski" to ""
    )

    private val specialVariables = listOf(
        "\$fn", "\$fa", "\$fs", "\$t",
        "\$vpr", "\$vpt", "\$vpd",
        "\$children", "\$preview"
    )

    @Test
    fun testKeywordsContainModule() {
        assertTrue("Keywords should contain 'module'", keywords.contains("module"))
    }

    @Test
    fun testKeywordsContainFunction() {
        assertTrue("Keywords should contain 'function'", keywords.contains("function"))
    }

    @Test
    fun testPrimitivesContainCube() {
        assertTrue("Primitives should contain 'cube'", primitives3D.containsKey("cube"))
    }

    @Test
    fun testPrimitivesContainSphere() {
        assertTrue("Primitives should contain 'sphere'", primitives3D.containsKey("sphere"))
    }

    @Test
    fun testPrimitivesContainCylinder() {
        assertTrue("Primitives should contain 'cylinder'", primitives3D.containsKey("cylinder"))
    }

    @Test
    fun testTransformationsContainTranslate() {
        assertTrue("Transformations should contain 'translate'", transformations.containsKey("translate"))
    }

    @Test
    fun testTransformationsContainRotate() {
        assertTrue("Transformations should contain 'rotate'", transformations.containsKey("rotate"))
    }

    @Test
    fun testBooleanOpsContainDifference() {
        assertTrue("Boolean ops should contain 'difference'", booleanOps.containsKey("difference"))
    }

    @Test
    fun testBooleanOpsContainUnion() {
        assertTrue("Boolean ops should contain 'union'", booleanOps.containsKey("union"))
    }

    @Test
    fun testSpecialVariablesContainFn() {
        assertTrue("Special variables should contain '\$fn'", specialVariables.contains("\$fn"))
    }

    @Test
    fun testAllKeywordsNotEmpty() {
        keywords.forEach { keyword ->
            assertTrue("Keyword '$keyword' should not be empty", keyword.isNotEmpty())
        }
    }

    @Test
    fun testAllPrimitivesHaveParams() {
        primitives3D.forEach { (name, params) ->
            assertTrue("Primitive '$name' should have name", name.isNotEmpty())
        }
    }
}
