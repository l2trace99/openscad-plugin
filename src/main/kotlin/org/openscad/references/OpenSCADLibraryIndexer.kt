package org.openscad.references

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import org.openscad.psi.OpenSCADFunctionDeclaration
import org.openscad.psi.OpenSCADModuleDeclaration
import org.openscad.settings.OpenSCADSettings
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Indexes OpenSCAD library files for symbol autocomplete
 * Scans configured library paths and extracts module/function definitions
 */
@Service(Service.Level.PROJECT)
class OpenSCADLibraryIndexer(private val project: Project) {
    
    private val logger = Logger.getInstance(OpenSCADLibraryIndexer::class.java)
    private val symbolCache = ConcurrentHashMap<String, LibrarySymbol>()
    private val fileIndex = ConcurrentHashMap<String, MutableList<LibrarySymbol>>()
    private var lastLibraryFingerprint: String = ""
    private val fileModificationTimes = ConcurrentHashMap<String, Long>()
    
    /**
     * Get all indexed library symbols
     */
    fun getLibrarySymbols(): List<LibrarySymbol> {
        ensureIndexed()
        return symbolCache.values.toList()
    }
    
    /**
     * Get symbols from a specific library file
     */
    fun getSymbolsFromFile(filePath: String): List<LibrarySymbol> {
        ensureIndexed()
        return fileIndex[filePath] ?: emptyList()
    }
    
    /**
     * Force re-indexing of library files
     */
    fun reindex() {
        lastLibraryFingerprint = ""
        ensureIndexed()
    }
    
    /**
     * Check if a file path is within any of the configured library paths
     */
    fun isInLibraryPath(filePath: String): Boolean {
        val normalizedPath = File(filePath).absolutePath
        return getLibraryPaths().any { libPath ->
            normalizedPath.startsWith(libPath)
        }
    }
    
    /**
     * Ensure the index is up-to-date
     */
    private fun ensureIndexed() {
        val libraryPaths = getLibraryPaths()
        val currentFingerprint = computeLibraryFingerprint(libraryPaths)
        
        if (currentFingerprint == lastLibraryFingerprint && symbolCache.isNotEmpty()) {
            logger.debug("Library fingerprint unchanged, skipping reindex")
            return
        }
        
        synchronized(this) {
            // Double-check after acquiring lock
            val recheckFingerprint = computeLibraryFingerprint(libraryPaths)
            if (recheckFingerprint == lastLibraryFingerprint && symbolCache.isNotEmpty()) {
                return
            }
            
            symbolCache.clear()
            fileIndex.clear()
            fileModificationTimes.clear()
            
            logger.info("Indexing OpenSCAD libraries from ${libraryPaths.size} paths (fingerprint changed)")
            
            // Collect all files to index first
            val filesToIndex = mutableListOf<Pair<File, String>>()
            for (libPath in libraryPaths) {
                val dir = File(libPath)
                if (dir.exists() && dir.isDirectory) {
                    dir.walkTopDown()
                        .filter { it.isFile && it.extension == "scad" }
                        .forEach { file ->
                            filesToIndex.add(Pair(file, libPath))
                        }
                }
            }
            
            // Index with progress indicator
            val indicator = ProgressManager.getInstance().progressIndicator
            if (indicator != null) {
                indicator.text = "Indexing OpenSCAD libraries..."
                indicator.isIndeterminate = false
                
                filesToIndex.forEachIndexed { index, (file, libPath) ->
                    indicator.text2 = file.name
                    indicator.fraction = index.toDouble() / filesToIndex.size
                    indexFile(file, libPath)
                    fileModificationTimes[file.absolutePath] = file.lastModified()
                }
            } else {
                // No progress indicator available, just index
                filesToIndex.forEach { (file, libPath) ->
                    indexFile(file, libPath)
                    fileModificationTimes[file.absolutePath] = file.lastModified()
                }
            }
            
            lastLibraryFingerprint = recheckFingerprint
            logger.info("Indexed ${symbolCache.size} symbols from library files")
        }
    }
    
    /**
     * Compute a fingerprint of the library directories based on file paths and modification times.
     * This fingerprint changes when files are added, removed, or modified.
     */
    private fun computeLibraryFingerprint(libraryPaths: List<String>): String {
        val sb = StringBuilder()
        sb.append(libraryPaths.sorted().joinToString(";"))
        sb.append("|")
        
        var fileCount = 0
        var latestModTime = 0L
        
        for (libPath in libraryPaths) {
            val dir = File(libPath)
            if (dir.exists() && dir.isDirectory) {
                dir.walkTopDown()
                    .filter { it.isFile && it.extension == "scad" }
                    .forEach { file ->
                        fileCount++
                        val modTime = file.lastModified()
                        if (modTime > latestModTime) {
                            latestModTime = modTime
                        }
                    }
            }
        }
        
        sb.append(fileCount)
        sb.append("|")
        sb.append(latestModTime)
        
        return sb.toString()
    }
    
    /**
     * Get all configured library paths
     */
    private fun getLibraryPaths(): List<String> {
        val paths = mutableListOf<String>()
        
        // Add configured library paths from settings
        val settings = OpenSCADSettings.getInstance(project)
        paths.addAll(settings.libraryPaths.filter { File(it).exists() })
        
        // Add project directory itself
        project.basePath?.let { basePath ->
            val projectDir = File(basePath)
            if (projectDir.exists() && projectDir.isDirectory) {
                paths.add(projectDir.absolutePath)
            }
            
            // Also add project-relative lib directory if it exists
            val projectLibDir = File(basePath, "lib")
            if (projectLibDir.exists() && projectLibDir.isDirectory) {
                paths.add(projectLibDir.absolutePath)
            }
        }
        
        // Add common OpenSCAD library locations based on OS
        paths.addAll(org.openscad.util.OpenSCADPathUtils.getExistingLibraryPaths())
        
        return paths.distinct()
    }
    
    /**
     * Index a single .scad file
     */
    private fun indexFile(file: File, libraryRoot: String) {
        try {
            val virtualFile = LocalFileSystem.getInstance().findFileByPath(file.absolutePath) ?: return
            
            ApplicationManager.getApplication().runReadAction {
                val psiFile = PsiManager.getInstance(project).findFile(virtualFile) ?: return@runReadAction
                
                // Calculate relative path from library root for use statement
                val relativePath = file.absolutePath
                    .removePrefix(libraryRoot)
                    .removePrefix(File.separator)
                    .replace(File.separatorChar, '/')
                
                val symbols = mutableListOf<LibrarySymbol>()
                
                // Find all module declarations
                val modules = PsiTreeUtil.findChildrenOfType(psiFile, OpenSCADModuleDeclaration::class.java)
                modules.forEach { module ->
                    val name = module.name
                    if (name != null && !name.startsWith("_")) { // Skip private modules (starting with _)
                        val params = extractParameters(module)
                        val symbol = LibrarySymbol(
                            name = name,
                            type = SymbolType.MODULE,
                            filePath = file.absolutePath,
                            relativePath = relativePath,
                            libraryRoot = libraryRoot,
                            parameters = params,
                            documentation = extractDocumentation(module)
                        )
                        symbolCache[name] = symbol
                        symbols.add(symbol)
                    }
                }
                
                // Find all function declarations
                val functions = PsiTreeUtil.findChildrenOfType(psiFile, OpenSCADFunctionDeclaration::class.java)
                functions.forEach { function ->
                    val name = function.name
                    if (name != null && !name.startsWith("_")) { // Skip private functions
                        val params = extractParameters(function)
                        val symbol = LibrarySymbol(
                            name = name,
                            type = SymbolType.FUNCTION,
                            filePath = file.absolutePath,
                            relativePath = relativePath,
                            libraryRoot = libraryRoot,
                            parameters = params,
                            documentation = extractDocumentation(function)
                        )
                        symbolCache[name] = symbol
                        symbols.add(symbol)
                    }
                }
                
                if (symbols.isNotEmpty()) {
                    fileIndex[file.absolutePath] = symbols.toMutableList()
                }
            }
        } catch (e: ProcessCanceledException) {
            // Rethrow ProcessCanceledException - it's a control-flow exception
            throw e
        } catch (e: Exception) {
            logger.warn("Error indexing file: ${file.absolutePath}", e)
        }
    }
    
    /**
     * Extract parameter names from module/function declaration
     */
    private fun extractParameters(element: com.intellij.psi.PsiElement): String {
        val text = element.text
        val startIdx = text.indexOf('(')
        val endIdx = text.indexOf(')')
        
        if (startIdx != -1 && endIdx != -1 && endIdx > startIdx) {
            return text.substring(startIdx + 1, endIdx).trim()
        }
        return ""
    }
    
    /**
     * Extract documentation comment preceding the declaration
     */
    private fun extractDocumentation(element: com.intellij.psi.PsiElement): String? {
        var prev = element.prevSibling
        while (prev != null) {
            val text = prev.text.trim()
            if (text.startsWith("//") || text.startsWith("/*")) {
                return text
                    .removePrefix("//")
                    .removePrefix("/*")
                    .removeSuffix("*/")
                    .trim()
            }
            if (text.isNotEmpty() && !text.startsWith("//") && !text.startsWith("/*")) {
                break
            }
            prev = prev.prevSibling
        }
        return null
    }
    
    enum class SymbolType {
        MODULE,
        FUNCTION
    }
    
    data class LibrarySymbol(
        val name: String,
        val type: SymbolType,
        val filePath: String,
        val relativePath: String,
        val libraryRoot: String,
        val parameters: String,
        val documentation: String?
    )
    
    companion object {
        fun getInstance(project: Project): OpenSCADLibraryIndexer {
            return project.getService(OpenSCADLibraryIndexer::class.java)
        }
    }
}
