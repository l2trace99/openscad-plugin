package org.openscad.preview

import org.junit.Assert.*
import org.junit.Test
import javax.swing.JComponent

/**
 * Unit tests for ViewerFactory and STLViewer interface
 */
class ViewerFactoryTest {

    // ========== ViewerFactory.createSoftwareViewer() tests ==========

    @Test
    fun testCreateSoftwareViewerReturnsSTLViewerPanel() {
        val result = ViewerFactory.createSoftwareViewer()
        
        assertNotNull("Result should not be null", result)
        assertTrue("Viewer should be STLViewerPanel", result.viewer is STLViewerPanel)
        assertTrue("Viewer should implement STLViewer", result.viewer is STLViewer)
        assertFalse("Should not be hardware accelerated", result.isHardwareAccelerated)
        assertSame("Panel and viewer should be same object for STLViewerPanel", result.panel, result.viewer)
    }

    @Test
    fun testCreateSoftwareViewerPanelIsJComponent() {
        val result = ViewerFactory.createSoftwareViewer()
        assertTrue("Panel should be a JComponent", result.panel is JComponent)
    }

    @Test
    fun testCreateSoftwareViewerIsIdempotent() {
        val result1 = ViewerFactory.createSoftwareViewer()
        val result2 = ViewerFactory.createSoftwareViewer()
        
        // Each call should create a new instance
        assertNotSame("Each call should create new viewer instance", result1.viewer, result2.viewer)
        assertNotSame("Each call should create new panel instance", result1.panel, result2.panel)
        
        // But both should have same properties
        assertEquals("Both should have same acceleration status", 
            result1.isHardwareAccelerated, result2.isHardwareAccelerated)
    }

    // ========== STLViewer interface contract tests ==========

    @Test
    fun testSoftwareViewerImplementsInterface() {
        val result = ViewerFactory.createSoftwareViewer()
        val viewer = result.viewer
        
        // Verify interface methods are callable without exception
        viewer.resetView()
        viewer.setModel(null)
        viewer.setColoredModel(null)
    }

    @Test
    fun testSetModelWithNullDoesNotThrow() {
        val viewer = ViewerFactory.createSoftwareViewer().viewer
        
        // Should not throw
        viewer.setModel(null)
    }

    @Test
    fun testSetColoredModelWithNullDoesNotThrow() {
        val viewer = ViewerFactory.createSoftwareViewer().viewer
        
        // Should not throw
        viewer.setColoredModel(null)
    }

    @Test
    fun testResetViewDoesNotThrow() {
        val viewer = ViewerFactory.createSoftwareViewer().viewer
        
        // Should not throw even when called multiple times
        viewer.resetView()
        viewer.resetView()
    }

    @Test
    fun testMultipleSetModelCallsDoNotThrow() {
        val viewer = ViewerFactory.createSoftwareViewer().viewer
        
        // Should handle multiple calls gracefully
        viewer.setModel(null)
        viewer.setModel(null)
        viewer.setColoredModel(null)
        viewer.setModel(null)
    }

    // ========== CreateResult data class tests ==========

    @Test
    fun testCreateResultTracksHardwareAcceleration() {
        val result = ViewerFactory.createSoftwareViewer()
        assertFalse("Software viewer should not be hardware accelerated", result.isHardwareAccelerated)
    }

    @Test
    fun testCreateResultDataClassEquality() {
        val viewer = STLViewerPanel()
        val result1 = ViewerFactory.CreateResult(
            panel = viewer,
            viewer = viewer,
            isHardwareAccelerated = false
        )
        val result2 = ViewerFactory.CreateResult(
            panel = viewer,
            viewer = viewer,
            isHardwareAccelerated = false
        )
        
        assertEquals("Same data should be equal", result1, result2)
    }

    @Test
    fun testCreateResultDataClassInequalityOnAcceleration() {
        val viewer = STLViewerPanel()
        val softwareResult = ViewerFactory.CreateResult(
            panel = viewer,
            viewer = viewer,
            isHardwareAccelerated = false
        )
        val hardwareResult = ViewerFactory.CreateResult(
            panel = viewer,
            viewer = viewer,
            isHardwareAccelerated = true
        )
        
        assertNotEquals("Different acceleration status should not be equal", 
            softwareResult, hardwareResult)
    }

    // ========== STLViewerPanel-specific tests ==========

    @Test
    fun testSTLViewerPanelImplementsSTLViewer() {
        val panel = STLViewerPanel()
        assertTrue("STLViewerPanel should implement STLViewer", panel is STLViewer)
    }

    @Test
    fun testSTLViewerPanelSetModelOverloads() {
        val panel = STLViewerPanel()
        
        // Interface method (no preserveView param)
        panel.setModel(null)
        
        // Overloaded method with preserveView
        panel.setModel(null, preserveView = false)
        panel.setModel(null, preserveView = true)
    }

    @Test
    fun testSTLViewerPanelSetColoredModelOverloads() {
        val panel = STLViewerPanel()
        
        // Interface method (no preserveView param)
        panel.setColoredModel(null)
        
        // Overloaded method with preserveView
        panel.setColoredModel(null, preserveView = false)
        panel.setColoredModel(null, preserveView = true)
    }

    @Test
    fun testSTLViewerPanelWireframeMethods() {
        val panel = STLViewerPanel()
        
        // These are STLViewerPanel-specific (not in interface)
        panel.setWireframe(true)
        panel.setWireframe(false)
        panel.toggleWireframe()
    }
}
