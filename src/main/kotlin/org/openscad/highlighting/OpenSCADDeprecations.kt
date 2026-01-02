package org.openscad.highlighting

/**
 * Detects deprecated syntax patterns in OpenSCAD code.
 * Based on deprecation warnings from the OpenSCAD source code.
 */
object OpenSCADDeprecations {
    
    /**
     * Represents a deprecation warning with location and message.
     */
    data class Deprecation(
        val startOffset: Int,
        val endOffset: Int,
        val message: String,
        val replacement: String? = null
    )
    
    /**
     * Deprecated parameter names and their replacements.
     */
    private val DEPRECATED_PARAMETERS = mapOf(
        "filename" to Pair("file", "filename= is deprecated. Please use file="),
        "layername" to Pair("layer", "layername= is deprecated. Please use layer="),
        "triangles" to Pair("faces", "triangles= is deprecated. Please use faces=")
    )
    
    /**
     * Deprecated file extensions for import.
     */
    private val DEPRECATED_EXTENSIONS = mapOf(
        ".amf" to "AMF import is deprecated. Please use 3MF instead."
    )
    
    /**
     * Find all deprecations in the given OpenSCAD code.
     * Limits to one warning per line per identifier name.
     */
    fun findDeprecations(code: String): List<Deprecation> {
        val deprecations = mutableListOf<Deprecation>()
        
        // Strip only comments (not strings) for parameter and import detection
        val codeNoComments = stripComments(code)
        
        // Strip both comments and strings for identifier detection
        val codeNoCommentsOrStrings = stripCommentsAndStrings(code)
        
        // Find deprecated parameter names (e.g., filename=, layername=, triangles=)
        deprecations.addAll(findDeprecatedParameters(codeNoComments))
        
        // Find deprecated file formats in import statements (needs to see strings)
        deprecations.addAll(findDeprecatedImports(codeNoComments))
        
        // Find identifiers starting with digits (must not match inside strings)
        deprecations.addAll(findDigitStartingIdentifiers(codeNoCommentsOrStrings))
        
        // Deduplicate: one warning per line per identifier
        return deduplicateByLine(code, deprecations)
    }
    
    /**
     * Deduplicate deprecations to one per line per identifier name.
     */
    private fun deduplicateByLine(code: String, deprecations: List<Deprecation>): List<Deprecation> {
        // Build line number lookup
        val lineStarts = mutableListOf(0)
        code.forEachIndexed { index, char ->
            if (char == '\n') lineStarts.add(index + 1)
        }
        
        fun getLineNumber(offset: Int): Int {
            return lineStarts.indexOfLast { it <= offset } + 1
        }
        
        // Extract identifier from deprecation (the text being flagged)
        fun getIdentifier(dep: Deprecation): String {
            return code.substring(dep.startOffset, dep.endOffset)
        }
        
        // Track seen (line, identifier) pairs
        val seen = mutableSetOf<Pair<Int, String>>()
        
        return deprecations.filter { dep ->
            val line = getLineNumber(dep.startOffset)
            val identifier = getIdentifier(dep)
            val key = Pair(line, identifier)
            if (seen.contains(key)) {
                false
            } else {
                seen.add(key)
                true
            }
        }
    }
    
    /**
     * Strip only comments from code while preserving character positions (replace with spaces).
     */
    private fun stripComments(code: String): String {
        val result = StringBuilder(code)
        var i = 0
        var inBlockComment = false
        
        while (i < result.length) {
            if (inBlockComment) {
                if (i + 1 < result.length && result[i] == '*' && result[i + 1] == '/') {
                    result[i] = ' '
                    result[i + 1] = ' '
                    inBlockComment = false
                    i += 2
                } else {
                    if (result[i] != '\n') result[i] = ' '
                    i++
                }
            } else {
                if (i + 1 < result.length && result[i] == '/' && result[i + 1] == '*') {
                    result[i] = ' '
                    result[i + 1] = ' '
                    inBlockComment = true
                    i += 2
                } else if (i + 1 < result.length && result[i] == '/' && result[i + 1] == '/') {
                    while (i < result.length && result[i] != '\n') {
                        result[i] = ' '
                        i++
                    }
                } else {
                    i++
                }
            }
        }
        
        return result.toString()
    }
    
    /**
     * Strip comments and strings from code while preserving character positions (replace with spaces).
     */
    private fun stripCommentsAndStrings(code: String): String {
        val result = StringBuilder(code)
        var i = 0
        var inBlockComment = false
        var inString = false
        var stringChar = '"'
        
        while (i < result.length) {
            if (inBlockComment) {
                if (i + 1 < result.length && result[i] == '*' && result[i + 1] == '/') {
                    result[i] = ' '
                    result[i + 1] = ' '
                    inBlockComment = false
                    i += 2
                } else {
                    if (result[i] != '\n') result[i] = ' '
                    i++
                }
            } else if (inString) {
                // Handle escape sequences
                if (result[i] == '\\' && i + 1 < result.length) {
                    result[i] = ' '
                    result[i + 1] = ' '
                    i += 2
                } else if (result[i] == stringChar) {
                    result[i] = ' '
                    inString = false
                    i++
                } else {
                    if (result[i] != '\n') result[i] = ' '
                    i++
                }
            } else {
                if (i + 1 < result.length && result[i] == '/' && result[i + 1] == '*') {
                    result[i] = ' '
                    result[i + 1] = ' '
                    inBlockComment = true
                    i += 2
                } else if (i + 1 < result.length && result[i] == '/' && result[i + 1] == '/') {
                    // Single line comment - replace until end of line
                    while (i < result.length && result[i] != '\n') {
                        result[i] = ' '
                        i++
                    }
                } else if (result[i] == '"' || result[i] == '\'') {
                    // Start of string
                    stringChar = result[i]
                    result[i] = ' '
                    inString = true
                    i++
                } else {
                    i++
                }
            }
        }
        
        return result.toString()
    }
    
    /**
     * Find deprecated parameter names in the code.
     */
    private fun findDeprecatedParameters(code: String): List<Deprecation> {
        val deprecations = mutableListOf<Deprecation>()
        
        for ((deprecated, replacement) in DEPRECATED_PARAMETERS) {
            // Match parameter name followed by = (with optional spaces)
            val pattern = Regex("""\b($deprecated)\s*=""")
            pattern.findAll(code).forEach { match ->
                val paramGroup = match.groups[1]!!
                deprecations.add(Deprecation(
                    startOffset = paramGroup.range.first,
                    endOffset = paramGroup.range.last + 1,
                    message = replacement.second,
                    replacement = replacement.first
                ))
            }
        }
        
        return deprecations
    }
    
    /**
     * Find deprecated file formats in import statements.
     */
    private fun findDeprecatedImports(code: String): List<Deprecation> {
        val deprecations = mutableListOf<Deprecation>()
        
        // Match import statements with string literals
        val importPattern = Regex("""import\s*\(\s*(?:[^)]*["']([^"']+\.amf)["'])""", RegexOption.IGNORE_CASE)
        importPattern.findAll(code).forEach { match ->
            val filenameGroup = match.groups[1]
            if (filenameGroup != null) {
                deprecations.add(Deprecation(
                    startOffset = filenameGroup.range.first,
                    endOffset = filenameGroup.range.last + 1,
                    message = DEPRECATED_EXTENSIONS[".amf"]!!,
                    replacement = null
                ))
            }
        }
        
        return deprecations
    }
    
    /**
     * Find identifiers starting with digits used anywhere in code.
     * OpenSCAD allows this but it's deprecated and will be removed in future releases.
     * Matches: assignments (2D=x), definitions (module 2D), and usages (!2D, if(2D), etc.)
     * Excludes: scientific notation (1e-9, 2E+5, etc.)
     */
    private fun findDigitStartingIdentifiers(code: String): List<Deprecation> {
        val deprecations = mutableListOf<Deprecation>()
        
        // Match any digit-starting identifier that looks like a variable/identifier
        // Must start with digit, followed by at least one letter or underscore (to distinguish from pure numbers)
        // Negative lookbehind: not preceded by word char or dot
        // The identifier must contain at least one letter to be an identifier (not just a number)
        val identifierPattern = Regex("""(?<![.\w])(\d+[a-zA-Z_]\w*)""")
        
        // Pattern for scientific notation: digits followed by e/E and optional +/- and digits
        val scientificPattern = Regex("""^\d+[eE][+\-]?\d*$""")
        
        identifierPattern.findAll(code).forEach { match ->
            val idGroup = match.groups[1]!!
            val value = idGroup.value
            
            // Skip if this looks like scientific notation
            if (scientificPattern.matches(value)) {
                return@forEach
            }
            
            deprecations.add(Deprecation(
                startOffset = idGroup.range.first,
                endOffset = idGroup.range.last + 1,
                message = "Identifier names starting with digits (${value}) will be removed in future releases.",
                replacement = null
            ))
        }
        
        return deprecations
    }
}
