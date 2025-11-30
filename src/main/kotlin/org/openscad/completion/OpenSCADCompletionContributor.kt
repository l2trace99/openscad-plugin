package org.openscad.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.util.ProcessingContext
import org.openscad.OpenSCADLanguage
import org.openscad.psi.OpenSCADTypes
import org.openscad.references.OpenSCADImportResolver

class OpenSCADCompletionContributor : CompletionContributor() {
    init {
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement().withLanguage(OpenSCADLanguage.INSTANCE),
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
