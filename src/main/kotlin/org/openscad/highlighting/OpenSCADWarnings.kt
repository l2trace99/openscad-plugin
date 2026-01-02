package org.openscad.highlighting

/**
 * Detects warning patterns in OpenSCAD code.
 * Based on warning messages from the OpenSCAD source code.
 */
object OpenSCADWarnings {
    
    /**
     * Represents a warning with location and message.
     */
    data class Warning(
        val startOffset: Int,
        val endOffset: Int,
        val message: String
    )
    
    /**
     * Find all warnings in the given OpenSCAD code.
     */
    fun findWarnings(code: String): List<Warning> {
        val warnings = mutableListOf<Warning>()
        
        // Find variable reassignment warnings
        warnings.addAll(findVariableReassignments(code))
        
        return warnings
    }
    
    /**
     * Find variables that are assigned multiple times at the same scope level.
     * Tracks scope using:
     * - module/function declarations create new scopes (not bare {} blocks)
     * - Properly handles multiline parameter lists
     * - Skips named parameters in module/function calls (inside parentheses)
     * - Ignores commented code (// and /* */)
     * 
     * Note: OpenSCAD bare {} blocks don't create variable scopes like modules do.
     * Variables assigned anywhere at file level are global, even inside {} fold blocks.
     */
    private fun findVariableReassignments(code: String): List<Warning> {
        val warnings = mutableListOf<Warning>()
        
        // Use unique scope IDs for module/function bodies only
        // Map of (scopeId, varName) -> (lineNumber, offset)
        val assignments = mutableMapOf<Pair<Int, String>, Pair<Int, Int>>()
        
        // Stack of (scopeId, braceDepthWhenEntered)
        // Only push when entering a module/function body
        val scopeStack = mutableListOf(Pair(0, 0))  // Start with global scope (id=0) at brace depth 0
        var nextScopeId = 1
        
        // Track brace depth to know when we exit a module/function
        var braceDepth = 0
        
        // Track parentheses depth globally - any assignment inside () is a named parameter
        var parenDepth = 0
        
        // Track if we're inside a block comment
        var inBlockComment = false
        
        // Track if next { starts a module/function body
        var nextBraceIsScope = false
        
        val lines = code.split("\n")
        var currentOffset = 0
        
        // Patterns
        val assignmentPattern = Regex("""^\s*(\w+)\s*=\s*[^=]""")
        val moduleOrFunctionDefPattern = Regex("""^\s*(module|function)\s+\w+\s*\(""")
        
        for ((index, line) in lines.withIndex()) {
            val lineNumber = index + 1
            
            // Strip comments from line for analysis
            var processedLine = line
            val wasInBlockComment = inBlockComment
            
            // Handle block comments
            if (inBlockComment) {
                val endIdx = line.indexOf("*/")
                if (endIdx >= 0) {
                    inBlockComment = false
                    processedLine = line.substring(endIdx + 2)
                } else {
                    processedLine = ""  // Entire line is inside block comment
                }
            }
            
            // Check for new block comment start (after handling existing block comment)
            if (!inBlockComment) {
                val startIdx = processedLine.indexOf("/*")
                if (startIdx >= 0) {
                    val endIdx = processedLine.indexOf("*/", startIdx + 2)
                    if (endIdx >= 0) {
                        // Block comment starts and ends on same line
                        processedLine = processedLine.substring(0, startIdx) + processedLine.substring(endIdx + 2)
                    } else {
                        // Block comment continues to next line
                        inBlockComment = true
                        processedLine = processedLine.substring(0, startIdx)
                    }
                }
            }
            
            // Remove single-line comments
            val singleCommentIdx = processedLine.indexOf("//")
            if (singleCommentIdx >= 0) {
                processedLine = processedLine.substring(0, singleCommentIdx)
            }
            
            // Calculate paren depth at the START of the line (before any changes on this line)
            val startingParenDepth = parenDepth
            
            // Check if this line declares a module/function (next { will start a scope)
            if (moduleOrFunctionDefPattern.containsMatchIn(processedLine)) {
                nextBraceIsScope = true
            }
            
            // Count braces to track module/function scope changes
            val openBraces = processedLine.count { it == '{' }
            val closeBraces = processedLine.count { it == '}' }
            
            // Process opening braces - only create new scope for module/function bodies
            repeat(openBraces) {
                braceDepth++
                if (nextBraceIsScope) {
                    // This brace starts a module/function body - create new scope
                    scopeStack.add(Pair(nextScopeId++, braceDepth))
                    nextBraceIsScope = false
                }
            }
            
            // Only check for reassignments if NOT inside parentheses and not in block comment
            // This excludes: module/function definitions, module calls with named params
            if (startingParenDepth == 0 && !wasInBlockComment) {
                val match = assignmentPattern.find(processedLine)
                
                if (match != null) {
                    val varName = match.groupValues[1]
                    
                    // Skip keywords that look like assignments
                    if (varName in setOf("module", "function", "if", "else", "for", "let", "each")) {
                        // Not a variable assignment
                    } else {
                        val currentScopeId = scopeStack.last().first
                        val key = Pair(currentScopeId, varName)
                        
                        // Calculate offset of variable name in original code
                        val varOffset = currentOffset + match.groups[1]!!.range.first
                        
                        if (assignments.containsKey(key)) {
                            val (originalLine, _) = assignments[key]!!
                            warnings.add(Warning(
                                startOffset = varOffset,
                                endOffset = varOffset + varName.length,
                                message = "'$varName' was assigned on line $originalLine but was overwritten"
                            ))
                        } else {
                            assignments[key] = Pair(lineNumber, varOffset)
                        }
                    }
                }
            }
            
            // Update paren depth for next line (track all parens on processed line)
            for (char in processedLine) {
                when (char) {
                    '(' -> parenDepth++
                    ')' -> parenDepth = maxOf(0, parenDepth - 1)
                }
            }
            
            // Process closing braces - pop scope if we're exiting a module/function body
            repeat(closeBraces) {
                // Check if this closing brace ends a module/function scope
                if (scopeStack.size > 1 && scopeStack.last().second == braceDepth) {
                    val (exitingScope, _) = scopeStack.removeAt(scopeStack.lastIndex)
                    // Remove all variables from the exiting scope
                    val keysToRemove = assignments.keys.filter { it.first == exitingScope }
                    keysToRemove.forEach { assignments.remove(it) }
                }
                braceDepth = maxOf(0, braceDepth - 1)
            }
            
            currentOffset += line.length + 1 // +1 for newline
        }
        
        return warnings
    }
}
