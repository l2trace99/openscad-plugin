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

    // Viewer settings
    var useHardwareAcceleration: Boolean = false // Use JOGL hardware-accelerated viewer (experimental)
    var renderBackend: String = "" // "manifold", "cgal", or "" (auto/default)
    
    // Preview grid settings
    var showGrid: Boolean = true // Show grid in 3D preview
    var gridSize: Float = 250.0f // Grid size in mm (250mm x 250mm default)
    var gridSpacing: Float = 10.0f // Grid line spacing in mm
    
    // Library paths
    var libraryPaths: MutableList<String> = mutableListOf()
    
    // Custom temp directory (empty = use system default, with Linux Flatpak-friendly fallback)
    var customTempDirectory: String = ""
    
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
