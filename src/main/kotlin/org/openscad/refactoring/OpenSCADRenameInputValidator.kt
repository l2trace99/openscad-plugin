package org.openscad.refactoring

import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.refactoring.rename.RenameInputValidatorEx
import com.intellij.util.ProcessingContext
import org.openscad.psi.OpenSCADFunctionDeclaration
import org.openscad.psi.OpenSCADModuleDeclaration

/**
 * Validates rename input for OpenSCAD elements.
 * Provides custom error messages for invalid names.
 */
class OpenSCADRenameInputValidator : RenameInputValidatorEx {
    
    companion object {
        private val KEYWORDS = setOf(
            "module", "function", "include", "use",
            "if", "else", "for", "intersection_for", "let",
            "true", "false", "undef", "each", "assert", "echo"
        )
        
        private val IDENTIFIER_PATTERN = Regex("^[a-zA-Z_][a-zA-Z0-9_]*$")
    }
    
    /**
     * Returns the pattern of elements this validator applies to.
     * Validates module and function declarations.
     */
    override fun getPattern(): ElementPattern<out PsiElement> {
        return PlatformPatterns.or(
            PlatformPatterns.psiElement(OpenSCADModuleDeclaration::class.java),
            PlatformPatterns.psiElement(OpenSCADFunctionDeclaration::class.java)
        )
    }
    
    /**
     * Checks if the input name is valid.
     */
    override fun isInputValid(newName: String, element: PsiElement, context: ProcessingContext): Boolean {
        return isInputValid(newName)
    }
    
    /**
     * Standalone validation method for testing.
     */
    fun isInputValid(newName: String): Boolean {
        if (newName.isEmpty()) return false
        if (newName in KEYWORDS) return false
        return IDENTIFIER_PATTERN.matches(newName)
    }
    
    /**
     * Returns a custom error message for invalid names.
     * Returns null if the name is valid.
     */
    override fun getErrorMessage(newName: String, project: com.intellij.openapi.project.Project): String? {
        return getErrorMessage(newName)
    }
    
    /**
     * Standalone error message method for testing.
     */
    fun getErrorMessage(newName: String): String? {
        if (newName.isEmpty()) {
            return "Name cannot be empty"
        }
        
        if (newName in KEYWORDS) {
            return "'$newName' is a reserved keyword in OpenSCAD"
        }
        
        if (!newName[0].isLetter() && newName[0] != '_') {
            return "Identifier must start with a letter or underscore"
        }
        
        if (!IDENTIFIER_PATTERN.matches(newName)) {
            return "Identifier can only contain letters, digits, and underscores"
        }
        
        return null
    }
}
