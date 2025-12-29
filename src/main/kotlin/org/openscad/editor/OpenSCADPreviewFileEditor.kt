package org.openscad.editor

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorLocation
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import org.openscad.preview.OpenSCADRenderer
import org.openscad.preview.STLParser
import org.openscad.preview.STLViewer3D
import org.openscad.preview.STLViewerPanel
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.beans.PropertyChangeListener
import javax.swing.*

/**
 * File editor for OpenSCAD preview panel
 */
class OpenSCADPreviewFileEditor(
    private val project: Project,
    private val file: VirtualFile
) : UserDataHolderBase(), FileEditor {
    
    private val logger = Logger.getInstance(OpenSCADPreviewFileEditor::class.java)
    private val renderer = OpenSCADRenderer(project)
    private val parser = STLParser()
    
    private val component: JComponent
    private val viewer: Any
    private val viewerPanel: JComponent
    
    private val statusLabel = JLabel("Ready")
    private val renderButton = JButton("Render")
    private val resetViewButton = JButton("Reset View")
    private val wireframeButton = JButton("Wireframe")
    private val debugPreviewButton = JButton("Debug Preview")
    private val exportSTLButton = JButton("Export STL")
    private val autoRenderCheckbox = JCheckBox("Auto-render", false)
    
    private var isRendering = false
    
    init {
        // Initialize viewer - always use simple wireframe viewer for now
        // JOGL has issues with ARM64 Mac (Apple Silicon) native libraries
        val v = STLViewerPanel()
        viewerPanel = v
        viewer = v
        logger.info("Initialized simple wireframe viewer")
        
        // Create main component
        component = JPanel(BorderLayout()).apply {
            // Toolbar
            val toolbar = JPanel(FlowLayout(FlowLayout.LEFT, 5, 5))
            toolbar.add(renderButton)
            toolbar.add(resetViewButton)
            toolbar.add(wireframeButton)
            toolbar.add(debugPreviewButton)
            toolbar.add(JSeparator(SwingConstants.VERTICAL))
            toolbar.add(exportSTLButton)
            toolbar.add(JSeparator(SwingConstants.VERTICAL))
            toolbar.add(autoRenderCheckbox)
            toolbar.add(Box.createHorizontalStrut(10))
            toolbar.add(statusLabel)
            
            add(toolbar, BorderLayout.NORTH)
            add(viewerPanel, BorderLayout.CENTER)
        }
        
        setupListeners()
        checkOpenSCADAvailability()
    }
    
    private fun setupListeners() {
        renderButton.addActionListener {
            renderFile()
        }
        
        resetViewButton.addActionListener {
            (viewer as? STLViewerPanel)?.resetView()
        }
        
        wireframeButton.addActionListener {
            (viewer as? STLViewerPanel)?.toggleWireframe()
        }
        
        debugPreviewButton.addActionListener {
            renderDebugPreview()
        }
        
        exportSTLButton.addActionListener {
            exportSTL()
        }
        
        autoRenderCheckbox.addActionListener {
            if (autoRenderCheckbox.isSelected) {
                renderFile()
            }
        }
        
        // Listen for file save events
        val connection = project.messageBus.connect()
        connection.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, object : FileEditorManagerListener {
            override fun fileClosed(source: FileEditorManager, file: VirtualFile) {
                // Cleanup when file is closed
            }
        })
        
        // Listen for file save events using bulk file listener
        connection.subscribe(com.intellij.openapi.vfs.VirtualFileManager.VFS_CHANGES,
            object : BulkFileListener {
                override fun after(events: List<VFileEvent>) {
                    events.forEach { event ->
                        if (event.file == file && autoRenderCheckbox.isSelected) {
                            // Render after save completes
                            ApplicationManager.getApplication().invokeLater {
                                renderFile()
                            }
                        }
                    }
                }
            }
        )
    }
    
    private fun checkOpenSCADAvailability() {
        if (!renderer.isOpenSCADAvailable()) {
            updateStatus("⚠️ OpenSCAD not found. Configure in Settings → Tools → OpenSCAD")
            renderButton.isEnabled = false
        } else {
            updateStatus("✓ Ready to render")
        }
    }
    
    private fun renderFile() {
        if (isRendering) {
            updateStatus("Already rendering...")
            return
        }
        
        if (!file.isValid) {
            updateStatus("✗ File is not valid")
            return
        }
        
        isRendering = true
        renderButton.isEnabled = false
        updateStatus("Rendering ${file.name}...")
        
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val stlPath = renderer.renderToSTL(file)
                
                if (stlPath != null) {
                    val model = parser.parse(stlPath)
                    
                    SwingUtilities.invokeLater {
                        if (model != null) {
                            (viewer as? STLViewerPanel)?.setModel(model)
                            updateStatus("✓ Rendered (${model.triangles.size} triangles)")
                        } else {
                            updateStatus("✗ Failed to parse STL")
                        }
                    }
                } else {
                    SwingUtilities.invokeLater {
                        updateStatus("✗ Rendering failed. Check OpenSCAD syntax.")
                    }
                }
            } catch (e: Exception) {
                logger.error("Error rendering file", e)
                SwingUtilities.invokeLater {
                    updateStatus("✗ Error: ${e.message}")
                }
            } finally {
                SwingUtilities.invokeLater {
                    isRendering = false
                    renderButton.isEnabled = true
                }
            }
        }
    }
    
    private fun updateStatus(message: String) {
        SwingUtilities.invokeLater {
            statusLabel.text = message
        }
    }
    
    private fun renderDebugPreview() {
        if (isRendering) {
            updateStatus("Already rendering...")
            return
        }
        
        if (!file.isValid) {
            updateStatus("✗ File is not valid")
            return
        }
        
        // Check if already showing debug preview, toggle back to 3D view
        val viewerPanel = viewer as? STLViewerPanel
        if (viewerPanel?.isShowingImagePreview() == true) {
            viewerPanel.toggleImagePreview()
            debugPreviewButton.text = "Debug Preview"
            updateStatus("✓ Switched to 3D view")
            return
        }
        
        isRendering = true
        debugPreviewButton.isEnabled = false
        updateStatus("Rendering debug preview...")
        
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                // Get current view parameters for camera synchronization
                val cameraParams = (viewer as? STLViewerPanel)?.getViewParameters()?.let { vp ->
                    OpenSCADRenderer.CameraParameters(
                        rotX = vp.rotX,
                        rotY = vp.rotY,
                        rotZ = vp.rotZ,
                        distance = vp.distance
                    )
                }
                val pngPath = renderer.renderToPNG(file, 1024, 768, cameraParams)
                
                SwingUtilities.invokeLater {
                    if (pngPath != null) {
                        viewerPanel?.setPreviewImage(pngPath)
                        debugPreviewButton.text = "3D View"
                        updateStatus("✓ Debug preview (shows # % ! * modifiers)")
                    } else {
                        updateStatus("✗ Debug preview failed")
                    }
                }
            } catch (e: Exception) {
                logger.error("Error rendering debug preview", e)
                SwingUtilities.invokeLater {
                    updateStatus("✗ Error: ${e.message}")
                }
            } finally {
                SwingUtilities.invokeLater {
                    isRendering = false
                    debugPreviewButton.isEnabled = true
                }
            }
        }
    }
    
    private fun exportSTL() {
        if (!file.isValid) {
            updateStatus("✗ File is not valid")
            return
        }
        
        // Show file save dialog
        val descriptor = FileSaverDescriptor("Export to STL", "Choose output STL file", "stl")
        val saveDialog = FileChooserFactory.getInstance().createSaveFileDialog(descriptor, project)
        val fileWrapper = saveDialog.save(file.parent, file.nameWithoutExtension + ".stl") ?: return
        
        val outputFile = fileWrapper.file
        
        updateStatus("Exporting to ${outputFile.name}...")
        
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val stlPath = renderer.renderToSTL(file, outputFile.toPath())
                
                SwingUtilities.invokeLater {
                    if (stlPath != null) {
                        updateStatus("✓ Exported to ${outputFile.name}")
                        NotificationGroupManager.getInstance()
                            .getNotificationGroup("OpenSCAD")
                            .createNotification(
                                "Export Successful",
                                "Exported to ${outputFile.absolutePath}",
                                NotificationType.INFORMATION
                            )
                            .notify(project)
                    } else {
                        updateStatus("✗ Export failed")
                        NotificationGroupManager.getInstance()
                            .getNotificationGroup("OpenSCAD")
                            .createNotification(
                                "Export Failed",
                                "Failed to export STL file",
                                NotificationType.ERROR
                            )
                            .notify(project)
                    }
                }
            } catch (e: Exception) {
                logger.error("Error exporting STL", e)
                SwingUtilities.invokeLater {
                    updateStatus("✗ Export error: ${e.message}")
                }
            }
        }
    }
    
    override fun getComponent(): JComponent = component
    
    override fun getPreferredFocusedComponent(): JComponent? = viewerPanel
    
    override fun getName(): String = "Preview"
    
    override fun setState(state: FileEditorState) {}
    
    override fun isModified(): Boolean = false
    
    override fun isValid(): Boolean = file.isValid
    
    override fun addPropertyChangeListener(listener: PropertyChangeListener) {}
    
    override fun removePropertyChangeListener(listener: PropertyChangeListener) {}
    
    override fun getCurrentLocation(): FileEditorLocation? = null
    
    override fun dispose() {
        // Cleanup if needed
    }
    
    override fun getFile(): VirtualFile = file
}
