package org.openscad.intentions

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.openscad.psi.OpenSCADFunctionDeclaration
import org.openscad.psi.OpenSCADModuleDeclaration
import org.openscad.psi.OpenSCADTypes

/**
 * Intention action to show documentation for OpenSCAD functions and modules
 */
class ShowDocumentationIntention : PsiElementBaseIntentionAction(), IntentionAction {
    
    override fun getText(): String {
        // Make the text dynamic based on the element
        return "Show quick documentation"
    }
    
    override fun getFamilyName(): String = "OpenSCAD"
    
    override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
        // Available if cursor is on an identifier that could be a function/module call
        if (element.node?.elementType != OpenSCADTypes.IDENT) {
            return false
        }
        
        // Check if it's a built-in function/module or a user-defined one
        val text = element.text
        return isBuiltInSymbol(text) || findDeclaration(element) != null
    }
    
    override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
        if (editor == null) return
        
        // Trigger quick documentation for the element at cursor position
        ApplicationManager.getApplication().invokeLater {
            val action = com.intellij.codeInsight.documentation.actions.ShowQuickDocInfoAction()
            val dataContext = com.intellij.openapi.actionSystem.DataContext { dataId ->
                when (dataId) {
                    com.intellij.openapi.actionSystem.CommonDataKeys.PROJECT.name -> project
                    com.intellij.openapi.actionSystem.CommonDataKeys.EDITOR.name -> editor
                    com.intellij.openapi.actionSystem.CommonDataKeys.PSI_FILE.name -> element.containingFile
                    com.intellij.openapi.actionSystem.CommonDataKeys.PSI_ELEMENT.name -> element
                    else -> null
                }
            }
            val event = com.intellij.openapi.actionSystem.AnActionEvent.createFromDataContext(
                "",
                null,
                dataContext
            )
            action.actionPerformed(event)
        }
    }
    
    override fun generatePreview(project: Project, editor: Editor, file: com.intellij.psi.PsiFile): IntentionPreviewInfo {
        // Don't generate preview for this intention as it shows a popup
        return IntentionPreviewInfo.EMPTY
    }
    
    private fun findDeclaration(element: PsiElement): PsiElement? {
        val file = element.containingFile
        val name = element.text
        
        // Search for module declarations
        PsiTreeUtil.findChildrenOfType(file, OpenSCADModuleDeclaration::class.java).forEach { module ->
            if (module.name == name) {
                return module
            }
        }
        
        // Search for function declarations
        PsiTreeUtil.findChildrenOfType(file, OpenSCADFunctionDeclaration::class.java).forEach { function ->
            if (function.name == name) {
                return function
            }
        }
        
        return null
    }
    
    private fun isBuiltInSymbol(name: String): Boolean {
        return name in BUILTIN_SYMBOLS
    }
    
    companion object {
        private val BUILTIN_SYMBOLS = setOf(
            // 3D Primitives
            "cube", "sphere", "cylinder", "polyhedron", "text",
            // 2D Primitives
            "circle", "square", "polygon", "import",
            // Transformations
            "translate", "rotate", "scale", "resize", "mirror", "multmatrix",
            "color", "offset", "hull", "minkowski",
            // Boolean Operations
            "union", "difference", "intersection",
            // Extrusions
            "linear_extrude", "rotate_extrude",
            // Other
            "projection", "surface", "render",
            // Control Flow
            "for", "if", "let", "intersection_for",
            // Functions
            "cos", "sin", "tan", "acos", "asin", "atan", "atan2",
            "abs", "ceil", "floor", "round", "sqrt", "pow", "exp", "ln", "log",
            "min", "max", "norm", "cross", "len", "concat", "lookup",
            "str", "chr", "ord", "search", "version", "version_num",
            "parent_module", "echo", "assert"
        )
    }
}
