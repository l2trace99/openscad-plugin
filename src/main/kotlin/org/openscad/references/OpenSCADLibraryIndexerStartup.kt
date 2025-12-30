package org.openscad.references

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

/**
 * Startup activity that triggers library indexing when project opens
 */
class OpenSCADLibraryIndexerStartup : ProjectActivity {
    
    private val logger = Logger.getInstance(OpenSCADLibraryIndexerStartup::class.java)
    
    override suspend fun execute(project: Project) {
        logger.info("Starting OpenSCAD library indexing for project: ${project.name}")
        
        // Run indexing in a background task with progress
        ProgressManager.getInstance().run(object : Task.Backgroundable(
            project,
            "Indexing OpenSCAD Libraries",
            true
        ) {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true
                indicator.text = "Scanning OpenSCAD library paths..."
                
                try {
                    val indexer = OpenSCADLibraryIndexer.getInstance(project)
                    indexer.reindex()
                    
                    val symbolCount = indexer.getLibrarySymbols().size
                    logger.info("OpenSCAD library indexing complete: $symbolCount symbols indexed")
                } catch (e: Exception) {
                    logger.warn("Error during OpenSCAD library indexing", e)
                }
            }
        })
    }
}
