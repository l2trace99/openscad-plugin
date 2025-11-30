package org.openscad.actions

import com.intellij.ide.actions.CreateFileFromTemplateAction
import com.intellij.ide.actions.CreateFileFromTemplateDialog
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import org.openscad.file.OpenSCADFileType

/**
 * Action to create a new OpenSCAD file from the File → New menu
 */
class NewOpenSCADFileAction : CreateFileFromTemplateAction(
    "OpenSCAD File",
    "Create new OpenSCAD file",
    OpenSCADFileType.INSTANCE.icon
), DumbAware {
    
    override fun buildDialog(
        project: Project,
        directory: PsiDirectory,
        builder: CreateFileFromTemplateDialog.Builder
    ) {
        val icon = OpenSCADFileType.INSTANCE.icon
        builder
            .setTitle("New OpenSCAD File")
            .addKind("OpenSCAD File", icon, "OpenSCAD File")
    }
    
    override fun getActionName(directory: PsiDirectory?, newName: String, templateName: String?): String {
        return "Create OpenSCAD File $newName"
    }
}
