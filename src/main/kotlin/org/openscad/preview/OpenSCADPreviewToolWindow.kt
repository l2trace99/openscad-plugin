package org.openscad.preview

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import org.openscad.settings.OpenSCADSettings
import java.awt.BorderLayout
import java.awt.FlowLayout
import javax.swing.*

/**
 * Tool window factory for OpenSCAD preview
 */
class OpenSCADPreviewToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val previewPanel = OpenSCADPreviewPanel(project)
        val content = ContentFactory.getInstance().createContent(previewPanel, "", false)
        toolWindow.contentManager.addContent(content)
    }
}

/**
 * Main preview panel containing the STL viewer and controls
 */
class OpenSCADPreviewPanel(private val project: Project) : JPanel(BorderLayout()) {
    
    private val logger = Logger.getInstance(OpenSCADPreviewPanel::class.java)
    private val renderer = OpenSCADRenderer(project)
    private val stlParser = STLParser()
    private val threemfParser = ThreeMFParser()
    private val viewerPanel: JComponent
    private val viewer: Any // Can be STLViewer3D or STLViewerPanel
    
    private val statusLabel = JLabel("Ready")
    private val renderButton = JButton("Render")
    private val resetViewButton = JButton("Reset View")
    private val debugPreviewButton = JButton("Debug Preview")
    private val exportSTLButton = JButton("Export STL")
    
    private var currentFile: VirtualFile? = null
    private var isRendering = false
    
    init {
        // Initialize viewer based on hardware acceleration setting
        val settings = OpenSCADSettings.getInstance(project)
        val (panel, viewerInstance) = if (settings.useHardwareAcceleration) {
            try {
                val v = STLViewer3D(project)
                logger.info("Initialized hardware-accelerated JOGL viewer")
                Pair(v as JComponent, v as Any)
            } catch (e: Exception) {
                logger.warn("Failed to initialize JOGL viewer, falling back to software renderer", e)
                val v = STLViewerPanel()
                Pair(v as JComponent, v as Any)
            }
        } else {
            val v = STLViewerPanel()
            logger.info("Initialized software renderer")
            Pair(v as JComponent, v as Any)
        }
        viewerPanel = panel
        viewer = viewerInstance
        
        setupUI()
        setupListeners()
        checkOpenSCADAvailability()
    }
    
    private fun setupUI() {
        // Toolbar
        val toolbar = JPanel(FlowLayout(FlowLayout.LEFT))
        toolbar.add(renderButton)
        toolbar.add(resetViewButton)
        toolbar.add(debugPreviewButton)
        toolbar.add(exportSTLButton)
        toolbar.add(Box.createHorizontalStrut(20))
        toolbar.add(statusLabel)
        
        add(toolbar, BorderLayout.NORTH)
        add(viewerPanel, BorderLayout.CENTER)
        
        // Button actions
        renderButton.addActionListener {
            currentFile?.let { renderCurrentFile(it) }
        }
        
        resetViewButton.addActionListener {
            when (viewer) {
                is STLViewer3D -> viewer.resetView()
                is STLViewerPanel -> viewer.resetView()
            }
        }
        
        exportSTLButton.addActionListener {
            currentFile?.let { exportSTL(it) }
        }
        
        debugPreviewButton.addActionListener {
            currentFile?.let { renderDebugPreview(it) }
        }

        // Debug preview requires STLViewerPanel features (camera sync, image preview)
        if (viewer !is STLViewerPanel) {
            debugPreviewButton.isEnabled = false
            debugPreviewButton.toolTipText = "Debug preview requires software renderer"
        }
    }

    private fun setupListeners() {
        // Listen for file editor changes
        project.messageBus.connect().subscribe(
            FileEditorManagerListener.FILE_EDITOR_MANAGER,
            object : FileEditorManagerListener {
                override fun selectionChanged(event: FileEditorManagerEvent) {
                    val file = event.newFile
                    if (file != null && file.extension == "scad") {
                        currentFile = file
                        updateStatus("File selected: ${file.name}")
                    }
                }
            }
        )
        
        // Check currently open file
        val currentEditor = FileEditorManager.getInstance(project).selectedFiles.firstOrNull()
        if (currentEditor?.extension == "scad") {
            currentFile = currentEditor
        }
    }
    
    private fun checkOpenSCADAvailability() {
        if (!renderer.isOpenSCADAvailable()) {
            updateStatus("⚠️ OpenSCAD not found. Please install OpenSCAD.")
            renderButton.isEnabled = false
        } else {
            updateStatus("✓ OpenSCAD found. Ready to render.")
        }
    }
    
    private fun renderCurrentFile(file: VirtualFile) {
        if (isRendering) {
            updateStatus("Already rendering...")
            return
        }
        
        isRendering = true
        renderButton.isEnabled = false
        updateStatus("Rendering ${file.name}...")

        // Flush in-memory VFS changes to disk for this file before rendering
        FileDocumentManager.getInstance().getDocument(file)?.let {
            FileDocumentManager.getInstance().saveDocument(it)
        }

        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                // Try 3MF first (preserves colors from color() statements)
                logger.info("Starting 3MF render for ${file.name}")
                val threemfPath = renderer.renderTo3MF(file)
                logger.info("3MF render result: $threemfPath")
                
                if (threemfPath != null) {
                    logger.info("Parsing 3MF file...")
                    val coloredModel = threemfParser.parse(threemfPath)
                    logger.info("3MF parse result: ${coloredModel?.triangles?.size ?: "null"} triangles")
                    
                    if (coloredModel != null && coloredModel.triangles.isNotEmpty()) {
                        val uniqueColors = coloredModel.triangles.map { it.color }.distinct()
                        logger.info("Unique colors in model: ${uniqueColors.size} - $uniqueColors")
                        SwingUtilities.invokeLater {
                            when (viewer) {
                                is STLViewer3D -> viewer.setColoredModel(coloredModel)
                                is STLViewerPanel -> viewer.setColoredModel(coloredModel)
                            }
                            updateStatus("✓ Rendered with colors (${coloredModel.triangles.size} triangles)")
                        }
                        return@executeOnPooledThread
                    } else {
                        logger.warn("3MF parsed but no triangles found, falling back to STL")
                    }
                } else {
                    logger.warn("3MF render returned null, falling back to STL")
                }
                
                // Fall back to STL if 3MF fails
                val stlPath = renderer.renderToSTL(file)
                
                if (stlPath != null) {
                    val model = stlParser.parse(stlPath)
                    
                    SwingUtilities.invokeLater {
                        if (model != null) {
                            when (viewer) {
                                is STLViewer3D -> viewer.setModel(model)
                                is STLViewerPanel -> viewer.setModel(model)
                            }
                            updateStatus("✓ Rendered successfully (${model.triangles.size} triangles)")
                        } else {
                            clearModel()
                            updateStatus("✗ Failed to parse STL file")
                        }
                    }
                } else {
                    SwingUtilities.invokeLater {
                        clearModel()
                        updateStatus("✗ Rendering failed")
                    }
                }
            } catch (e: Exception) {
                logger.error("Error rendering file", e)
                SwingUtilities.invokeLater {
                    clearModel()
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
            logger.info(message)
        }
    }
    
    private fun clearModel() {
        when (viewer) {
            is STLViewer3D -> viewer.setModel(null)
            is STLViewerPanel -> viewer.setModel(null)
        }
    }
    
    private fun renderDebugPreview(file: VirtualFile) {
        if (isRendering) {
            updateStatus("Already rendering...")
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

        // Flush in-memory VFS changes to disk for this file before rendering
        FileDocumentManager.getInstance().getDocument(file)?.let {
            FileDocumentManager.getInstance().saveDocument(it)
        }

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
                        (viewer as? STLViewerPanel)?.setPreviewImage(pngPath)
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
    
    private fun exportSTL(file: VirtualFile) {
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
}
