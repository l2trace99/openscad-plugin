package org.openscad.references

import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileDocumentManagerListener
import com.intellij.openapi.project.ProjectManager
import org.openscad.file.OpenSCADFileType

/**
 * Listener that triggers library re-indexing when OpenSCAD library files are saved.
 * Only triggers reindex if the saved file is within a configured library path.
 */
class OpenSCADFileSaveListener : FileDocumentManagerListener {
    
    override fun beforeDocumentSaving(document: Document) {
        val virtualFile = FileDocumentManager.getInstance().getFile(document) ?: return
        
        // Only trigger for OpenSCAD files
        if (virtualFile.extension != "scad") return
        
        val filePath = virtualFile.path
        
        // Re-index only for projects where this file is in a library path
        ProjectManager.getInstance().openProjects.forEach { project ->
            if (!project.isDisposed) {
                val indexer = OpenSCADLibraryIndexer.getInstance(project)
                if (indexer.isInLibraryPath(filePath)) {
                    indexer.reindex()
                }
            }
        }
    }
}
