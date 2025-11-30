package org.openscad.actions

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import org.openscad.file.OpenSCADFileType
import org.openscad.preview.OpenSCADRenderer
import java.io.File

/**
 * Action to export current OpenSCAD file to STL
 */
class ExportSTLAction : AnAction("Export to STL...", "Export OpenSCAD file to STL format", null) {
    
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        
        // Show file chooser
        val descriptor = FileSaverDescriptor("Export to STL", "Choose output STL file", "stl")
        val saveDialog = FileChooserFactory.getInstance().createSaveFileDialog(descriptor, project)
        val fileWrapper = saveDialog.save(file.parent, file.nameWithoutExtension + ".stl") ?: return
        
        val outputFile = fileWrapper.file
        
        // Render in background
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Exporting to STL", true) {
            override fun run(indicator: ProgressIndicator) {
                indicator.text = "Rendering ${file.name}..."
                
                val renderer = OpenSCADRenderer(project)
                val stlPath = renderer.renderToSTL(file, outputFile.toPath())
                
                if (stlPath != null) {
                    NotificationGroupManager.getInstance()
                        .getNotificationGroup("OpenSCAD")
                        .createNotification(
                            "Export Successful",
                            "Exported to ${outputFile.absolutePath}",
                            NotificationType.INFORMATION
                        )
                        .notify(project)
                } else {
                    NotificationGroupManager.getInstance()
                        .getNotificationGroup("OpenSCAD")
                        .createNotification(
                            "Export Failed",
                            "Failed to export STL file",
                            NotificationType.ERROR
                        )
                        .notify(project)
                }
            }
        })
    }
    
    override fun update(e: AnActionEvent) {
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
        e.presentation.isEnabledAndVisible = file?.fileType is OpenSCADFileType
    }
}
