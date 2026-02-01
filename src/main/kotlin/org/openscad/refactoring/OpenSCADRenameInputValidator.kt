package org.openscad.refactoring

import com.intellij.openapi.project.Project
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.refactoring.rename.RenameInputValidatorEx
import com.intellij.util.ProcessingContext
import org.openscad.psi.OpenSCADNamedElement

class OpenSCADRenameInputValidator : RenameInputValidatorEx {
    
    private val namesValidator = OpenSCADNamesValidator()
    
    override fun isInputValid(newName: String, element: PsiElement, context: ProcessingContext): Boolean {
        return namesValidator.isIdentifier(newName, element.project)
    }
    
    override fun getPattern(): ElementPattern<out PsiElement> {
        return PlatformPatterns.psiElement(OpenSCADNamedElement::class.java)
    }
    
    override fun getErrorMessage(newName: String, project: Project): String? {
        if (newName.isEmpty()) {
            return "Name cannot be empty"
        }
        if (!namesValidator.isIdentifier(newName, project)) {
            return "Invalid identifier: must start with letter, underscore, or $, followed by letters, digits, or underscores"
        }
        if (namesValidator.isKeyword(newName, project)) {
            return "'$newName' is a reserved keyword"
        }
        return null
    }
}
