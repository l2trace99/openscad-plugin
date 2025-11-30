package org.openscad.preview

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
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
    private val parser = STLParser()
    private val viewerPanel: JComponent
    private val viewer: Any // Can be STLViewer3D or STLViewerPanel
    
    private val statusLabel = JLabel("Ready")
    private val renderButton = JButton("Render")
    private val resetViewButton = JButton("Reset View")
    
    private var currentFile: VirtualFile? = null
    private var isRendering = false
    
    init {
        // Try to initialize JOGL viewer, fallback to simple viewer
        val (panel, viewerInstance) = try {
            val v = STLViewer3D()
            Pair(v as JComponent, v)
        } catch (e: Exception) {
            logger.warn("Failed to initialize JOGL viewer, falling back to simple viewer", e)
            val v = STLViewerPanel()
            Pair(v as JComponent, v)
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
        
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val stlPath = renderer.renderToSTL(file)
                
                if (stlPath != null) {
                    val model = parser.parse(stlPath)
                    
                    SwingUtilities.invokeLater {
                        if (model != null) {
                            when (viewer) {
                                is STLViewer3D -> viewer.setModel(model)
                                is STLViewerPanel -> viewer.setModel(model)
                            }
                            updateStatus("✓ Rendered successfully (${model.triangles.size} triangles)")
                        } else {
                            updateStatus("✗ Failed to parse STL file")
                        }
                    }
                } else {
                    SwingUtilities.invokeLater {
                        updateStatus("✗ Rendering failed")
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
            logger.info(message)
        }
    }
}
