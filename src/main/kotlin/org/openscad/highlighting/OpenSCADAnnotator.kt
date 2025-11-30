package org.openscad.highlighting

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.psi.PsiElement
import org.openscad.psi.OpenSCADTypes

/**
 * Annotator for semantic highlighting of OpenSCAD built-in functions and special variables
 */
class OpenSCADAnnotator : Annotator {
    
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (element.node?.elementType != OpenSCADTypes.IDENT) {
            return
        }
        
        val text = element.text
        
        when {
            // Special variables (starting with $)
            text.startsWith("$") -> {
                holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                    .range(element)
                    .textAttributes(SPECIAL_VARIABLE)
                    .create()
            }
            // 3D Primitives
            text in PRIMITIVES_3D -> {
                holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                    .range(element)
                    .textAttributes(BUILTIN_FUNCTION)
                    .create()
            }
            // 2D Primitives
            text in PRIMITIVES_2D -> {
                holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                    .range(element)
                    .textAttributes(BUILTIN_FUNCTION)
                    .create()
            }
            // Transformations
            text in TRANSFORMATIONS -> {
                holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                    .range(element)
                    .textAttributes(BUILTIN_TRANSFORMATION)
                    .create()
            }
            // Boolean operations
            text in BOOLEAN_OPS -> {
                holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                    .range(element)
                    .textAttributes(BUILTIN_TRANSFORMATION)
                    .create()
            }
            // Built-in functions
            text in BUILTIN_FUNCTIONS -> {
                holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                    .range(element)
                    .textAttributes(BUILTIN_FUNCTION)
                    .create()
            }
        }
    }
    
    companion object {
        // Color definitions - using more distinct colors
        val BUILTIN_FUNCTION = TextAttributesKey.createTextAttributesKey(
            "OPENSCAD_BUILTIN_FUNCTION",
            DefaultLanguageHighlighterColors.STATIC_METHOD  // More visible - typically bold/different
        )
        
        val BUILTIN_TRANSFORMATION = TextAttributesKey.createTextAttributesKey(
            "OPENSCAD_BUILTIN_TRANSFORMATION",
            DefaultLanguageHighlighterColors.INSTANCE_METHOD  // Different from functions
        )
        
        val SPECIAL_VARIABLE = TextAttributesKey.createTextAttributesKey(
            "OPENSCAD_SPECIAL_VARIABLE",
            DefaultLanguageHighlighterColors.GLOBAL_VARIABLE  // More visible for $ variables
        )
        
        // 3D Primitives
        private val PRIMITIVES_3D = setOf(
            "cube", "sphere", "cylinder", "polyhedron", "text"
        )
        
        // 2D Primitives
        private val PRIMITIVES_2D = setOf(
            "circle", "square", "polygon", "import"
        )
        
        // Transformations
        private val TRANSFORMATIONS = setOf(
            "translate", "rotate", "scale", "resize", "mirror", "multmatrix",
            "color", "offset", "hull", "minkowski", "projection", "render",
            "linear_extrude", "rotate_extrude", "surface"
        )
        
        // Boolean operations
        private val BOOLEAN_OPS = setOf(
            "union", "difference", "intersection"
        )
        
        // Built-in functions
        private val BUILTIN_FUNCTIONS = setOf(
            // Math functions
            "cos", "sin", "tan", "acos", "asin", "atan", "atan2",
            "abs", "ceil", "floor", "round", "sqrt", "pow", "exp", "ln", "log",
            "min", "max", "sign", "rands",
            // List/vector functions
            "len", "concat", "lookup", "norm", "cross",
            // String functions
            "str", "chr", "ord", "search",
            // Other
            "version", "version_num", "parent_module", "echo", "assert"
        )
    }
}
