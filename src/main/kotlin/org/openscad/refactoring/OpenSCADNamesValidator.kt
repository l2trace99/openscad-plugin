package org.openscad.refactoring

import com.intellij.lang.refactoring.NamesValidator
import com.intellij.openapi.project.Project

class OpenSCADNamesValidator : NamesValidator {
    
    override fun isKeyword(name: String, project: Project?): Boolean {
        return KEYWORDS.contains(name)
    }
    
    override fun isIdentifier(name: String, project: Project?): Boolean {
        if (name.isEmpty()) return false
        // OpenSCAD identifiers start with letter or underscore, followed by letters, digits, or underscores
        val first = name[0]
        if (!first.isLetter() && first != '_' && first != '$') return false
        return name.all { it.isLetterOrDigit() || it == '_' }
    }
    
    companion object {
        private val KEYWORDS = setOf(
            "module", "function", "if", "else", "for", "let", "each",
            "intersection_for", "assign", "echo", "assert",
            "true", "false", "undef"
        )
    }
}
