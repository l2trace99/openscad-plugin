package org.openscad.references

import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileDocumentManagerListener
import com.intellij.openapi.project.ProjectManager
import org.openscad.file.OpenSCADFileType

/**
 * Listener that triggers library re-indexing when OpenSCAD files are saved
 */
class OpenSCADFileSaveListener : FileDocumentManagerListener {
    
    override fun beforeDocumentSaving(document: Document) {
        val virtualFile = FileDocumentManager.getInstance().getFile(document) ?: return
        
        // Only trigger for OpenSCAD files
        if (virtualFile.extension != "scad") return
        
        // Re-index for all open projects
        ProjectManager.getInstance().openProjects.forEach { project ->
            if (!project.isDisposed) {
                OpenSCADLibraryIndexer.getInstance(project).reindex()
            }
        }
    }
}
