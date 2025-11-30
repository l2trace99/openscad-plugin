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
        
        // Listen for document save events
        val documentManager = com.intellij.openapi.fileEditor.FileDocumentManager.getInstance()
        ApplicationManager.getApplication().messageBus.connect().subscribe(
            com.intellij.AppTopics.FILE_DOCUMENT_SYNC,
            object : com.intellij.openapi.fileEditor.FileDocumentManagerListener {
                override fun beforeDocumentSaving(document: com.intellij.openapi.editor.Document) {
                    val savedFile = documentManager.getFile(document)
                    if (savedFile == file && autoRenderCheckbox.isSelected) {
                        // Render after save completes
                        ApplicationManager.getApplication().invokeLater {
                            renderFile()
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
