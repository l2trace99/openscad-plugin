package org.openscad.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.patterns.PlatformPatterns
import com.intellij.util.ProcessingContext
import org.openscad.OpenSCADLanguage
import org.openscad.file.OpenSCADFileType
import org.openscad.psi.OpenSCADTypes
import org.openscad.references.OpenSCADImportResolver
import org.openscad.references.OpenSCADLibraryIndexer

class OpenSCADCompletionContributor : CompletionContributor() {
    init {
        // Register for OpenSCAD language
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement().withLanguage(OpenSCADLanguage.INSTANCE),
            OpenSCADCompletionProvider()
        )
        
        // Also register for OpenSCAD file type as fallback
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement().inFile(PlatformPatterns.psiFile().withFileType(PlatformPatterns.instanceOf(OpenSCADFileType::class.java))),
            OpenSCADCompletionProvider()
        )
    }
}

class OpenSCADCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        // Add keywords
        KEYWORDS.forEach { keyword ->
            result.addElement(
                LookupElementBuilder.create(keyword)
                    .bold()
                    .withTypeText("keyword")
            )
        }
        
        // Add built-in 3D primitives
        PRIMITIVES_3D.forEach { (name, params) ->
            result.addElement(
                LookupElementBuilder.create(name)
                    .withInsertHandler { context, _ ->
                        val editor = context.editor
                        val document = editor.document
                        val offset = context.tailOffset
                        document.insertString(offset, "($params)")
                        editor.caretModel.moveToOffset(offset + 1)
                    }
                    .withTypeText("3D primitive")
                    .withTailText("($params)")
            )
        }
        
        // Add built-in 2D primitives
        PRIMITIVES_2D.forEach { (name, params) ->
            result.addElement(
                LookupElementBuilder.create(name)
                    .withInsertHandler { context, _ ->
                        val editor = context.editor
                        val document = editor.document
                        val offset = context.tailOffset
                        document.insertString(offset, "($params)")
                        editor.caretModel.moveToOffset(offset + 1)
                    }
                    .withTypeText("2D primitive")
                    .withTailText("($params)")
            )
        }
        
        // Add transformations
        TRANSFORMATIONS.forEach { (name, params) ->
            result.addElement(
                LookupElementBuilder.create(name)
                    .withInsertHandler { context, _ ->
                        val editor = context.editor
                        val document = editor.document
                        val offset = context.tailOffset
                        document.insertString(offset, "($params)")
                        editor.caretModel.moveToOffset(offset + 1)
                    }
                    .withTypeText("transformation")
                    .withTailText("($params)")
            )
        }
        
        // Add boolean operations
        BOOLEAN_OPS.forEach { (name, params) ->
            result.addElement(
                LookupElementBuilder.create(name)
                    .withInsertHandler { context, _ ->
                        val editor = context.editor
                        val document = editor.document
                        val offset = context.tailOffset
                        document.insertString(offset, "($params)")
                        editor.caretModel.moveToOffset(offset + 1)
                    }
                    .withTypeText("boolean operation")
                    .withTailText("($params)")
            )
        }
        
        // Add special variables
        SPECIAL_VARIABLES.forEach { variable ->
            result.addElement(
                LookupElementBuilder.create(variable)
                    .withTypeText("special variable")
            )
        }
        
        // Add built-in functions
        BUILTIN_FUNCTIONS.forEach { (name, params) ->
            result.addElement(
                LookupElementBuilder.create(name)
                    .withInsertHandler { context, _ ->
                        val editor = context.editor
                        val document = editor.document
                        val offset = context.tailOffset
                        document.insertString(offset, "($params)")
                        editor.caretModel.moveToOffset(offset + 1)
                    }
                    .withTypeText("function")
                    .withTailText("($params)")
            )
        }
        
        // Add imported symbols from use/include statements
        val file = parameters.originalFile
        val importedSymbols = OpenSCADImportResolver.getImportedSymbols(file)
        val importedNames = importedSymbols.map { it.name }.toSet()
        
        importedSymbols.forEach { symbol ->
            val typeText = when (symbol.type) {
                OpenSCADImportResolver.SymbolType.MODULE -> "imported module"
                OpenSCADImportResolver.SymbolType.FUNCTION -> "imported function"
                OpenSCADImportResolver.SymbolType.VARIABLE -> "imported variable"
            }
            
            val sourceFile = symbol.declaration.containingFile.name
            
            result.addElement(
                LookupElementBuilder.create(symbol.name)
                    .withTypeText(typeText)
                    .withTailText(" from $sourceFile", true)
                    .withInsertHandler { context, _ ->
                        // For modules/functions, add parentheses
                        if (symbol.type != OpenSCADImportResolver.SymbolType.VARIABLE) {
                            val editor = context.editor
                            val document = editor.document
                            val offset = context.tailOffset
                            document.insertString(offset, "()")
                            editor.caretModel.moveToOffset(offset + 1)
                        }
                    }
            )
        }
        
        // Add library symbols (from indexed library paths)
        try {
            val project = parameters.originalFile.project
            val libraryIndexer = OpenSCADLibraryIndexer.getInstance(project)
            val librarySymbols = libraryIndexer.getLibrarySymbols()
            
            librarySymbols.forEach { symbol ->
            // Skip if already imported
            if (importedNames.contains(symbol.name)) return@forEach
            
            val typeText = symbol.relativePath
            
            val icon = when (symbol.type) {
                OpenSCADLibraryIndexer.SymbolType.MODULE -> AllIcons.Nodes.Module
                OpenSCADLibraryIndexer.SymbolType.FUNCTION -> AllIcons.Nodes.Function
            }
            
            val symbolRelativePath = symbol.relativePath
            result.addElement(
                LookupElementBuilder.create(symbol.name)
                    .withIcon(icon)
                    .withTypeText(typeText)
                    .withTailText(if (symbol.parameters.isNotEmpty()) "(${symbol.parameters})" else "()", true)
                    .withInsertHandler { insertContext, _ ->
                        val editor = insertContext.editor
                        val document = editor.document
                        val offset = insertContext.tailOffset
                        
                        // Check if use statement needs to be added BEFORE modifying document
                        val fileText = document.text
                        val useStatement = "use <$symbolRelativePath>"
                        val needsUseStatement = !fileText.contains(useStatement) && 
                                               !fileText.contains("include <$symbolRelativePath>")
                        
                        // Add use statement first (at top of file)
                        if (needsUseStatement) {
                            val insertPos = findUseInsertPosition(fileText)
                            document.insertString(insertPos, "$useStatement\n")
                        }
                        
                        // Add parentheses - adjust offset if we inserted use statement
                        val adjustedOffset = if (needsUseStatement) {
                            offset + useStatement.length + 1 // +1 for newline
                        } else {
                            offset
                        }
                        document.insertString(adjustedOffset, "()")
                        editor.caretModel.moveToOffset(adjustedOffset + 1)
                    }
            )
        }
        } catch (e: ProcessCanceledException) {
            // Rethrow ProcessCanceledException - it's a control-flow exception
            throw e
        } catch (e: Exception) {
            // Silently ignore other indexer errors to not break autocomplete
        }
    }
    
    /**
     * Find the best position to insert a use statement
     */
    private fun findUseInsertPosition(fileText: String): Int {
        // Find the last use/include statement and insert after it
        val lines = fileText.lines()
        var lastImportEnd = 0
        var currentPos = 0
        
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("use ") || trimmed.startsWith("include ")) {
                lastImportEnd = currentPos + line.length + 1 // +1 for newline
            } else if (trimmed.isNotEmpty() && !trimmed.startsWith("//") && !trimmed.startsWith("/*")) {
                // Found first non-import, non-comment line
                break
            }
            currentPos += line.length + 1 // +1 for newline
        }
        
        return lastImportEnd
    }
    
    companion object {
        private val KEYWORDS = listOf(
            "module", "function", "include", "use",
            "if", "else", "for", "intersection_for", "let",
            "true", "false", "undef"
        )
        
        private val PRIMITIVES_3D = mapOf(
            "cube" to "size",
            "sphere" to "r",
            "cylinder" to "h, r",
            "polyhedron" to "points, faces",
            "text" to "text"
        )
        
        private val PRIMITIVES_2D = mapOf(
            "circle" to "r",
            "square" to "size",
            "polygon" to "points",
            "import" to "file"
        )
        
        private val TRANSFORMATIONS = mapOf(
            "translate" to "v",
            "rotate" to "a",
            "scale" to "v",
            "resize" to "newsize",
            "mirror" to "v",
            "multmatrix" to "m",
            "color" to "c",
            "offset" to "r"
        )
        
        private val BOOLEAN_OPS = mapOf(
            "union" to "",
            "difference" to "",
            "intersection" to "",
            "hull" to "",
            "minkowski" to ""
        )
        
        private val SPECIAL_VARIABLES = listOf(
            "\$fn", "\$fa", "\$fs", "\$t",
            "\$vpr", "\$vpt", "\$vpd",
            "\$children", "\$preview"
        )
        
        private val BUILTIN_FUNCTIONS = mapOf(
            "cos" to "degrees",
            "sin" to "degrees",
            "tan" to "degrees",
            "acos" to "x",
            "asin" to "x",
            "atan" to "x",
            "atan2" to "y, x",
            "abs" to "x",
            "ceil" to "x",
            "floor" to "x",
            "round" to "x",
            "sqrt" to "x",
            "pow" to "base, exponent",
            "exp" to "x",
            "ln" to "x",
            "log" to "x",
            "min" to "a, b",
            "max" to "a, b",
            "norm" to "v",
            "cross" to "a, b",
            "len" to "v",
            "concat" to "lists",
            "lookup" to "key, table",
            "str" to "values",
            "chr" to "n",
            "ord" to "s",
            "search" to "match, string",
            "version" to "",
            "version_num" to "",
            "parent_module" to "n",
            "echo" to "values",
            "assert" to "condition"
        )
    }
}
