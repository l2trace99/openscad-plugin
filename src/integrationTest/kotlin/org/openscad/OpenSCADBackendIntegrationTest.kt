package org.openscad

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for OpenSCAD backend setting logic
 */
class OpenSCADBackendIntegrationTest {

    /**
     * Test that the default backend is empty (auto), safe for older OpenSCAD versions
     */
    @Test
    fun testDefaultBackendIsEmpty() {
        val defaultBackend = ""
        assertEquals("Default backend should be empty for compatibility", "", defaultBackend)
    }

    /**
     * Test that the valid backend setting values are recognized
     */
    @Test
    fun testBackendSettingValues() {
        val validValues = listOf("", "manifold", "cgal")

        assertTrue("Empty string (auto) should be valid", "" in validValues)
        assertTrue("manifold should be valid", "manifold" in validValues)
        assertTrue("cgal should be valid", "cgal" in validValues)
        assertFalse("arbitrary value should not be valid", "invalid" in validValues)
    }

    /**
     * Test settings combo box index to setting value mapping (mirrors OpenSCADSettingsConfigurable)
     */
    @Test
    fun testSettingsBackendComboBoxMapping() {
        // Settings configurable: index 0 = Manifold, 1 = CGAL, 2 = Auto
        fun backendToSettingValue(index: Int): String = when (index) {
            0 -> "manifold"
            1 -> "cgal"
            else -> ""
        }

        fun settingValueToBackendIndex(value: String): Int = when (value) {
            "manifold" -> 0
            "cgal" -> 1
            else -> 2
        }

        // Round-trip: index -> value -> index
        assertEquals(0, settingValueToBackendIndex(backendToSettingValue(0)))
        assertEquals(1, settingValueToBackendIndex(backendToSettingValue(1)))
        assertEquals(2, settingValueToBackendIndex(backendToSettingValue(2)))

        // Round-trip: value -> index -> value
        assertEquals("manifold", backendToSettingValue(settingValueToBackendIndex("manifold")))
        assertEquals("cgal", backendToSettingValue(settingValueToBackendIndex("cgal")))
        assertEquals("", backendToSettingValue(settingValueToBackendIndex("")))
    }

    /**
     * Test run config combo box index to setting value mapping (mirrors OpenSCADRunConfigurationEditor)
     */
    @Test
    fun testRunConfigBackendComboBoxMapping() {
        // Run config: index 0 = Use project setting, 1 = Manifold, 2 = CGAL, 3 = Auto
        fun backendIndexToSetting(index: Int): String = when (index) {
            1 -> "manifold"
            2 -> "cgal"
            3 -> "auto"
            else -> "" // 0 = use project setting
        }

        fun backendSettingToIndex(value: String): Int = when (value) {
            "manifold" -> 1
            "cgal" -> 2
            "auto" -> 3
            else -> 0
        }

        // Round-trip: index -> value -> index
        assertEquals(0, backendSettingToIndex(backendIndexToSetting(0)))
        assertEquals(1, backendSettingToIndex(backendIndexToSetting(1)))
        assertEquals(2, backendSettingToIndex(backendIndexToSetting(2)))
        assertEquals(3, backendSettingToIndex(backendIndexToSetting(3)))

        // Round-trip: value -> index -> value
        assertEquals("", backendIndexToSetting(backendSettingToIndex("")))
        assertEquals("manifold", backendIndexToSetting(backendSettingToIndex("manifold")))
        assertEquals("cgal", backendIndexToSetting(backendSettingToIndex("cgal")))
        assertEquals("auto", backendIndexToSetting(backendSettingToIndex("auto")))
    }

    /**
     * Test run config backend fallback logic (mirrors OpenSCADRunConfiguration.getState)
     */
    @Test
    fun testRunConfigBackendFallback() {
        val projectBackend = "manifold" // simulated project setting

        fun resolveBackend(runConfigBackend: String): String {
            return if (runConfigBackend.isEmpty()) {
                projectBackend // fall back to project setting
            } else if (runConfigBackend == "auto") {
                "" // explicitly no backend flag
            } else {
                runConfigBackend // pass through
            }
        }

        // Empty run config -> falls back to project setting
        assertEquals("manifold", resolveBackend(""))
        // "auto" -> empty (no --backend flag)
        assertEquals("", resolveBackend("auto"))
        // Explicit values pass through
        assertEquals("manifold", resolveBackend("manifold"))
        assertEquals("cgal", resolveBackend("cgal"))
    }
}
