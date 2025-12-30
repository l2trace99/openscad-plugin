package org.openscad.references

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import org.openscad.psi.OpenSCADFunctionDeclaration
import org.openscad.psi.OpenSCADModuleDeclaration

/**
 * Handles Cmd/Ctrl+Click navigation to symbol definitions
 */
class OpenSCADGotoDeclarationHandler : GotoDeclarationHandler {
    
    override fun getGotoDeclarationTargets(
        sourceElement: PsiElement?,
        offset: Int,
        editor: Editor?
    ): Array<PsiElement>? {
        if (sourceElement == null) return null
        
        val text = sourceElement.text ?: return null
        
        // Skip if not an identifier-like text
        if (!text.matches(IDENT_PATTERN)) return null
        
        // Skip keywords and builtins
        if (isKeywordOrBuiltin(text)) return null
        
        val file = sourceElement.containingFile ?: return null
        val project = sourceElement.project
        val results = mutableListOf<PsiElement>()
        
        // 1. Find in current file - modules
        PsiTreeUtil.findChildrenOfType(file, OpenSCADModuleDeclaration::class.java).forEach { module ->
            if (module.name == text) {
                results.add(module)
            }
        }
        
        // 2. Find in current file - functions
        PsiTreeUtil.findChildrenOfType(file, OpenSCADFunctionDeclaration::class.java).forEach { function ->
            if (function.name == text) {
                results.add(function)
            }
        }
        
        // 3. Find in imported files (use/include statements)
        if (results.isEmpty()) {
            val importedSymbols = OpenSCADImportResolver.getImportedSymbols(file)
            importedSymbols.forEach { symbol ->
                if (symbol.name == text) {
                    results.add(symbol.declaration)
                }
            }
        }
        
        // 4. Find in indexed library files
        if (results.isEmpty()) {
            try {
                val indexer = OpenSCADLibraryIndexer.getInstance(project)
                val librarySymbols = indexer.getLibrarySymbols()
                librarySymbols.forEach { symbol ->
                    if (symbol.name == text) {
                        val virtualFile = LocalFileSystem.getInstance().findFileByPath(symbol.filePath)
                        if (virtualFile != null) {
                            val psiFile = PsiManager.getInstance(project).findFile(virtualFile)
                            if (psiFile != null) {
                                when (symbol.type) {
                                    OpenSCADLibraryIndexer.SymbolType.MODULE -> {
                                        PsiTreeUtil.findChildrenOfType(psiFile, OpenSCADModuleDeclaration::class.java)
                                            .find { it.name == text }
                                            ?.let { results.add(it) }
                                    }
                                    OpenSCADLibraryIndexer.SymbolType.FUNCTION -> {
                                        PsiTreeUtil.findChildrenOfType(psiFile, OpenSCADFunctionDeclaration::class.java)
                                            .find { it.name == text }
                                            ?.let { results.add(it) }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Silently ignore indexer errors
            }
        }
        
        return if (results.isNotEmpty()) results.toTypedArray() else null
    }
    
    private fun isKeywordOrBuiltin(text: String): Boolean {
        return text in KEYWORDS || text in BUILTIN_MODULES || text in BUILTIN_FUNCTIONS
    }
    
    companion object {
        private val IDENT_PATTERN = Regex("^[a-zA-Z_][a-zA-Z0-9_]*$")
        
        private val KEYWORDS = setOf(
            "module", "function", "include", "use",
            "if", "else", "for", "intersection_for", "let",
            "true", "false", "undef"
        )
        
        private val BUILTIN_MODULES = setOf(
            "cube", "sphere", "cylinder", "polyhedron", "text",
            "circle", "square", "polygon", "import", "surface",
            "translate", "rotate", "scale", "resize", "mirror", "multmatrix", "color", "offset",
            "union", "difference", "intersection", "hull", "minkowski",
            "linear_extrude", "rotate_extrude", "projection",
            "render", "children", "echo", "assert"
        )
        
        private val BUILTIN_FUNCTIONS = setOf(
            "cos", "sin", "tan", "acos", "asin", "atan", "atan2",
            "abs", "ceil", "floor", "round", "sqrt", "pow", "exp", "ln", "log",
            "min", "max", "norm", "cross", "len", "concat", "lookup",
            "str", "chr", "ord", "search", "version", "version_num", "parent_module"
        )
    }
}
