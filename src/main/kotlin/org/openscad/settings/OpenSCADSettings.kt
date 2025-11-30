package org.openscad.settings

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil

/**
 * Persistent settings for OpenSCAD plugin
 */
@State(
    name = "OpenSCADSettings",
    storages = [Storage("openscad.xml")]
)
@Service(Service.Level.PROJECT)
class OpenSCADSettings : PersistentStateComponent<OpenSCADSettings> {
    
    var openscadPath: String = ""
    var autoRenderOnSave: Boolean = false
    var renderTimeout: Int = 30 // seconds
    
    // Rendering options
    var useFullRender: Boolean = false // Use --render instead of preview
    var autoCenter: Boolean = true // Use --autocenter
    var viewAll: Boolean = true // Use --viewall
    
    // Library paths
    var libraryPaths: MutableList<String> = mutableListOf()
    
    override fun getState(): OpenSCADSettings = this
    
    override fun loadState(state: OpenSCADSettings) {
        XmlSerializerUtil.copyBean(state, this)
    }
    
    companion object {
        fun getInstance(project: Project): OpenSCADSettings {
            return project.service()
        }
    }
}
