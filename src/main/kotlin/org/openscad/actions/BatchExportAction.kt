package org.openscad.actions

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.vfs.VirtualFile
import org.openscad.file.OpenSCADFileType
import org.openscad.preview.OpenSCADRenderer
import java.io.File

/**
 * Action to batch export multiple OpenSCAD files
 */
class BatchExportAction : AnAction() {
    
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        
        // Select output directory
        val descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor()
        descriptor.title = "Select Output Directory for STL Files"
        val outputDir = FileChooser.chooseFile(descriptor, project, null) ?: return
        
        // Get all .scad files in project
        val scadFiles = mutableListOf<VirtualFile>()
        project.baseDir?.let { collectScadFiles(it, scadFiles) }
        
        if (scadFiles.isEmpty()) {
            NotificationGroupManager.getInstance()
                .getNotificationGroup("OpenSCAD")
                .createNotification(
                    "No Files Found",
                    "No OpenSCAD files found in project",
                    NotificationType.WARNING
                )
                .notify(project)
            return
        }
        
        // Batch export
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Batch Exporting STL Files", true) {
            override fun run(indicator: ProgressIndicator) {
                val renderer = OpenSCADRenderer(project)
                var successCount = 0
                var failCount = 0
                
                scadFiles.forEachIndexed { index, file ->
                    if (indicator.isCanceled) return
                    
                    indicator.fraction = index.toDouble() / scadFiles.size
                    indicator.text = "Exporting ${file.name} (${index + 1}/${scadFiles.size})"
                    
                    val outputFile = File(outputDir.path, file.nameWithoutExtension + ".stl")
                    val result = renderer.renderToSTL(file, outputFile.toPath())
                    
                    if (result != null) {
                        successCount++
                    } else {
                        failCount++
                    }
                }
                
                NotificationGroupManager.getInstance()
                    .getNotificationGroup("OpenSCAD")
                    .createNotification(
                        "Batch Export Complete",
                        "Exported $successCount files successfully, $failCount failed",
                        if (failCount == 0) NotificationType.INFORMATION else NotificationType.WARNING
                    )
                    .notify(project)
            }
        })
    }
    
    private fun collectScadFiles(dir: VirtualFile, result: MutableList<VirtualFile>) {
        dir.children.forEach { child ->
            if (child.isDirectory) {
                collectScadFiles(child, result)
            } else if (child.fileType is OpenSCADFileType) {
                result.add(child)
            }
        }
    }
    
    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null
    }
}
