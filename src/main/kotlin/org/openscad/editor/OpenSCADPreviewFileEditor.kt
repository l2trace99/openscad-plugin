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
import org.openscad.preview.ThreeMFParser
import org.openscad.settings.OpenSCADSettings
import java.awt.BorderLayout
import java.awt.Dimension
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
    private val settings = OpenSCADSettings.getInstance(project)
    private val renderer = OpenSCADRenderer(project)
    private val stlParser = STLParser()
    private val threemfParser = ThreeMFParser()

    private val component: JComponent
    private val viewer: Any
    private val viewerPanel: JComponent

    private val statusLabel = JLabel("Ready")
    private val renderButton = JButton("Render")
    private val resetViewButton = JButton("Reset View")
    private val previewModeButton = JButton("Preview: 3D ▼")
    private var currentPreviewMode = PreviewMode.SOLID_3D

    private enum class PreviewMode(val displayName: String) {
        SOLID_3D("3D"),
        WIREFRAME("Wireframe"),
        OPENSCAD_DEBUG("OpenSCAD Debug")
    }
    private val exportButton = JButton("Export ▼")
    private val autoRenderCheckbox = JCheckBox("auto-refresh")

    private var isRendering = false

    init {
        // Initialize viewer based on hardware acceleration setting
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

        // Create main component
        component = JPanel(BorderLayout()).apply {
            minimumSize = Dimension(50, 50)
            preferredSize = Dimension(400, 400)

            // Toolbar
            val toolbar = JPanel(FlowLayout(FlowLayout.LEFT, 5, 5))
            toolbar.add(renderButton)
            toolbar.add(resetViewButton)
            toolbar.add(previewModeButton)
            toolbar.add(JSeparator(SwingConstants.VERTICAL))
            toolbar.add(exportButton)
            toolbar.add(JSeparator(SwingConstants.VERTICAL))
            toolbar.add(autoRenderCheckbox)
            toolbar.add(Box.createHorizontalStrut(10))
            toolbar.add(statusLabel)

            add(toolbar, BorderLayout.NORTH)
            add(viewerPanel, BorderLayout.CENTER)
        }

        // Initialize auto-refresh from settings
        autoRenderCheckbox.isSelected = settings.autoRenderOnSave

        setupListeners()
        checkOpenSCADAvailability()
    }

    private fun setupListeners() {
        renderButton.addActionListener {
            renderForCurrentMode()
        }

        resetViewButton.addActionListener {
            when (viewer) {
                is STLViewer3D -> viewer.resetView()
                is STLViewerPanel -> viewer.resetView()
            }
        }

        previewModeButton.addActionListener {
            val popup = JPopupMenu()
            val availableModes = if (viewer is STLViewerPanel) {
                PreviewMode.values().toList()
            } else {
                // JOGL viewer only supports solid 3D mode
                listOf(PreviewMode.SOLID_3D)
            }
            availableModes.forEach { mode ->
                popup.add(JMenuItem(mode.displayName).apply {
                    addActionListener {
                        setPreviewMode(mode)
                    }
                })
            }
            popup.show(previewModeButton, 0, previewModeButton.height)
        }

        exportButton.addActionListener { e ->
            val popup = JPopupMenu()
            popup.add(JMenuItem("Export STL").apply {
                addActionListener { exportSTL() }
            })
            popup.add(JMenuItem("Export 3MF (with colors)").apply {
                addActionListener { export3MF() }
            })
            popup.show(exportButton, 0, exportButton.height)
        }

        autoRenderCheckbox.addActionListener {
            settings.autoRenderOnSave = autoRenderCheckbox.isSelected
            if (autoRenderCheckbox.isSelected) {
                renderForCurrentMode()
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
                                renderForCurrentMode()
                            }
                        }
                    }
                }
            }
        )
    }

    private fun renderForCurrentMode() {
        when (currentPreviewMode) {
            PreviewMode.SOLID_3D -> {
                (viewer as? STLViewerPanel)?.setWireframe(false)
                renderFile()
            }
            PreviewMode.WIREFRAME -> {
                if (viewer is STLViewerPanel) {
                    viewer.setWireframe(true)
                    renderFile()
                } else {
                    // JOGL viewer doesn't support wireframe, fall back to solid 3D
                    currentPreviewMode = PreviewMode.SOLID_3D
                    previewModeButton.text = "Preview: ${PreviewMode.SOLID_3D.displayName} ▼"
                    renderFile()
                }
            }
            PreviewMode.OPENSCAD_DEBUG -> {
                if (viewer is STLViewerPanel) {
                    renderDebugPreview()
                } else {
                    // JOGL viewer doesn't support debug preview, fall back to solid 3D
                    currentPreviewMode = PreviewMode.SOLID_3D
                    previewModeButton.text = "Preview: ${PreviewMode.SOLID_3D.displayName} ▼"
                    renderFile()
                }
            }
        }
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
                // Try 3MF first (preserves colors from color() statements)
                logger.info("Starting 3MF render for ${file.name}")
                val threemfPath = renderer.renderTo3MF(file)

                if (threemfPath != null) {
                    val coloredModel = threemfParser.parse(threemfPath)

                    if (coloredModel != null && coloredModel.triangles.isNotEmpty()) {
                        logger.info("3MF parsed: ${coloredModel.triangles.size} triangles")
                        SwingUtilities.invokeLater {
                            when (viewer) {
                                is STLViewer3D -> viewer.setColoredModel(coloredModel)
                                is STLViewerPanel -> viewer.setColoredModel(coloredModel)
                            }
                            updateStatus("✓ Rendered with colors (${coloredModel.triangles.size} triangles)")
                        }
                        return@executeOnPooledThread
                    }
                }

                // Fall back to STL if 3MF fails
                logger.info("Falling back to STL render")
                val stlPath = renderer.renderToSTL(file)

                if (stlPath != null) {
                    val model = stlParser.parse(stlPath)

                    SwingUtilities.invokeLater {
                        if (model != null) {
                            when (viewer) {
                                is STLViewer3D -> viewer.setModel(model)
                                is STLViewerPanel -> viewer.setModel(model)
                            }
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

    private fun setPreviewMode(mode: PreviewMode) {
        currentPreviewMode = mode
        previewModeButton.text = "Preview: ${mode.displayName} ▼"
        renderForCurrentMode()
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

        val viewerPanel = viewer as? STLViewerPanel

        isRendering = true
        renderButton.isEnabled = false
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
                    renderButton.isEnabled = true
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

    private fun export3MF() {
        if (!file.isValid) {
            updateStatus("✗ File is not valid")
            return
        }

        // Show file save dialog
        val descriptor = FileSaverDescriptor("Export to 3MF", "Choose output 3MF file (preserves colors)", "3mf")
        val saveDialog = FileChooserFactory.getInstance().createSaveFileDialog(descriptor, project)
        val fileWrapper = saveDialog.save(file.parent, file.nameWithoutExtension + ".3mf") ?: return

        val outputFile = fileWrapper.file

        updateStatus("Exporting to ${outputFile.name}...")

        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val threemfPath = renderer.renderTo3MF(file, outputFile.toPath())

                SwingUtilities.invokeLater {
                    if (threemfPath != null) {
                        updateStatus("✓ Exported to ${outputFile.name}")
                        NotificationGroupManager.getInstance()
                            .getNotificationGroup("OpenSCAD")
                            .createNotification(
                                "Export Successful",
                                "Exported 3MF (with colors) to ${outputFile.absolutePath}",
                                NotificationType.INFORMATION
                            )
                            .notify(project)
                    } else {
                        updateStatus("✗ Export failed")
                        NotificationGroupManager.getInstance()
                            .getNotificationGroup("OpenSCAD")
                            .createNotification(
                                "Export Failed",
                                "Failed to export 3MF file",
                                NotificationType.ERROR
                            )
                            .notify(project)
                    }
                }
            } catch (e: Exception) {
                logger.error("Error exporting 3MF", e)
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
