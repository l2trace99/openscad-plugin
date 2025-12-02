package org.openscad.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.ui.popup.JBPopupFactory
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
 * Action to show OpenSCAD documentation in a popup
 */
class ShowDocumentationAction : AnAction() {
    
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val psiFile = e.getData(CommonDataKeys.PSI_FILE) ?: return
        val project = e.project ?: return
        
        // Get element at caret
        val offset = editor.caretModel.offset
        val element = psiFile.findElementAt(offset) ?: return
        
        // Only show for identifiers
        if (element.node?.elementType != OpenSCADTypes.IDENT) {
            return
        }
        
        val text = element.text
        val docProvider = OpenSCADDocumentationProvider()
        
        // Get documentation
        val documentation = when {
            // Check for user-defined declaration
            findDeclaration(element, text) != null -> {
                val declaration = findDeclaration(element, text)
                docProvider.generateDoc(declaration, element)
            }
            // Fall back to built-in documentation
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
    
    override fun update(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)
        val psiFile = e.getData(CommonDataKeys.PSI_FILE)
        
        // Enable action only if we have an editor and PSI file
        e.presentation.isEnabled = editor != null && psiFile != null
        
        // Check if cursor is on an identifier
        if (editor != null && psiFile != null) {
            val offset = editor.caretModel.offset
            val element = psiFile.findElementAt(offset)
            e.presentation.isEnabled = element?.node?.elementType == OpenSCADTypes.IDENT
        }
    }
    
    private fun findDeclaration(element: com.intellij.psi.PsiElement, name: String): com.intellij.psi.PsiElement? {
        val file = element.containingFile
        
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
}
