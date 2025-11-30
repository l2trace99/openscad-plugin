package org.openscad.editor

import com.intellij.openapi.fileEditor.*
import com.intellij.openapi.fileEditor.impl.text.PsiAwareTextEditorProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

/**
 * Split editor provider for OpenSCAD files
 * Shows code on left, 3D preview on right (like Markdown preview)
 */
class OpenSCADSplitEditorProvider : FileEditorProvider, DumbAware {
    
    companion object {
        private const val EDITOR_TYPE_ID = "openscad-split-editor"
    }
    
    override fun accept(project: Project, file: VirtualFile): Boolean {
        return file.extension == "scad"
    }
    
    override fun createEditor(project: Project, file: VirtualFile): FileEditor {
        return OpenSCADSplitEditor(project, file)
    }
    
    override fun getEditorTypeId(): String = EDITOR_TYPE_ID
    
    override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.HIDE_DEFAULT_EDITOR
}

/**
 * Split editor combining text editor and preview
 */
class OpenSCADSplitEditor(
    project: Project,
    file: VirtualFile
) : TextEditorWithPreview(
    PsiAwareTextEditorProvider().createEditor(project, file) as TextEditor,
    OpenSCADPreviewFileEditor(project, file),
    "OpenSCAD Editor",
    Layout.SHOW_EDITOR_AND_PREVIEW
) {
    
    override fun getName(): String = "OpenSCAD"
}
