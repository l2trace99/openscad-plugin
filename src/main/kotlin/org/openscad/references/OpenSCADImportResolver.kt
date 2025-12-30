package org.openscad.references

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import org.openscad.psi.OpenSCADFunctionDeclaration
import org.openscad.psi.OpenSCADModuleDeclaration
import org.openscad.settings.OpenSCADSettings

/**
 * Resolves imported symbols from use/include statements
 */
object OpenSCADImportResolver {
    
    /**
     * Find all symbols imported via use/include statements in a file
     */
    fun getImportedSymbols(file: PsiFile): List<ImportedSymbol> {
        val symbols = mutableListOf<ImportedSymbol>()
        
        // Find all use/include statements by looking for elements with these keywords
        file.accept(object : PsiRecursiveElementVisitor() {
            override fun visitElement(element: PsiElement) {
                super.visitElement(element)
                
                val text = element.text
                if (text.startsWith("use <") || text.startsWith("include <")) {
                    val importType = if (text.startsWith("use")) ImportType.USE else ImportType.INCLUDE
                    val importedFile = resolveImportPath(element, file.project)
                    if (importedFile != null) {
                        symbols.addAll(extractExportedSymbols(importedFile, importType))
                    }
                }
            }
        })
        
        return symbols
    }
    
    /**
     * Resolve the path from a use/include statement to an actual file
     */
    private fun resolveImportPath(statement: PsiElement, project: Project): PsiFile? {
        // Extract path from statement
        val pathText = extractPathFromStatement(statement) ?: return null
        
        // Get the directory of the current file
        val currentFile = statement.containingFile.virtualFile ?: return null
        val currentDir = currentFile.parent ?: return null
        
        // Try to resolve relative to current file
        var targetFile = currentDir.findFileByRelativePath(pathText)
        
        // If not found, try project-relative paths
        if (targetFile == null) {
            val contentRoots = com.intellij.openapi.roots.ProjectRootManager.getInstance(project).contentRoots
            for (root in contentRoots) {
                targetFile = root.findFileByRelativePath(pathText)
                if (targetFile != null) break
            }
        }
        
        // If still not found, try common library paths
        if (targetFile == null) {
            targetFile = findInLibraryPaths(pathText, project)
        }
        
        return if (targetFile != null) {
            PsiManager.getInstance(project).findFile(targetFile)
        } else {
            null
        }
    }
    
    /**
     * Extract the path string from use/include statement
     */
    private fun extractPathFromStatement(statement: PsiElement): String? {
        val text = statement.text
        
        // Extract path between < and >
        val startIdx = text.indexOf('<')
        val endIdx = text.indexOf('>')
        
        if (startIdx != -1 && endIdx != -1 && endIdx > startIdx) {
            return text.substring(startIdx + 1, endIdx).trim()
        }
        
        return null
    }
    
    /**
     * Find file in configured and common OpenSCAD library paths
     */
    private fun findInLibraryPaths(path: String, project: Project): VirtualFile? {
        val allPaths = mutableListOf<String>()
        
        // First, add user-configured library paths from settings
        try {
            val settings = OpenSCADSettings.getInstance(project)
            allPaths.addAll(settings.libraryPaths)
        } catch (e: Exception) {
            // Settings not available, continue with common paths
        }
        
        // Then add common library locations
        allPaths.addAll(listOf(
            "/usr/share/openscad/libraries",
            "/usr/local/share/openscad/libraries",
            System.getProperty("user.home") + "/.local/share/OpenSCAD/libraries",
            System.getProperty("user.home") + "/Documents/OpenSCAD/libraries"
        ))
        
        for (libPath in allPaths) {
            val libDir = LocalFileSystem.getInstance().findFileByPath(libPath)
            val file = libDir?.findFileByRelativePath(path)
            if (file != null) {
                return file
            }
        }
        
        return null
    }
    
    /**
     * Extract exported symbols from a file
     */
    private fun extractExportedSymbols(file: PsiFile, importType: ImportType): List<ImportedSymbol> {
        val symbols = mutableListOf<ImportedSymbol>()
        
        // Find all module declarations
        val modules = PsiTreeUtil.findChildrenOfType(file, OpenSCADModuleDeclaration::class.java)
        modules.forEach { module ->
            val name = module.name
            if (name != null) {
                symbols.add(ImportedSymbol(name, SymbolType.MODULE, module, importType))
            }
        }
        
        // Find all function declarations
        val functions = PsiTreeUtil.findChildrenOfType(file, OpenSCADFunctionDeclaration::class.java)
        functions.forEach { function ->
            val name = function.name
            if (name != null) {
                symbols.add(ImportedSymbol(name, SymbolType.FUNCTION, function, importType))
            }
        }
        
        // For include statements, also get variables (simplified - just look for assignments)
        if (importType == ImportType.INCLUDE) {
            file.accept(object : PsiRecursiveElementVisitor() {
                override fun visitElement(element: PsiElement) {
                    super.visitElement(element)
                    
                    val text = element.text
                    // Simple pattern matching for assignments
                    if (text.contains("=") && !text.contains("==") && !text.contains("!=")) {
                        val name = extractVariableName(element)
                        if (name != null) {
                            symbols.add(ImportedSymbol(name, SymbolType.VARIABLE, element, importType))
                        }
                    }
                }
            })
        }
        
        return symbols
    }
    
    /**
     * Extract variable name from assignment
     */
    private fun extractVariableName(assignment: PsiElement): String? {
        val text = assignment.text
        val eqIdx = text.indexOf('=')
        if (eqIdx > 0) {
            return text.substring(0, eqIdx).trim()
        }
        return null
    }
    
    enum class ImportType {
        USE,     // Only modules and functions
        INCLUDE  // Everything including variables
    }
    
    enum class SymbolType {
        MODULE,
        FUNCTION,
        VARIABLE
    }
    
    data class ImportedSymbol(
        val name: String,
        val type: SymbolType,
        val declaration: PsiElement,
        val importType: ImportType
    )
}
