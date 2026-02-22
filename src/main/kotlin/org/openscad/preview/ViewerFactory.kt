package org.openscad.preview

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.openscad.settings.OpenSCADSettings
import javax.swing.JComponent

/**
 * Factory for creating STL viewer instances.
 * Centralizes viewer creation logic to avoid duplication between
 * OpenSCADPreviewFileEditor and OpenSCADPreviewToolWindow.
 */
object ViewerFactory {
    
    private val logger = Logger.getInstance(ViewerFactory::class.java)
    
    /**
     * Result of viewer creation containing the panel, viewer instance, and acceleration status.
     */
    data class CreateResult(
        val panel: JComponent,
        val viewer: STLViewer,
        val isHardwareAccelerated: Boolean
    )
    
    /**
     * Creates a viewer based on the project's hardware acceleration setting.
     * Falls back to software renderer if JOGL initialization fails.
     */
    fun create(project: Project): CreateResult {
        val settings = OpenSCADSettings.getInstance(project)
        return if (settings.useHardwareAcceleration) {
            createHardwareViewer(project)
        } else {
            createSoftwareViewer()
        }
    }
    
    /**
     * Creates a software-based STLViewerPanel.
     * Always succeeds and is safe for all platforms.
     */
    fun createSoftwareViewer(): CreateResult {
        val viewer = STLViewerPanel()
        logger.info("Initialized software renderer")
        return CreateResult(
            panel = viewer,
            viewer = viewer,
            isHardwareAccelerated = false
        )
    }
    
    /**
     * Attempts to create a hardware-accelerated JOGL viewer.
     * Falls back to software renderer if JOGL fails (e.g., on ARM64 Mac).
     */
    private fun createHardwareViewer(project: Project): CreateResult {
        return try {
            val viewer = STLViewer3D(project)
            logger.info("Initialized hardware-accelerated JOGL viewer")
            CreateResult(
                panel = viewer,
                viewer = viewer,
                isHardwareAccelerated = true
            )
        } catch (e: Exception) {
            logger.warn("Failed to initialize JOGL viewer, falling back to software renderer", e)
            createSoftwareViewer()
        }
    }
}
