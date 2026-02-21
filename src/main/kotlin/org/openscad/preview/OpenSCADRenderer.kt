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
     * @param usePreviewMode If true, uses preview mode (shows %, #, ! modifiers); if false, uses settings
     * @return Path to the generated STL file, or null if rendering failed
     */
    fun renderToSTL(scadFile: VirtualFile, outputPath: Path? = null, usePreviewMode: Boolean = false): Path? {
        val openscadPath = findOpenSCADExecutable() ?: run {
            logger.warn("OpenSCAD executable not found")
            return null
        }
        
        val stlPath = outputPath ?: createTempSTLPath(scadFile.nameWithoutExtension)
        
        try {
            val settings = org.openscad.settings.OpenSCADSettings.getInstance(project)
            
            // Build parameter list
            val params = mutableListOf<String>()

            // Backend selection (Manifold or CGAL)
            if (settings.renderBackend.isNotEmpty()) {
                params.add("--backend=${settings.renderBackend}")
            }

            // Rendering mode
            // Use preview mode if requested (to show debug modifiers like %, #, !, *)
            // Otherwise respect settings
            if (!usePreviewMode && settings.useFullRender) {
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
            
            // Output file (absolute path) - use toAbsolutePath to ensure OpenSCAD can resolve it
            params.add("-o")
            params.add(stlPath.toAbsolutePath().toString())
            
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
        
        // Add common OpenSCAD library locations based on OS
        paths.addAll(org.openscad.util.OpenSCADPathUtils.getExistingLibraryPaths())
        
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
        
        // Try PATH using OS-appropriate command
        try {
            val findCommand = if (org.openscad.util.OpenSCADPathUtils.isWindows()) {
                GeneralCommandLine("where", "openscad")
            } else {
                GeneralCommandLine("which", "openscad")
            }
            val result = ExecUtil.execAndGetOutput(findCommand, 5000)
            if (result.exitCode == 0) {
                return result.stdout.trim().lines().firstOrNull()?.trim()
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
        val settings = org.openscad.settings.OpenSCADSettings.getInstance(project)
        val tempDir = org.openscad.util.OpenSCADPathUtils.createTempDirectory("openscad-preview", settings.customTempDirectory)
        // Ensure the directory exists and is accessible
        if (!Files.exists(tempDir) || !Files.isDirectory(tempDir)) {
            logger.error("Failed to create temp directory: $tempDir")
            throw IllegalStateException("Could not create temp directory for rendering")
        }
        logger.info("Created temp directory: $tempDir")
        return tempDir.resolve("$baseName.stl")
    }
    
    /**
     * Renders an OpenSCAD file to 3MF format (preserves colors from color() statements)
     * @param scadFile The OpenSCAD source file
     * @param outputPath Optional output path for the 3MF file. If null, uses temp directory
     * @return Path to the generated 3MF file, or null if rendering failed
     */
    fun renderTo3MF(scadFile: VirtualFile, outputPath: Path? = null): Path? {
        val openscadPath = findOpenSCADExecutable() ?: run {
            logger.warn("OpenSCAD executable not found")
            return null
        }
        
        val settings = org.openscad.settings.OpenSCADSettings.getInstance(project)
        val threemfPath = outputPath ?: run {
            val tempDir = org.openscad.util.OpenSCADPathUtils.createTempDirectory("openscad-preview", settings.customTempDirectory)
            if (!Files.exists(tempDir) || !Files.isDirectory(tempDir)) {
                logger.error("Failed to create temp directory: $tempDir")
                return null
            }
            tempDir.resolve("${scadFile.nameWithoutExtension}.3mf")
        }
        
        try {
            val params = mutableListOf<String>()

            // Backend selection (Manifold or CGAL)
            if (settings.renderBackend.isNotEmpty()) {
                params.add("--backend=${settings.renderBackend}")
            }

            // Rendering mode - need full render for 3MF export
            params.add("--render")
            
            // View options
            if (settings.autoCenter) {
                params.add("--autocenter")
            }
            if (settings.viewAll) {
                params.add("--viewall")
            }
            
            // Use project root as working directory
            val workDir = project.basePath ?: scadFile.parent.path
            
            // Output file (absolute path)
            params.add("-o")
            params.add(threemfPath.toAbsolutePath().toString())
            
            // Input file
            val inputPath = if (project.basePath != null && scadFile.path.startsWith(project.basePath!!)) {
                File(project.basePath!!).toPath().relativize(File(scadFile.path).toPath()).toString()
            } else {
                scadFile.path
            }
            params.add(inputPath)
            
            val openscadPathEnv = buildOpenSCADPath(settings)
            
            val commandLine = GeneralCommandLine()
                .withExePath(openscadPath)
                .withParameters(params)
                .withWorkDirectory(workDir)
                .withEnvironment("OPENSCADPATH", openscadPathEnv)
            
            logger.info("Executing 3MF render: ${commandLine.commandLineString}")
            
            val output: ProcessOutput = ExecUtil.execAndGetOutput(commandLine, settings.renderTimeout * 1000)
            
            if (output.exitCode == 0 && threemfPath.exists()) {
                logger.info("Successfully rendered ${scadFile.name} to 3MF")
                return threemfPath
            } else {
                logger.error("3MF rendering failed: ${output.stderr}")
                return null
            }
        } catch (e: Exception) {
            logger.error("Error rendering to 3MF", e)
            return null
        }
    }
    
    /**
     * Renders an OpenSCAD file to PNG format (shows debug modifiers # % ! *)
     * @param scadFile The OpenSCAD source file
     * @param width Image width
     * @param height Image height
     * @param cameraParams Optional camera parameters (rotX, rotY, rotZ in degrees, distance)
     * @return Path to the generated PNG file, or null if rendering failed
     */
    fun renderToPNG(
        scadFile: VirtualFile, 
        width: Int = 800, 
        height: Int = 600,
        cameraParams: CameraParameters? = null
    ): Path? {
        val openscadPath = findOpenSCADExecutable() ?: run {
            logger.warn("OpenSCAD executable not found")
            return null
        }
        
        val settings = org.openscad.settings.OpenSCADSettings.getInstance(project)
        val tempDir = org.openscad.util.OpenSCADPathUtils.createTempDirectory("openscad-preview", settings.customTempDirectory)
        // Ensure the directory exists and is accessible
        if (!Files.exists(tempDir) || !Files.isDirectory(tempDir)) {
            logger.error("Failed to create temp directory: $tempDir")
            return null
        }
        logger.info("Created temp directory for PNG: $tempDir")
        val pngPath = tempDir.resolve("${scadFile.nameWithoutExtension}.png")
        
        try {
            
            val params = mutableListOf<String>()

            // Backend selection (Manifold or CGAL)
            if (settings.renderBackend.isNotEmpty()) {
                params.add("--backend=${settings.renderBackend}")
            }

            // Use preview mode (not --render) to show debug modifiers
            // Debug modifiers (#, %) are only visible in preview mode
            
            // Camera parameters for preserving orientation
            if (cameraParams != null) {
                // OpenSCAD camera format: --camera=translate_x,translate_y,translate_z,rot_x,rot_y,rot_z,distance
                val cameraString = "0,0,0,${cameraParams.rotX},${cameraParams.rotY},${cameraParams.rotZ},${cameraParams.distance}"
                logger.info("Debug preview camera: --camera=$cameraString")
                params.add("--camera")
                params.add(cameraString)
                // Use autocenter and viewall to ensure model is centered and visible
                params.add("--autocenter")
                params.add("--viewall")
            } else {
                // Use settings when not using custom camera
                if (settings.autoCenter) {
                    params.add("--autocenter")
                }
                if (settings.viewAll) {
                    params.add("--viewall")
                }
            }
            
            // Image size
            params.add("--imgsize")
            params.add("$width,$height")
            
            // Use OpenSCAD's default colorscheme which shows debug colors
            params.add("--colorscheme")
            params.add("Cornfield")
            
            // Output file (absolute path)
            params.add("-o")
            params.add(pngPath.toAbsolutePath().toString())
            
            // Use project root as working directory
            val workDir = project.basePath ?: scadFile.parent.path
            
            // Input file
            val inputPath = if (project.basePath != null && scadFile.path.startsWith(project.basePath!!)) {
                File(project.basePath!!).toPath().relativize(File(scadFile.path).toPath()).toString()
            } else {
                scadFile.path
            }
            params.add(inputPath)
            
            val openscadPathEnv = buildOpenSCADPath(settings)
            
            val commandLine = GeneralCommandLine()
                .withExePath(openscadPath)
                .withParameters(params)
                .withWorkDirectory(workDir)
                .withEnvironment("OPENSCADPATH", openscadPathEnv)
            
            logger.info("Executing PNG render: ${commandLine.commandLineString}")
            
            val output: ProcessOutput = ExecUtil.execAndGetOutput(commandLine, 30000)
            
            if (output.exitCode == 0 && pngPath.exists()) {
                logger.info("Successfully rendered ${scadFile.name} to PNG")
                return pngPath
            } else {
                logger.error("PNG rendering failed: ${output.stderr}")
                return null
            }
        } catch (e: Exception) {
            logger.error("Error rendering to PNG", e)
            return null
        }
    }
    
    /**
     * Checks if OpenSCAD is available on the system
     */
    fun isOpenSCADAvailable(): Boolean {
        return findOpenSCADExecutable() != null
    }
    
    /**
     * Camera parameters for PNG rendering
     */
    data class CameraParameters(
        val rotX: Double,
        val rotY: Double,
        val rotZ: Double,
        val distance: Double
    )
}
