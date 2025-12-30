package org.openscad.references

import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import org.openscad.OpenSCADLanguage
import org.openscad.psi.OpenSCADFunctionDeclaration
import org.openscad.psi.OpenSCADModuleDeclaration
import org.openscad.psi.OpenSCADTypes

/**
 * Reference implementation for OpenSCAD symbols (modules, functions, variables)
 * Enables Cmd/Ctrl+click navigation to symbol definitions
 */
class OpenSCADReference(element: PsiElement, textRange: TextRange) :
    PsiReferenceBase<PsiElement>(element, textRange), PsiPolyVariantReference {
    
    private val referenceName: String = element.text.substring(textRange.startOffset, textRange.endOffset)
    
    override fun resolve(): PsiElement? {
        val results = multiResolve(false)
        return if (results.size == 1) results[0].element else null
    }
    
    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        val file = element.containingFile ?: return emptyArray()
        val project = element.project
        val results = mutableListOf<ResolveResult>()
        
        // 1. Find module declarations in current file
        PsiTreeUtil.findChildrenOfType(file, OpenSCADModuleDeclaration::class.java).forEach { module ->
            if (module.name == referenceName) {
                results.add(PsiElementResolveResult(module))
            }
        }
        
        // 2. Find function declarations in current file
        PsiTreeUtil.findChildrenOfType(file, OpenSCADFunctionDeclaration::class.java).forEach { function ->
            if (function.name == referenceName) {
                results.add(PsiElementResolveResult(function))
            }
        }
        
        // 3. Find imported symbols from use/include statements
        val importedSymbols = OpenSCADImportResolver.getImportedSymbols(file)
        importedSymbols.forEach { symbol ->
            if (symbol.name == referenceName) {
                results.add(PsiElementResolveResult(symbol.declaration))
            }
        }
        
        // 4. Find symbols from indexed library files
        if (results.isEmpty()) {
            try {
                val indexer = OpenSCADLibraryIndexer.getInstance(project)
                val librarySymbols = indexer.getLibrarySymbols()
                librarySymbols.forEach { symbol ->
                    if (symbol.name == referenceName) {
                        // Resolve the library file and find the declaration
                        val virtualFile = LocalFileSystem.getInstance().findFileByPath(symbol.filePath)
                        if (virtualFile != null) {
                            val psiFile = PsiManager.getInstance(project).findFile(virtualFile)
                            if (psiFile != null) {
                                when (symbol.type) {
                                    OpenSCADLibraryIndexer.SymbolType.MODULE -> {
                                        PsiTreeUtil.findChildrenOfType(psiFile, OpenSCADModuleDeclaration::class.java)
                                            .find { it.name == referenceName }
                                            ?.let { results.add(PsiElementResolveResult(it)) }
                                    }
                                    OpenSCADLibraryIndexer.SymbolType.FUNCTION -> {
                                        PsiTreeUtil.findChildrenOfType(psiFile, OpenSCADFunctionDeclaration::class.java)
                                            .find { it.name == referenceName }
                                            ?.let { results.add(PsiElementResolveResult(it)) }
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
        
        return results.toTypedArray()
    }
    
    override fun getVariants(): Array<Any> {
        val file = element.containingFile
        val variants = mutableListOf<Any>()
        
        // Add all module and function declarations as completion variants
        PsiTreeUtil.findChildrenOfType(file, OpenSCADModuleDeclaration::class.java).forEach { module ->
            module.name?.let { variants.add(it) }
        }
        
        PsiTreeUtil.findChildrenOfType(file, OpenSCADFunctionDeclaration::class.java).forEach { function ->
            function.name?.let { variants.add(it) }
        }
        
        return variants.toTypedArray()
    }
}

/**
 * Registers the reference provider for OpenSCAD identifiers
 */
class OpenSCADReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        // Register for all leaf elements in OpenSCAD files
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement()
                .withLanguage(OpenSCADLanguage.INSTANCE),
            OpenSCADReferenceProvider()
        )
    }
}

/**
 * Provides references for identifier elements that represent symbol usages
 */
class OpenSCADReferenceProvider : PsiReferenceProvider() {
    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        // Only work with leaf elements (actual tokens)
        if (element.firstChild != null) {
            return emptyArray()
        }
        
        // Skip declarations - we only want usages
        if (element is OpenSCADModuleDeclaration || element is OpenSCADFunctionDeclaration) {
            return emptyArray()
        }
        
        // Skip if parent is a declaration (the identifier inside the declaration)
        val parent = element.parent
        if (parent is OpenSCADModuleDeclaration || parent is OpenSCADFunctionDeclaration) {
            return emptyArray()
        }
        
        // Check if this looks like an identifier token
        val elementType = element.node?.elementType
        val isIdent = elementType == OpenSCADTypes.IDENT || 
                      elementType?.toString() == "IDENT" ||
                      (element.text?.matches(IDENT_PATTERN) == true && elementType?.toString()?.contains("COMMENT") != true)
        
        if (!isIdent) {
            return emptyArray()
        }
        
        // Skip keywords and literals
        val text = element.text
        if (text.isNullOrEmpty() || isKeywordOrBuiltin(text)) {
            return emptyArray()
        }
        
        val textRange = TextRange(0, element.textLength)
        return arrayOf(OpenSCADReference(element, textRange))
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
