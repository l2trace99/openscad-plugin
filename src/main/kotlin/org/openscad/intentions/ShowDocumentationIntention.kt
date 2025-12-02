package org.openscad.intentions

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo
import com.intellij.lang.documentation.DocumentationProvider
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.ui.components.JBScrollPane
import org.openscad.documentation.OpenSCADDocumentationProvider
import org.openscad.psi.OpenSCADFunctionDeclaration
import org.openscad.psi.OpenSCADModuleDeclaration
import org.openscad.psi.OpenSCADTypes
import java.awt.Dimension
import javax.swing.JEditorPane
import javax.swing.text.html.HTMLEditorKit

/**
 * Intention action to show documentation for OpenSCAD functions and modules
 */
class ShowDocumentationIntention : IntentionAction {
    
    override fun getText(): String {
        // Make the text dynamic based on the element
        return "Show quick documentation"
    }
    
    override fun getFamilyName(): String = "OpenSCAD"
    
    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        if (editor == null || file == null) return false
        
        // Get element at caret
        val offset = editor.caretModel.offset
        val element = file.findElementAt(offset) ?: return false
        
        // Available if cursor is on an identifier that could be a function/module call
        if (element.node?.elementType != OpenSCADTypes.IDENT) {
            return false
        }
        
        // Check if it's a built-in function/module or a user-defined one
        val text = element.text
        return isBuiltInSymbol(text) || findDeclaration(element) != null
    }
    
    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        if (editor == null || file == null) return
        
        // Get element at caret
        val offset = editor.caretModel.offset
        val element = file.findElementAt(offset) ?: return
        
        // Get documentation using the documentation provider
        val docProvider = OpenSCADDocumentationProvider()
        val text = element.text
        
        // Get documentation for the element or its declaration
        val documentation = when {
            findDeclaration(element) != null -> {
                val declaration = findDeclaration(element)
                docProvider.generateDoc(declaration, element)
            }
            else -> {
                docProvider.generateDoc(element, element)
            }
        } ?: return
        
        // Create HTML viewer
        val editorPane = JEditorPane().apply {
            editorKit = HTMLEditorKit()
            setText(documentation)
            isEditable = false
            caretPosition = 0
        }
        
        val scrollPane = JBScrollPane(editorPane).apply {
            preferredSize = Dimension(500, 400)
        }
        
        // Show popup
        JBPopupFactory.getInstance()
            .createComponentPopupBuilder(scrollPane, editorPane)
            .setTitle("OpenSCAD Documentation: $text")
            .setResizable(true)
            .setMovable(true)
            .setRequestFocus(true)
            .createPopup()
            .showInBestPositionFor(editor)
    }
    
    override fun generatePreview(project: Project, editor: Editor, file: PsiFile): IntentionPreviewInfo {
        // Don't generate preview for this intention as it shows a popup
        return IntentionPreviewInfo.EMPTY
    }
    
    override fun startInWriteAction(): Boolean = false
    
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
