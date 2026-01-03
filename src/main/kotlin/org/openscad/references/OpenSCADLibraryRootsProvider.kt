package org.openscad.references

import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.AdditionalLibraryRootsProvider
import com.intellij.openapi.roots.SyntheticLibrary
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import org.openscad.settings.OpenSCADSettings
import java.io.File
import javax.swing.Icon

/**
 * Provides OpenSCAD library directories as additional library roots
 * This allows PSI parsing to work for files in library directories
 */
class OpenSCADLibraryRootsProvider : AdditionalLibraryRootsProvider() {
    
    override fun getAdditionalProjectLibraries(project: Project): Collection<SyntheticLibrary> {
        val libraries = mutableListOf<SyntheticLibrary>()
        
        val libraryPaths = getLibraryPaths(project)
        
        for (libPath in libraryPaths) {
            val virtualFile = LocalFileSystem.getInstance().findFileByPath(libPath)
            if (virtualFile != null && virtualFile.isDirectory) {
                libraries.add(OpenSCADLibrary(libPath, virtualFile))
            }
        }
        
        return libraries
    }
    
    override fun getRootsToWatch(project: Project): Collection<VirtualFile> {
        val roots = mutableListOf<VirtualFile>()
        
        val libraryPaths = getLibraryPaths(project)
        
        for (libPath in libraryPaths) {
            val virtualFile = LocalFileSystem.getInstance().findFileByPath(libPath)
            if (virtualFile != null) {
                roots.add(virtualFile)
            }
        }
        
        return roots
    }
    
    private fun getLibraryPaths(project: Project): List<String> {
        val paths = mutableListOf<String>()
        
        // Add configured library paths from settings
        val settings = OpenSCADSettings.getInstance(project)
        paths.addAll(settings.libraryPaths.filter { File(it).exists() })
        
        // Add project-relative lib directory if it exists
        project.basePath?.let { basePath ->
            val projectLibDir = File(basePath, "lib")
            if (projectLibDir.exists() && projectLibDir.isDirectory) {
                paths.add(projectLibDir.absolutePath)
            }
        }
        
        // Add common OpenSCAD library locations based on OS
        paths.addAll(org.openscad.util.OpenSCADPathUtils.getExistingLibraryPaths())
        
        return paths.distinct()
    }
    
    /**
     * Represents an OpenSCAD library as a synthetic library
     */
    private class OpenSCADLibrary(
        private val name: String,
        private val root: VirtualFile
    ) : SyntheticLibrary(), ItemPresentation {
        
        override fun getSourceRoots(): Collection<VirtualFile> = listOf(root)
        
        override fun getPresentableText(): String = "OpenSCAD: ${File(name).name}"
        
        override fun getIcon(unused: Boolean): Icon? = null
        
        override fun getLocationString(): String = name
        
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is OpenSCADLibrary) return false
            return name == other.name
        }
        
        override fun hashCode(): Int = name.hashCode()
    }
}
