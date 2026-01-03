package org.openscad.util

import java.io.File
import java.nio.file.Files
import java.nio.file.Path

/**
 * Utility functions for OS-independent path handling in OpenSCAD plugin
 */
object OpenSCADPathUtils {
    
    /**
     * Returns a list of common OpenSCAD library paths appropriate for the current OS.
     * Paths are not filtered for existence - caller should filter if needed.
     */
    fun getCommonLibraryPaths(): List<String> {
        val userHome = System.getProperty("user.home")
        return when {
            System.getProperty("os.name").contains("Windows", ignoreCase = true) -> listOf(
                "$userHome\\Documents\\OpenSCAD\\libraries",
                System.getenv("PROGRAMFILES")?.let { "$it\\OpenSCAD\\libraries" },
                System.getenv("PROGRAMFILES(X86)")?.let { "$it\\OpenSCAD\\libraries" }
            ).filterNotNull()
            else -> listOf( // Unix-like (macOS, Linux)
                "/usr/share/openscad/libraries",
                "/usr/local/share/openscad/libraries",
                "$userHome/.local/share/OpenSCAD/libraries",
                "$userHome/Documents/OpenSCAD/libraries"
            )
        }
    }
    
    /**
     * Returns common OpenSCAD library paths that exist on the current system.
     */
    fun getExistingLibraryPaths(): List<String> {
        return getCommonLibraryPaths().filter { File(it).exists() }
    }
    
    /**
     * Returns true if the current OS is Windows.
     */
    fun isWindows(): Boolean {
        return System.getProperty("os.name").contains("Windows", ignoreCase = true)
    }
    
    /**
     * Returns true if the current OS is Linux.
     */
    fun isLinux(): Boolean {
        return System.getProperty("os.name").contains("Linux", ignoreCase = true)
    }
    
    /**
     * Creates a temporary directory for OpenSCAD output that is accessible by
     * sandboxed applications like Flatpak.
     * 
     * Priority:
     * 1. Custom directory from settings (if provided and valid)
     * 2. On Linux: ~/.cache/openscad-plugin/ (Flatpak-compatible)
     * 3. On other systems: standard system temp directory
     * 
     * Before creating a new directory, collects all existing preview directories
     * and deletes them in a background thread, ensuring only one directory exists.
     * 
     * @param prefix Prefix for the temp directory name
     * @param customTempDir Optional custom temp directory from settings (empty = use default)
     * @return Path to the created temporary directory
     */
    fun createTempDirectory(prefix: String, customTempDir: String = ""): Path {
        // Determine the parent directory for temp files
        val parentDir: File = when {
            customTempDir.isNotBlank() -> {
                val customDir = File(customTempDir)
                if (!customDir.exists()) {
                    customDir.mkdirs()
                }
                customDir
            }
            isLinux() -> {
                val cacheDir = File(System.getProperty("user.home"), ".cache/openscad-plugin")
                if (!cacheDir.exists()) {
                    cacheDir.mkdirs()
                }
                cacheDir
            }
            else -> {
                File(System.getProperty("java.io.tmpdir"))
            }
        }
        
        // Collect existing preview directories before creating new one
        val existingDirs = getExistingTempDirectories(parentDir, prefix)
        
        // Create the new temp directory first
        val newDir = Files.createTempDirectory(parentDir.toPath(), prefix)
        
        // Delete old directories in background thread
        if (existingDirs.isNotEmpty()) {
            Thread {
                existingDirs.forEach { dir ->
                    try {
                        dir.deleteRecursively()
                    } catch (e: Exception) {
                        // Ignore errors during cleanup
                    }
                }
            }.start()
        }
        
        return newDir
    }
    
    /**
     * Gets all existing temporary directories that match the prefix.
     * 
     * @param parentDir The parent directory containing temp directories
     * @param prefix The prefix used for temp directory names
     * @return List of directories to be deleted
     */
    private fun getExistingTempDirectories(parentDir: File, prefix: String): List<File> {
        if (!parentDir.exists() || !parentDir.isDirectory) {
            return emptyList()
        }
        
        return parentDir.listFiles()?.filter { file ->
            file.isDirectory && file.name.startsWith(prefix)
        } ?: emptyList()
    }
}
