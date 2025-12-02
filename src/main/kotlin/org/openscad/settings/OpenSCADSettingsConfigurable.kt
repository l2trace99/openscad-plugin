package org.openscad.settings

import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import org.openscad.preview.OpenSCADRenderer
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * Settings UI for OpenSCAD plugin
 */
class OpenSCADSettingsConfigurable(private val project: Project) : Configurable {
    
    private val openscadPathField = TextFieldWithBrowseButton()
    private val autoRenderCheckbox = JBCheckBox("Auto-render on file save")
    private val timeoutField = JBTextField()
    private val detectedPathLabel = JBLabel()
    
    // Rendering options
    private val useFullRenderCheckbox = JBCheckBox("Use full render (slower, more accurate)")
    private val autoCenterCheckbox = JBCheckBox("Auto-center model")
    private val viewAllCheckbox = JBCheckBox("Auto-fit model to view")
    
    // Grid settings
    private val showGridCheckbox = JBCheckBox("Show grid in preview")
    private val gridSizeField = JBTextField()
    private val gridSpacingField = JBTextField()
    
    // Library paths
    private val libraryPathsField = JBTextArea()
    private val libraryPathsScrollPane = JBScrollPane(libraryPathsField)
    
    private var modified = false
    
    init {
        // Setup file chooser for OpenSCAD path
        val descriptor = FileChooserDescriptor(true, false, false, false, false, false)
            .withTitle("Select OpenSCAD Executable")
            .withDescription("Choose the OpenSCAD executable file")
        openscadPathField.addActionListener {
            val file = FileChooser.chooseFile(descriptor, project, null)
            if (file != null) {
                openscadPathField.text = file.path
            }
        }
        
        // Setup library paths text area
        libraryPathsField.rows = 5
        libraryPathsField.lineWrap = false
        libraryPathsScrollPane.preferredSize = java.awt.Dimension(400, 100)
        
        openscadPathField.textField.document.addDocumentListener(object : javax.swing.event.DocumentListener {
            override fun insertUpdate(e: javax.swing.event.DocumentEvent?) { modified = true }
            override fun removeUpdate(e: javax.swing.event.DocumentEvent?) { modified = true }
            override fun changedUpdate(e: javax.swing.event.DocumentEvent?) { modified = true }
        })
        
        autoRenderCheckbox.addActionListener { modified = true }
        timeoutField.document.addDocumentListener(object : javax.swing.event.DocumentListener {
            override fun insertUpdate(e: javax.swing.event.DocumentEvent?) { modified = true }
            override fun removeUpdate(e: javax.swing.event.DocumentEvent?) { modified = true }
            override fun changedUpdate(e: javax.swing.event.DocumentEvent?) { modified = true }
        })
        
        // Detect OpenSCAD installation
        detectOpenSCAD()
    }
    
    private fun detectOpenSCAD() {
        val renderer = OpenSCADRenderer(project)
        val detectedPath = renderer.findOpenSCADExecutable()
        if (detectedPath != null) {
            detectedPathLabel.text = "✓ Auto-detected: $detectedPath"
        } else {
            detectedPathLabel.text = "⚠ OpenSCAD not found automatically"
        }
    }
    
    override fun getDisplayName(): String = "OpenSCAD"
    
    override fun createComponent(): JComponent {
        val panel = FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("OpenSCAD executable path:"), openscadPathField, 1, false)
            .addComponent(detectedPathLabel)
            .addTooltip("Leave empty to use auto-detected path")
            .addSeparator()
            .addComponent(autoRenderCheckbox)
            .addLabeledComponent(JBLabel("Render timeout (seconds):"), timeoutField, 1, false)
            .addSeparator()
            .addLabeledComponent(JBLabel("Rendering Options:"), JPanel(), 1, false)
            .addComponent(useFullRenderCheckbox)
            .addTooltip("Use CGAL rendering instead of OpenCSG preview (slower but more accurate)")
            .addComponent(autoCenterCheckbox)
            .addTooltip("Automatically center the model in the view")
            .addComponent(viewAllCheckbox)
            .addTooltip("Automatically adjust camera to fit the entire model")
            .addSeparator()
            .addLabeledComponent(JBLabel("Grid Settings:"), JPanel(), 1, false)
            .addComponent(showGridCheckbox)
            .addTooltip("Show a horizontal grid centered on the origin")
            .addLabeledComponent(JBLabel("Grid size (mm):"), gridSizeField, 1, false)
            .addTooltip("Total grid size in millimeters (e.g., 250 for 250mm x 250mm)")
            .addLabeledComponent(JBLabel("Grid spacing (mm):"), gridSpacingField, 1, false)
            .addTooltip("Distance between grid lines in millimeters")
            .addSeparator()
            .addLabeledComponent(JBLabel("Library Paths (one per line):"), libraryPathsScrollPane, 1, true)
            .addTooltip("Additional directories to search for OpenSCAD libraries (added to OPENSCADPATH)")
            .addComponentFillVertically(JPanel(), 0)
            .panel
        
        val wrapper = JPanel(BorderLayout())
        wrapper.add(panel, BorderLayout.NORTH)
        
        return wrapper
    }
    
    override fun isModified(): Boolean {
        val settings = OpenSCADSettings.getInstance(project)
        val currentLibPaths = libraryPathsField.text.lines().filter { it.isNotBlank() }
        return openscadPathField.text != settings.openscadPath ||
                autoRenderCheckbox.isSelected != settings.autoRenderOnSave ||
                timeoutField.text != settings.renderTimeout.toString() ||
                useFullRenderCheckbox.isSelected != settings.useFullRender ||
                autoCenterCheckbox.isSelected != settings.autoCenter ||
                viewAllCheckbox.isSelected != settings.viewAll ||
                showGridCheckbox.isSelected != settings.showGrid ||
                gridSizeField.text != settings.gridSize.toString() ||
                gridSpacingField.text != settings.gridSpacing.toString() ||
                currentLibPaths != settings.libraryPaths
    }
    
    override fun apply() {
        val settings = OpenSCADSettings.getInstance(project)
        settings.openscadPath = openscadPathField.text
        settings.autoRenderOnSave = autoRenderCheckbox.isSelected
        settings.renderTimeout = timeoutField.text.toIntOrNull() ?: 30
        settings.useFullRender = useFullRenderCheckbox.isSelected
        settings.autoCenter = autoCenterCheckbox.isSelected
        settings.viewAll = viewAllCheckbox.isSelected
        settings.showGrid = showGridCheckbox.isSelected
        settings.gridSize = gridSizeField.text.toFloatOrNull() ?: 250.0f
        settings.gridSpacing = gridSpacingField.text.toFloatOrNull() ?: 10.0f
        settings.libraryPaths = libraryPathsField.text.lines()
            .filter { it.isNotBlank() }
            .map { it.trim() }
            .toMutableList()
        modified = false
    }
    
    override fun reset() {
        val settings = OpenSCADSettings.getInstance(project)
        openscadPathField.text = settings.openscadPath
        autoRenderCheckbox.isSelected = settings.autoRenderOnSave
        timeoutField.text = settings.renderTimeout.toString()
        useFullRenderCheckbox.isSelected = settings.useFullRender
        autoCenterCheckbox.isSelected = settings.autoCenter
        viewAllCheckbox.isSelected = settings.viewAll
        showGridCheckbox.isSelected = settings.showGrid
        gridSizeField.text = settings.gridSize.toString()
        gridSpacingField.text = settings.gridSpacing.toString()
        libraryPathsField.text = settings.libraryPaths.joinToString("\n")
        modified = false
    }
}
