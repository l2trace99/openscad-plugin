package org.openscad.preview

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ProcessOutput
import com.intellij.execution.util.ExecUtil
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists

/**
 * Handles rendering OpenSCAD files to STL using the OpenSCAD command-line tool
 */
class OpenSCADRenderer(private val project: Project) {
    
    private val logger = Logger.getInstance(OpenSCADRenderer::class.java)
    
    /**
     * Renders an OpenSCAD file to STL format
     * @param scadFile The OpenSCAD source file
     * @param outputPath Optional output path for the STL file. If null, uses temp directory
     * @return Path to the generated STL file, or null if rendering failed
     */
    fun renderToSTL(scadFile: VirtualFile, outputPath: Path? = null): Path? {
        val openscadPath = findOpenSCADExecutable() ?: run {
            logger.warn("OpenSCAD executable not found")
            return null
        }
        
        val stlPath = outputPath ?: createTempSTLPath(scadFile.nameWithoutExtension)
        
        try {
            val settings = org.openscad.settings.OpenSCADSettings.getInstance(project)
            
            // Build parameter list
            val params = mutableListOf<String>()
            
            // Rendering mode
            if (settings.useFullRender) {
                params.add("--render")
            }
            
            // View options
            if (settings.autoCenter) {
                params.add("--autocenter")
            }
            if (settings.viewAll) {
                params.add("--viewall")
            }
            
            // Use project root as working directory so relative paths work
            val workDir = project.basePath ?: scadFile.parent.path
            
            // Output file (absolute path)
            params.add("-o")
            params.add(stlPath.toString())
            
            // Input file - use relative path if in project, otherwise absolute
            val inputPath = if (project.basePath != null && scadFile.path.startsWith(project.basePath!!)) {
                // Make path relative to project root
                File(project.basePath!!).toPath().relativize(File(scadFile.path).toPath()).toString()
            } else {
                // Use absolute path for files outside project
                scadFile.path
            }
            params.add(inputPath)
            
            // Build OPENSCADPATH environment variable from library paths
            val openscadPathEnv = buildOpenSCADPath(settings)
            
            val commandLine = GeneralCommandLine()
                .withExePath(openscadPath)
                .withParameters(params)
                .withWorkDirectory(workDir)
                .withEnvironment("OPENSCADPATH", openscadPathEnv)
            
            logger.info("Executing: ${commandLine.commandLineString}")
            logger.info("Working directory: $workDir")
            logger.info("Input file path: $inputPath")
            logger.info("OPENSCADPATH: $openscadPathEnv")
            
            // Also show in notification if OPENSCADPATH is empty
            if (openscadPathEnv.isEmpty()) {
                showNotification(
                    "No Library Paths Configured",
                    "Add library paths in Settings → Tools → OpenSCAD to resolve 'use' and 'include' statements",
                    NotificationType.INFORMATION
                )
            }
            
            val output: ProcessOutput = ExecUtil.execAndGetOutput(commandLine, 30000)
            
            if (output.exitCode == 0 && stlPath.exists()) {
                logger.info("Successfully rendered ${scadFile.name} to ${stlPath}")
                
                // Show warnings if any
                if (output.stderr.isNotEmpty()) {
                    showNotification(
                        "OpenSCAD Warnings",
                        output.stderr,
                        NotificationType.WARNING
                    )
                }
                
                return stlPath
            } else {
                logger.error("OpenSCAD rendering failed with exit code ${output.exitCode}")
                logger.error("Stdout: ${output.stdout}")
                logger.error("Stderr: ${output.stderr}")
                
                // Show error notification
                val errorMessage = if (output.stderr.isNotEmpty()) {
                    output.stderr
                } else if (output.stdout.isNotEmpty()) {
                    output.stdout
                } else {
                    "Rendering failed with exit code ${output.exitCode}"
                }
                
                showNotification(
                    "OpenSCAD Rendering Failed",
                    errorMessage,
                    NotificationType.ERROR
                )
                
                return null
            }
        } catch (e: Exception) {
            logger.error("Error rendering OpenSCAD file", e)
            showNotification(
                "OpenSCAD Rendering Error",
                "Exception: ${e.message}",
                NotificationType.ERROR
            )
            return null
        }
    }
    
    /**
     * Show a notification in the IDE event log
     */
    private fun showNotification(title: String, content: String, type: NotificationType) {
        val notification = NotificationGroupManager.getInstance()
            .getNotificationGroup("OpenSCAD")
            .createNotification(title, content, type)
        
        notification.notify(project)
    }
    
    /**
     * Build OPENSCADPATH environment variable from configured library paths
     */
    private fun buildOpenSCADPath(settings: org.openscad.settings.OpenSCADSettings): String {
        val paths = mutableListOf<String>()
        
        // Add configured library paths
        paths.addAll(settings.libraryPaths.filter { File(it).exists() })
        
        // Add project-relative lib directory if it exists
        val projectLibDir = File(project.basePath, "lib")
        if (projectLibDir.exists() && projectLibDir.isDirectory) {
            paths.add(projectLibDir.absolutePath)
        }
        
        // Add common OpenSCAD library locations
        val commonPaths = listOf(
            "/usr/share/openscad/libraries",
            "/usr/local/share/openscad/libraries",
            System.getProperty("user.home") + "/.local/share/OpenSCAD/libraries",
            System.getProperty("user.home") + "/Documents/OpenSCAD/libraries"
        )
        
        paths.addAll(commonPaths.filter { File(it).exists() })
        
        // Join with platform-specific path separator
        return paths.joinToString(File.pathSeparator)
    }
    
    /**
     * Finds the OpenSCAD executable on the system
     */
    fun findOpenSCADExecutable(): String? {
        // First check settings for custom path
        val settings = org.openscad.settings.OpenSCADSettings.getInstance(project)
        if (settings.openscadPath.isNotEmpty() && File(settings.openscadPath).exists()) {
            return settings.openscadPath
        }
        
        // Auto-detect
        return autoDetectOpenSCAD()
    }
    
    private fun autoDetectOpenSCAD(): String? {
        // Try common locations
        val possiblePaths = when {
            System.getProperty("os.name").contains("Mac", ignoreCase = true) -> listOf(
                "/Applications/OpenSCAD.app/Contents/MacOS/OpenSCAD",
                "/usr/local/bin/openscad",
                "/opt/homebrew/bin/openscad"
            )
            System.getProperty("os.name").contains("Windows", ignoreCase = true) -> listOf(
                "C:\\Program Files\\OpenSCAD\\openscad.exe",
                "C:\\Program Files (x86)\\OpenSCAD\\openscad.exe"
            )
            else -> listOf( // Linux
                "/usr/bin/openscad",
                "/usr/local/bin/openscad",
                "/snap/bin/openscad"
            )
        }
        
        // Check each path
        for (path in possiblePaths) {
            if (File(path).exists()) {
                return path
            }
        }
        
        // Try PATH
        try {
            val result = ExecUtil.execAndGetOutput(GeneralCommandLine("which", "openscad"), 5000)
            if (result.exitCode == 0) {
                return result.stdout.trim()
            }
        } catch (e: Exception) {
            // Ignore
        }
        
        return null
    }
    
    /**
     * Creates a temporary path for the STL output
     */
    private fun createTempSTLPath(baseName: String): Path {
        val tempDir = Files.createTempDirectory("openscad-preview")
        return tempDir.resolve("$baseName.stl")
    }
    
    /**
     * Checks if OpenSCAD is available on the system
     */
    fun isOpenSCADAvailable(): Boolean {
        return findOpenSCADExecutable() != null
    }
}
