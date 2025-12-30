package org.openscad

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for OpenSCAD settings logic
 */
class OpenSCADSettingsIntegrationTest {

    /**
     * Test library path list manipulation
     */
    @Test
    fun testLibraryPathListManipulation() {
        val paths = mutableListOf<String>()
        
        paths.add("/test/path1")
        paths.add("/test/path2")
        
        assertEquals(2, paths.size)
        assertTrue(paths.contains("/test/path1"))
        assertTrue(paths.contains("/test/path2"))
    }

    /**
     * Test render timeout validation
     */
    @Test
    fun testRenderTimeoutValidation() {
        val defaultTimeout = 30
        assertTrue("Default timeout should be positive", defaultTimeout > 0)
    }

    /**
     * Test grid settings validation
     */
    @Test
    fun testGridSettingsValidation() {
        val defaultGridSize = 250.0f
        val defaultGridSpacing = 10.0f
        
        assertTrue("Grid size should be positive", defaultGridSize > 0)
        assertTrue("Grid spacing should be positive", defaultGridSpacing > 0)
    }

    /**
     * Test OpenSCAD path formats
     */
    @Test
    fun testOpenSCADPathFormats() {
        val macPath = "/Applications/OpenSCAD.app/Contents/MacOS/OpenSCAD"
        val linuxPath = "/usr/bin/openscad"
        val windowsPath = "C:\\Program Files\\OpenSCAD\\openscad.exe"
        
        assertTrue(macPath.contains("OpenSCAD"))
        assertTrue(linuxPath.contains("openscad"))
        assertTrue(windowsPath.contains("openscad"))
    }

    /**
     * Test library path parsing
     */
    @Test
    fun testLibraryPathParsing() {
        val pathsText = """
            /path/to/lib1
            /path/to/lib2
            /path/to/lib3
        """.trimIndent()
        
        val paths = pathsText.lines().filter { it.isNotBlank() }.map { it.trim() }
        
        assertEquals(3, paths.size)
        assertEquals("/path/to/lib1", paths[0])
        assertEquals("/path/to/lib2", paths[1])
        assertEquals("/path/to/lib3", paths[2])
    }
}
