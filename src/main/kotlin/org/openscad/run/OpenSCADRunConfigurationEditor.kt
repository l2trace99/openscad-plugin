package org.openscad.run

import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * UI editor for OpenSCAD run configuration
 */
class OpenSCADRunConfigurationEditor : SettingsEditor<OpenSCADRunConfiguration>() {
    
    private val scriptPathField = TextFieldWithBrowseButton()
    private val outputPathField = TextFieldWithBrowseButton()
    private val openscadPathField = TextFieldWithBrowseButton()
    private val useRenderCheckbox = JBCheckBox("Use full render (--render)")
    private val autoCenterCheckbox = JBCheckBox("Auto-center (--autocenter)")
    private val viewAllCheckbox = JBCheckBox("View all (--viewall)")
    private val enableAnimationCheckbox = JBCheckBox("Enable animation")
    private val animationStepsField = JBTextField()
    private val cameraSettingsField = JBTextField()
    private val imageSizeField = JBTextField()
    private val customParametersField = JBTextField()
    
    init {
        val scadDescriptor = FileChooserDescriptor(true, false, false, false, false, false)
            .withTitle("Select OpenSCAD File")
            .withFileFilter { it.extension == "scad" }
        scriptPathField.addActionListener {
            val file = FileChooser.chooseFile(scadDescriptor, null, null)
            if (file != null) {
                scriptPathField.text = file.path
            }
        }
        
        val outputDescriptor = FileChooserDescriptor(true, false, false, false, false, false)
            .withTitle("Select Output File")
        outputPathField.addActionListener {
            val file = FileChooser.chooseFile(outputDescriptor, null, null)
            if (file != null) {
                outputPathField.text = file.path
            }
        }
        
        val executableDescriptor = FileChooserDescriptor(true, false, false, false, false, false)
            .withTitle("Select OpenSCAD Executable")
        openscadPathField.addActionListener {
            val file = FileChooser.chooseFile(executableDescriptor, null, null)
            if (file != null) {
                openscadPathField.text = file.path
            }
        }
        
        animationStepsField.text = "20"
    }
    
    override fun createEditor(): JComponent {
        return FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("OpenSCAD file:"), scriptPathField, 1, false)
            .addLabeledComponent(JBLabel("Output file:"), outputPathField, 1, false)
            .addTooltip("Leave empty to just open in OpenSCAD GUI")
            .addLabeledComponent(JBLabel("OpenSCAD executable:"), openscadPathField, 1, false)
            .addTooltip("Leave empty to use system default")
            .addSeparator()
            .addComponent(useRenderCheckbox)
            .addComponent(autoCenterCheckbox)
            .addComponent(viewAllCheckbox)
            .addSeparator()
            .addComponent(enableAnimationCheckbox)
            .addLabeledComponent(JBLabel("Animation steps:"), animationStepsField, 1, false)
            .addTooltip("Number of frames for animation (uses \$t variable)")
            .addSeparator()
            .addLabeledComponent(JBLabel("Camera (x,y,z,rx,ry,rz,d):"), cameraSettingsField, 1, false)
            .addTooltip("Example: 0,0,0,55,0,25,500")
            .addLabeledComponent(JBLabel("Image size (width,height):"), imageSizeField, 1, false)
            .addTooltip("Example: 1920,1080 (for PNG export)")
            .addSeparator()
            .addLabeledComponent(JBLabel("Custom parameters:"), customParametersField, 1, false)
            .addComponentFillVertically(JPanel(), 0)
            .panel
    }
    
    override fun resetEditorFrom(configuration: OpenSCADRunConfiguration) {
        val options = configuration.getOptions()
        scriptPathField.text = options.scriptPath
        outputPathField.text = options.outputPath
        openscadPathField.text = options.openscadPath
        useRenderCheckbox.isSelected = options.useRender
        autoCenterCheckbox.isSelected = options.autoCenter
        viewAllCheckbox.isSelected = options.viewAll
        enableAnimationCheckbox.isSelected = options.enableAnimation
        animationStepsField.text = options.animationSteps.toString()
        cameraSettingsField.text = options.cameraSettings
        imageSizeField.text = options.imageSize
        customParametersField.text = options.customParameters
    }
    
    override fun applyEditorTo(configuration: OpenSCADRunConfiguration) {
        val options = configuration.getOptions()
        options.scriptPath = scriptPathField.text
        options.outputPath = outputPathField.text
        options.openscadPath = openscadPathField.text
        options.useRender = useRenderCheckbox.isSelected
        options.autoCenter = autoCenterCheckbox.isSelected
        options.viewAll = viewAllCheckbox.isSelected
        options.enableAnimation = enableAnimationCheckbox.isSelected
        options.animationSteps = animationStepsField.text.toIntOrNull() ?: 20
        options.cameraSettings = cameraSettingsField.text
        options.imageSize = imageSizeField.text
        options.customParameters = customParametersField.text
    }
}
