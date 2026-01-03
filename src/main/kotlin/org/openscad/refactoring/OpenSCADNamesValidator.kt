package org.openscad.refactoring

import com.intellij.lang.refactoring.NamesValidator
import com.intellij.openapi.project.Project

/**
 * Validates identifier names for OpenSCAD language.
 * Used by the rename refactoring to check if a name is valid.
 */
class OpenSCADNamesValidator : NamesValidator {
    
    companion object {
        private val KEYWORDS = setOf(
            "module", "function", "include", "use",
            "if", "else", "for", "intersection_for", "let",
            "true", "false", "undef", "each", "assert", "echo"
        )
        
        private val IDENTIFIER_PATTERN = Regex("^[a-zA-Z_][a-zA-Z0-9_]*$")
    }
    
    /**
     * Checks if the given string is a valid OpenSCAD identifier.
     * A valid identifier:
     * - Starts with a letter or underscore
     * - Contains only letters, digits, and underscores
     * - Is not a keyword
     */
    override fun isIdentifier(name: String, project: Project?): Boolean {
        if (name.isEmpty()) return false
        if (isKeyword(name, project)) return false
        return IDENTIFIER_PATTERN.matches(name)
    }
    
    /**
     * Checks if the given string is an OpenSCAD keyword.
     */
    override fun isKeyword(name: String, project: Project?): Boolean {
        return name in KEYWORDS
    }
}
