package org.openscad.run

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.project.Project
import org.openscad.file.OpenSCADFileType
import javax.swing.Icon

/**
 * Configuration type for OpenSCAD run configurations
 */
class OpenSCADConfigurationType : ConfigurationType {
    override fun getDisplayName(): String = "OpenSCAD"
    
    override fun getConfigurationTypeDescription(): String = "OpenSCAD run configuration"
    
    override fun getIcon(): Icon = OpenSCADFileType.INSTANCE.icon
    
    override fun getId(): String = "OpenSCADRunConfiguration"
    
    override fun getConfigurationFactories(): Array<ConfigurationFactory> {
        return arrayOf(OpenSCADConfigurationFactory(this))
    }
}

/**
 * Factory for creating OpenSCAD run configurations
 */
class OpenSCADConfigurationFactory(type: ConfigurationType) : ConfigurationFactory(type) {
    override fun getId(): String = "OpenSCAD"
    
    override fun createTemplateConfiguration(project: Project): RunConfiguration {
        return OpenSCADRunConfiguration(project, this, "OpenSCAD")
    }
    
    override fun getOptionsClass(): Class<out OpenSCADRunConfigurationOptions> {
        return OpenSCADRunConfigurationOptions::class.java
    }
}
