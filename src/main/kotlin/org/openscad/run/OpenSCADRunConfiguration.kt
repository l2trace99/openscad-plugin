package org.openscad.run

import com.intellij.execution.Executor
import com.intellij.execution.configurations.*
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessHandlerFactory
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jdom.Element
import org.openscad.file.OpenSCADFileType
import javax.swing.*

/**
 * Run configuration for OpenSCAD files
 * Allows running OpenSCAD with various export options
 */
class OpenSCADRunConfiguration(
    project: Project,
    factory: ConfigurationFactory,
    name: String
) : RunConfigurationBase<OpenSCADRunConfigurationOptions>(project, factory, name) {
    
    public override fun getOptions(): OpenSCADRunConfigurationOptions {
        return super.getOptions() as OpenSCADRunConfigurationOptions
    }
    
    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
        return OpenSCADRunConfigurationEditor()
    }
    
    override fun checkConfiguration() {
        if (options.scriptPath.isEmpty()) {
            throw RuntimeConfigurationException("OpenSCAD file not specified")
        }
    }
    
    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState {
        return object : CommandLineState(environment) {
            override fun startProcess(): ProcessHandler {
                val commandLine = GeneralCommandLine()
                    .withExePath(options.openscadPath.ifEmpty { "openscad" })
                    .withWorkDirectory(project.basePath)
                
                // Add parameters based on configuration
                if (options.outputPath.isNotEmpty()) {
                    commandLine.addParameter("-o")
                    commandLine.addParameter(options.outputPath)
                }
                
                if (options.useRender) {
                    commandLine.addParameter("--render")
                }
                
                if (options.autoCenter) {
                    commandLine.addParameter("--autocenter")
                }
                
                if (options.viewAll) {
                    commandLine.addParameter("--viewall")
                }
                
                // Animation support
                if (options.enableAnimation) {
                    commandLine.addParameter("--animate")
                    commandLine.addParameter(options.animationSteps.toString())
                }
                
                // Camera settings
                if (options.cameraSettings.isNotEmpty()) {
                    commandLine.addParameter("--camera")
                    commandLine.addParameter(options.cameraSettings)
                }
                
                // Image size for PNG export
                if (options.imageSize.isNotEmpty()) {
                    commandLine.addParameter("--imgsize")
                    commandLine.addParameter(options.imageSize)
                }
                
                // Custom parameters
                if (options.customParameters.isNotEmpty()) {
                    options.customParameters.split(" ").forEach {
                        commandLine.addParameter(it)
                    }
                }
                
                // Input file
                commandLine.addParameter(options.scriptPath)
                
                val processHandler = ProcessHandlerFactory.getInstance()
                    .createColoredProcessHandler(commandLine)
                ProcessTerminatedListener.attach(processHandler)
                return processHandler
            }
        }
    }
}

/**
 * Run configuration options
 */
class OpenSCADRunConfigurationOptions : RunConfigurationOptions() {
    private val scriptPathOption = string("").provideDelegate(this, "scriptPath")
    private val outputPathOption = string("").provideDelegate(this, "outputPath")
    private val openscadPathOption = string("").provideDelegate(this, "openscadPath")
    private val useRenderOption = property(false).provideDelegate(this, "useRender")
    private val autoCenterOption = property(true).provideDelegate(this, "autoCenter")
    private val viewAllOption = property(true).provideDelegate(this, "viewAll")
    private val enableAnimationOption = property(false).provideDelegate(this, "enableAnimation")
    private val animationStepsOption = property(20).provideDelegate(this, "animationSteps")
    private val cameraSettingsOption = string("").provideDelegate(this, "cameraSettings")
    private val imageSizeOption = string("").provideDelegate(this, "imageSize")
    private val customParametersOption = string("").provideDelegate(this, "customParameters")
    
    var scriptPath: String
        get() = scriptPathOption.getValue(this) ?: ""
        set(value) { scriptPathOption.setValue(this, value) }
    
    var outputPath: String
        get() = outputPathOption.getValue(this) ?: ""
        set(value) { outputPathOption.setValue(this, value) }
    
    var openscadPath: String
        get() = openscadPathOption.getValue(this) ?: ""
        set(value) { openscadPathOption.setValue(this, value) }
    
    var useRender: Boolean
        get() = useRenderOption.getValue(this)
        set(value) { useRenderOption.setValue(this, value) }
    
    var autoCenter: Boolean
        get() = autoCenterOption.getValue(this)
        set(value) { autoCenterOption.setValue(this, value) }
    
    var viewAll: Boolean
        get() = viewAllOption.getValue(this)
        set(value) { viewAllOption.setValue(this, value) }
    
    var enableAnimation: Boolean
        get() = enableAnimationOption.getValue(this)
        set(value) { enableAnimationOption.setValue(this, value) }
    
    var animationSteps: Int
        get() = animationStepsOption.getValue(this)
        set(value) { animationStepsOption.setValue(this, value) }
    
    var cameraSettings: String
        get() = cameraSettingsOption.getValue(this) ?: ""
        set(value) { cameraSettingsOption.setValue(this, value) }
    
    var imageSize: String
        get() = imageSizeOption.getValue(this) ?: ""
        set(value) { imageSizeOption.setValue(this, value) }
    
    var customParameters: String
        get() = customParametersOption.getValue(this) ?: ""
        set(value) { customParametersOption.setValue(this, value) }
}
