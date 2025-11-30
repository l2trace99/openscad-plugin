package org.openscad.file

import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.util.IconLoader
import org.openscad.OpenSCADLanguage
import javax.swing.Icon

class OpenSCADFileType : LanguageFileType(OpenSCADLanguage.INSTANCE) {
    override fun getName(): String = "OpenSCAD"
    override fun getDescription(): String = "OpenSCAD script"
    override fun getDefaultExtension(): String = "scad"
    override fun getIcon(): Icon = OpenSCAD_ICON

    companion object {
        @JvmStatic
        val INSTANCE = OpenSCADFileType()
        private val OpenSCAD_ICON = IconLoader.getIcon("/icons/openscad.svg", OpenSCADFileType::class.java)
    }
}
