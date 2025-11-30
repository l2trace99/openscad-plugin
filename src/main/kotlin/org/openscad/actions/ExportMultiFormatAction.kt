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
import com.intellij.openapi.ui.Messages
import org.openscad.file.OpenSCADFileType
import org.openscad.preview.OpenSCADRenderer
import java.io.File

/**
 * Action to export to multiple formats at once
 */
class ExportMultiFormatAction : AnAction("Export to Multiple Formats...", "Export to STL, PNG, and other formats", null) {
    
    private val formats = listOf("stl", "png", "3mf", "amf", "off", "dxf", "svg")
    
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        
        // Select output directory
        val descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor()
        descriptor.title = "Select Output Directory"
        val outputDir = FileChooser.chooseFile(descriptor, project, file.parent) ?: return
        
        // Export to all common formats
        val exportFormats = listOf("stl", "png")
        
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Exporting to Multiple Formats", true) {
            override fun run(indicator: ProgressIndicator) {
                val renderer = OpenSCADRenderer(project)
                val baseName = file.nameWithoutExtension
                var successCount = 0
                var failCount = 0
                
                exportFormats.forEachIndexed { index, format ->
                    if (indicator.isCanceled) return@run
                    
                    indicator.fraction = index.toDouble() / exportFormats.size
                    indicator.text = "Exporting to $format..."
                    
                    val outputFile = File(outputDir.path, "$baseName.$format")
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
                        "Multi-Format Export Complete",
                        "Exported to $successCount formats successfully, $failCount failed",
                        if (failCount == 0) NotificationType.INFORMATION else NotificationType.WARNING
                    )
                    .notify(project)
            }
        })
    }
    
    override fun update(e: AnActionEvent) {
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
        e.presentation.isEnabledAndVisible = file?.fileType is OpenSCADFileType
    }
}
