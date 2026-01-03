package org.openscad.parser

import com.intellij.lang.ASTNode
import com.intellij.lang.PsiBuilder
import com.intellij.lang.PsiBuilderFactory
import com.intellij.lang.PsiParser
import com.intellij.psi.tree.IElementType
import org.openscad.psi.OpenSCADTypes

/**
 * Simple error data class for test reporting
 */
data class ParseError(val offset: Int, val message: String)

/**
 * Complete OpenSCAD parser implementation following the BNF grammar
 */
class OpenSCADParserImpl : PsiParser {
    
    companion object {
        // Thread-local error collection for testing
        private val errorCollector = ThreadLocal<MutableList<ParseError>>()
        
        /**
         * Parse code and return list of errors (for testing)
         * Uses a lightweight token-based validation approach
         */
        fun parseAndGetErrors(code: String): List<ParseError> {
            // In test environment without full IDE context, use token validation
            return try {
                val parserDefinition = OpenSCADParserDefinition()
                val lexer = parserDefinition.createLexer(null)
                val factory = PsiBuilderFactory.getInstance()
                
                val builder = factory.createBuilder(parserDefinition, lexer, code)
                val errors = mutableListOf<ParseError>()
                errorCollector.set(errors)
                
                try {
                    val parser = OpenSCADParserImpl()
                    val tree = parser.parse(OpenSCADParserDefinition.FILE, builder)
                    
                    // Extract errors from the tree
                    fun collectErrors(node: ASTNode) {
                        if (node.elementType.toString() == "ERROR_ELEMENT" || 
                            node.elementType == com.intellij.psi.TokenType.ERROR_ELEMENT) {
                            val errorText = node.text.take(20)
                            errors.add(ParseError(node.startOffset, "Syntax error near: $errorText"))
                        }
                        node.getChildren(null).forEach { collectErrors(it) }
                    }
                    collectErrors(tree)
                } finally {
                    errorCollector.remove()
                }
                
                errors
            } catch (e: Exception) {
                // Fallback: simple token validation when not in IDE context
                validateTokens(code)
            }
        }
        
        /**
         * Simple token-based validation when PsiBuilder is unavailable
         */
        private fun validateTokens(code: String): List<ParseError> {
            val errors = mutableListOf<ParseError>()
            val lexer = org.openscad.lexer.OpenSCADLexer()
            lexer.start(code)
            
            while (lexer.tokenType != null) {
                if (lexer.tokenType == com.intellij.psi.TokenType.BAD_CHARACTER) {
                    errors.add(ParseError(lexer.tokenStart, "Bad character: ${code.substring(lexer.tokenStart, lexer.tokenEnd)}"))
                }
                lexer.advance()
            }
            return errors
        }
        
        internal fun reportError(offset: Int, message: String) {
            errorCollector.get()?.add(ParseError(offset, message))
        }
    }
    
    override fun parse(root: IElementType, builder: PsiBuilder): ASTNode {
        val marker = builder.mark()
        parseProgram(builder)
        marker.done(root)
        return builder.treeBuilt
    }
    
    // <program> ::= { <statement> }
    private fun parseProgram(b: PsiBuilder) {
        while (!b.eof()) {
            if (!parseStatement(b)) {
                b.error("Expected statement")
                b.advanceLexer()
            }
        }
    }
    
    // <statement> ::= ...
    private fun parseStatement(b: PsiBuilder): Boolean {
        return when (b.tokenType) {
            OpenSCADTypes.SEMICOLON -> {
                b.advanceLexer() // empty statement
                true
            }
            OpenSCADTypes.MODULE_KW -> parseModuleDeclaration(b)
            OpenSCADTypes.FUNCTION_KW -> parseFunctionDeclaration(b)
            OpenSCADTypes.INCLUDE_KW -> parseIncludeStatement(b)
            OpenSCADTypes.USE_KW -> parseUseStatement(b)
            OpenSCADTypes.IF_KW -> parseIfStatement(b)
            OpenSCADTypes.FOR_KW -> parseForStatement(b)
            OpenSCADTypes.INTERSECTION_FOR_KW -> parseIntersectionForStatement(b)
            OpenSCADTypes.LET_KW -> parseLetStatement(b)
            OpenSCADTypes.ASSERT_KW -> parseAssertStatement(b)
            OpenSCADTypes.ECHO_KW -> parseEchoStatement(b)
            OpenSCADTypes.HASH, OpenSCADTypes.NOT, OpenSCADTypes.MOD, OpenSCADTypes.MUL -> 
                parseModuleInstantiation(b)
            OpenSCADTypes.IDENT -> {
                // Could be assignment or module instantiation
                val mark = b.mark()
                b.advanceLexer()
                when (b.tokenType) {
                    OpenSCADTypes.EQ -> {
                        mark.rollbackTo()
                        parseAssignmentStatement(b)
                    }
                    else -> {
                        mark.rollbackTo()
                        parseModuleInstantiation(b)
                    }
                }
            }
            OpenSCADTypes.NUMBER -> {
                // Could be digit-starting identifier: assignment "2D = value" or module call "2Dpipe(...)"
                val mark = b.mark()
                b.advanceLexer()
                if (b.tokenType == OpenSCADTypes.IDENT) {
                    b.advanceLexer()
                    when (b.tokenType) {
                        OpenSCADTypes.EQ -> {
                            mark.rollbackTo()
                            parseAssignmentStatement(b)
                        }
                        OpenSCADTypes.LPAREN, OpenSCADTypes.LBRACE, OpenSCADTypes.SEMICOLON -> {
                            // Module call with digit-starting name
                            mark.rollbackTo()
                            parseModuleInstantiation(b)
                        }
                        else -> {
                            mark.rollbackTo()
                            return false
                        }
                    }
                } else {
                    mark.rollbackTo()
                    return false
                }
            }
            OpenSCADTypes.LBRACE -> parseModuleInstantiation(b)
            else -> false
        }
    }
    
    // <module_declaration> ::= "module" IDENT "(" [ <parameter_list> ] ")" ( <block> | <statement> )
    private fun parseModuleDeclaration(b: PsiBuilder): Boolean {
        val mark = b.mark()
        b.advanceLexer() // 'module'
        
        // Module name can be: IDENT or NUMBER IDENT (digit-starting like "2Dpipe")
        if (b.tokenType == OpenSCADTypes.NUMBER && b.lookAhead(1) == OpenSCADTypes.IDENT) {
            b.advanceLexer() // number part
            b.advanceLexer() // ident part
        } else if (!expect(b, OpenSCADTypes.IDENT, "Expected module name")) {
            mark.done(OpenSCADTypes.MODULE_DECLARATION)
            return true
        }
        
        if (!expect(b, OpenSCADTypes.LPAREN, "Expected '('")) {
            mark.done(OpenSCADTypes.MODULE_DECLARATION)
            return true
        }
        
        if (b.tokenType != OpenSCADTypes.RPAREN) {
            parseParameterList(b)
        }
        
        if (!expect(b, OpenSCADTypes.RPAREN, "Expected ')'")) {
            mark.done(OpenSCADTypes.MODULE_DECLARATION)
            return true
        }
        
        // Module body can be either a block or a single statement
        if (b.tokenType == OpenSCADTypes.LBRACE) {
            parseBlock(b)
        } else {
            // Single statement module body (e.g., module curve() polygon(...);)
            parseStatement(b)
        }
        
        mark.done(OpenSCADTypes.MODULE_DECLARATION)
        return true
    }
    
    // <function_declaration> ::= "function" IDENT "(" [ <parameter_list> ] ")" "=" <expression> ";"
    private fun parseFunctionDeclaration(b: PsiBuilder): Boolean {
        val mark = b.mark()
        b.advanceLexer() // 'function'
        
        // Function name can be: IDENT or NUMBER IDENT (digit-starting like "5gon")
        if (b.tokenType == OpenSCADTypes.NUMBER && b.lookAhead(1) == OpenSCADTypes.IDENT) {
            b.advanceLexer() // number part
            b.advanceLexer() // ident part
        } else if (!expect(b, OpenSCADTypes.IDENT, "Expected function name")) {
            mark.done(OpenSCADTypes.FUNCTION_DECLARATION)
            return true
        }
        
        if (!expect(b, OpenSCADTypes.LPAREN, "Expected '('")) {
            mark.done(OpenSCADTypes.FUNCTION_DECLARATION)
            return true
        }
        
        if (b.tokenType != OpenSCADTypes.RPAREN) {
            parseParameterList(b)
        }
        
        if (!expect(b, OpenSCADTypes.RPAREN, "Expected ')'")) {
            mark.done(OpenSCADTypes.FUNCTION_DECLARATION)
            return true
        }
        
        if (!expect(b, OpenSCADTypes.EQ, "Expected '='")) {
            mark.done(OpenSCADTypes.FUNCTION_DECLARATION)
            return true
        }
        
        parseExpression(b)
        expect(b, OpenSCADTypes.SEMICOLON, "Expected ';'")
        
        mark.done(OpenSCADTypes.FUNCTION_DECLARATION)
        return true
    }
    
    // <parameter_list> ::= <parameter> { "," <parameter> }
    private fun parseParameterList(b: PsiBuilder) {
        val mark = b.mark()
        
        do {
            parseParameter(b)
        } while (b.tokenType == OpenSCADTypes.COMMA && b.advanceLexer() != null)
        
        mark.done(OpenSCADTypes.PARAMETER_LIST)
    }
    
    // <parameter> ::= IDENT [ "=" <expression> ]
    private fun parseParameter(b: PsiBuilder) {
        val mark = b.mark()
        
        // Parameter name can be: IDENT or NUMBER IDENT (digit-starting like "2D")
        val hasName = if (b.tokenType == OpenSCADTypes.NUMBER && b.lookAhead(1) == OpenSCADTypes.IDENT) {
            b.advanceLexer() // number part
            b.advanceLexer() // ident part
            true
        } else {
            expect(b, OpenSCADTypes.IDENT, "Expected parameter name")
        }
        
        if (hasName) {
            if (b.tokenType == OpenSCADTypes.EQ) {
                b.advanceLexer()
                parseExpression(b)
            }
        }
        
        mark.done(OpenSCADTypes.PARAMETER)
    }
    
    // <block> ::= "{" { <statement> } "}"
    private fun parseBlock(b: PsiBuilder): Boolean {
        val mark = b.mark()
        
        if (!expect(b, OpenSCADTypes.LBRACE, "Expected '{'")) {
            mark.drop()
            return false
        }
        
        while (b.tokenType != OpenSCADTypes.RBRACE && !b.eof()) {
            if (!parseStatement(b)) {
                b.error("Unexpected token")
                b.advanceLexer()
            }
        }
        
        expect(b, OpenSCADTypes.RBRACE, "Expected '}'")
        mark.done(OpenSCADTypes.BLOCK)
        return true
    }
    
    // <assignment_statement> ::= IDENT "=" <expression> ";"
    private fun parseAssignmentStatement(b: PsiBuilder): Boolean {
        val mark = b.mark()
        
        // Variable name can be: IDENT or NUMBER IDENT (digit-starting like "2D")
        if (b.tokenType == OpenSCADTypes.NUMBER && b.lookAhead(1) == OpenSCADTypes.IDENT) {
            b.advanceLexer() // number part
            b.advanceLexer() // ident part
        } else {
            expect(b, OpenSCADTypes.IDENT, "Expected identifier")
        }
        expect(b, OpenSCADTypes.EQ, "Expected '='")
        parseExpression(b)
        expect(b, OpenSCADTypes.SEMICOLON, "Expected ';'")
        
        mark.done(OpenSCADTypes.ASSIGNMENT_STATEMENT)
        return true
    }
    
    // <module_instantiation_statement> ::= ...
    private fun parseModuleInstantiation(b: PsiBuilder): Boolean {
        val mark = b.mark()
        
        // Optional object modifier
        if (b.tokenType in setOf(OpenSCADTypes.HASH, OpenSCADTypes.NOT, OpenSCADTypes.MOD, OpenSCADTypes.MUL)) {
            b.advanceLexer()
        }
        
        // After modifier, can have: module call, block, if, for, let, etc.
        if (b.tokenType == OpenSCADTypes.IF_KW) {
            parseIfStatement(b)
            mark.done(OpenSCADTypes.MODULE_INSTANTIATION)
            return true
        } else if (b.tokenType == OpenSCADTypes.FOR_KW) {
            parseForStatement(b)
            mark.done(OpenSCADTypes.MODULE_INSTANTIATION)
            return true
        } else if (b.tokenType == OpenSCADTypes.LET_KW) {
            parseLetStatement(b)
            mark.done(OpenSCADTypes.MODULE_INSTANTIATION)
            return true
        } else if (b.tokenType == OpenSCADTypes.INTERSECTION_FOR_KW) {
            parseIntersectionForStatement(b)
            mark.done(OpenSCADTypes.MODULE_INSTANTIATION)
            return true
        }
        
        // Module call or block (can be IDENT or digit-starting like "2Dpipe")
        if (b.tokenType == OpenSCADTypes.IDENT || 
            (b.tokenType == OpenSCADTypes.NUMBER && b.lookAhead(1) == OpenSCADTypes.IDENT)) {
            parseModuleCall(b)
            
            // After a module call, we can have:
            // 1. A block: translate(...) { ... }
            // 2. Another statement (chained): translate(...) cube(...);
            // 3. A semicolon: cube(...);
            when (b.tokenType) {
                OpenSCADTypes.LBRACE -> {
                    // Block follows
                    parseBlock(b)
                }
                OpenSCADTypes.SEMICOLON -> {
                    // Statement terminator
                    b.advanceLexer()
                }
                else -> {
                    // Could be a chained statement (another module call, if, for, let, echo, assert, etc.)
                    // Try to parse it as a child statement
                    if (b.tokenType == OpenSCADTypes.IDENT || 
                        b.tokenType == OpenSCADTypes.IF_KW ||
                        b.tokenType == OpenSCADTypes.FOR_KW ||
                        b.tokenType == OpenSCADTypes.LET_KW ||
                        b.tokenType == OpenSCADTypes.ECHO_KW ||
                        b.tokenType == OpenSCADTypes.ASSERT_KW ||
                        b.tokenType == OpenSCADTypes.INTERSECTION_FOR_KW ||
                        b.tokenType == OpenSCADTypes.NUMBER ||  // digit-starting identifier
                        b.tokenType in setOf(OpenSCADTypes.HASH, OpenSCADTypes.NOT, OpenSCADTypes.MOD, OpenSCADTypes.MUL)) {
                        // Parse the chained statement
                        parseStatement(b)
                    } else {
                        // Not a valid continuation, expect semicolon
                        expect(b, OpenSCADTypes.SEMICOLON, "Expected ';', '{', or another statement")
                    }
                }
            }
        } else if (b.tokenType == OpenSCADTypes.LBRACE) {
            parseBlock(b)
        } else {
            b.error("Expected module call or block")
            mark.drop()
            return false
        }
        
        mark.done(OpenSCADTypes.MODULE_INSTANTIATION)
        return true
    }
    
    // <module_call> ::= IDENT "(" [ <argument_list> ] ")"
    private fun parseModuleCall(b: PsiBuilder) {
        val mark = b.mark()
        
        // Module name can be: IDENT or NUMBER IDENT (digit-starting like "2Dpipe")
        if (b.tokenType == OpenSCADTypes.NUMBER && b.lookAhead(1) == OpenSCADTypes.IDENT) {
            b.advanceLexer() // number part
            b.advanceLexer() // ident part
        } else {
            expect(b, OpenSCADTypes.IDENT, "Expected module name")
        }
        
        if (expect(b, OpenSCADTypes.LPAREN, "Expected '('")) {
            if (b.tokenType != OpenSCADTypes.RPAREN) {
                parseArgumentList(b)
            }
            expect(b, OpenSCADTypes.RPAREN, "Expected ')'")
        }
        
        mark.done(OpenSCADTypes.MODULE_CALL)
    }
    
    // <argument_list> ::= <argument> { "," <argument> } [ "," ]
    // Supports trailing commas
    private fun parseArgumentList(b: PsiBuilder) {
        val mark = b.mark()
        
        parseArgument(b)
        while (b.tokenType == OpenSCADTypes.COMMA) {
            b.advanceLexer()
            // Allow trailing comma - don't parse argument if we hit closing paren
            if (b.tokenType == OpenSCADTypes.RPAREN) break
            parseArgument(b)
        }
        
        mark.done(OpenSCADTypes.ARGUMENT_LIST)
    }
    
    // <argument> ::= [ IDENT "=" ] <expression>
    // Also handles OpenSCAD quirk where parameter names can start with digits (e.g., 2D=value)
    private fun parseArgument(b: PsiBuilder) {
        val mark = b.mark()
        
        // Check for named argument: IDENT = expr
        if (b.tokenType == OpenSCADTypes.IDENT) {
            val lookAhead = b.lookAhead(1)
            if (lookAhead == OpenSCADTypes.EQ) {
                b.advanceLexer() // IDENT
                b.advanceLexer() // =
            }
        }
        // Handle parameter names starting with digits: NUMBER IDENT = expr (e.g., 2D=value)
        else if (b.tokenType == OpenSCADTypes.NUMBER) {
            val lookAhead1 = b.lookAhead(1)
            val lookAhead2 = b.lookAhead(2)
            if (lookAhead1 == OpenSCADTypes.IDENT && lookAhead2 == OpenSCADTypes.EQ) {
                b.advanceLexer() // NUMBER (e.g., "2")
                b.advanceLexer() // IDENT (e.g., "D")
                b.advanceLexer() // =
            }
        }
        
        parseExpression(b)
        mark.done(OpenSCADTypes.ARGUMENT)
    }
    
    // <include_statement> ::= "include" "<" path ">" ";"
    // Path can be: identifier, identifier/identifier, identifier-identifier, etc.
    private fun parseIncludeStatement(b: PsiBuilder): Boolean {
        val mark = b.mark()
        b.advanceLexer() // 'include'
        
        if (!expect(b, OpenSCADTypes.LT, "Expected '<'")) {
            mark.done(OpenSCADTypes.INCLUDE_STATEMENT)
            return true
        }
        
        // Parse path - consume all tokens until we hit '>'
        // Path can contain: identifiers, slashes, dashes, dots, etc.
        // These get tokenized as IDENT, DIV, MINUS, etc.
        val pathMark = b.mark()
        var hasPath = false
        while (b.tokenType != null && b.tokenType != OpenSCADTypes.GT) {
            b.advanceLexer()
            hasPath = true
        }
        
        if (hasPath) {
            pathMark.done(OpenSCADTypes.STRING) // Mark as STRING for compatibility
        } else {
            pathMark.drop() // No path found
        }
        
        // Now we should be at '>'
        if (b.tokenType == OpenSCADTypes.GT) {
            b.advanceLexer() // consume '>'
        } else {
            b.error("Expected '>'")
        }
        
        // Semicolon is optional for include statements
        if (b.tokenType == OpenSCADTypes.SEMICOLON) {
            b.advanceLexer() // consume ';' if present
        }
        
        mark.done(OpenSCADTypes.INCLUDE_STATEMENT)
        return true
    }
    
    // <use_statement> ::= "use" "<" path ">"
    // Path can be: identifier, identifier/identifier, identifier-identifier, etc.
    // Semicolon is optional
    private fun parseUseStatement(b: PsiBuilder): Boolean {
        val mark = b.mark()
        b.advanceLexer() // 'use'
        
        if (!expect(b, OpenSCADTypes.LT, "Expected '<'")) {
            mark.done(OpenSCADTypes.USE_STATEMENT)
            return true
        }
        
        // Parse path - consume all tokens until we hit '>'
        // Path can contain: identifiers, slashes, dashes, dots, etc.
        // These get tokenized as IDENT, DIV, MINUS, etc.
        val pathMark = b.mark()
        var hasPath = false
        while (b.tokenType != null && b.tokenType != OpenSCADTypes.GT) {
            b.advanceLexer()
            hasPath = true
        }
        
        if (hasPath) {
            pathMark.done(OpenSCADTypes.STRING) // Mark as STRING for compatibility
        } else {
            pathMark.drop() // No path found
        }
        
        // Now we should be at '>'
        if (b.tokenType == OpenSCADTypes.GT) {
            b.advanceLexer() // consume '>'
        } else {
            b.error("Expected '>'")
        }
        
        // Semicolon is optional for use statements
        if (b.tokenType == OpenSCADTypes.SEMICOLON) {
            b.advanceLexer() // consume ';' if present
        }
        
        mark.done(OpenSCADTypes.USE_STATEMENT)
        return true
    }
    
    // <if_statement> ::= "if" "(" <expression> ")" <block_or_single_statement> [ "else" <block_or_single_statement> ]
    private fun parseIfStatement(b: PsiBuilder): Boolean {
        val mark = b.mark()
        b.advanceLexer() // 'if'
        
        expect(b, OpenSCADTypes.LPAREN, "Expected '('")
        parseExpression(b)
        expect(b, OpenSCADTypes.RPAREN, "Expected ')'")
        
        parseBlockOrSingleStatement(b)
        
        if (b.tokenType == OpenSCADTypes.ELSE_KW) {
            b.advanceLexer()
            parseBlockOrSingleStatement(b)
        }
        
        mark.done(OpenSCADTypes.IF_STATEMENT)
        return true
    }
    
    // <for_statement> ::= "for" "(" <for_binding_list> ")" <block_or_single_statement>
    private fun parseForStatement(b: PsiBuilder): Boolean {
        val mark = b.mark()
        b.advanceLexer() // 'for'
        
        expect(b, OpenSCADTypes.LPAREN, "Expected '('")
        parseForBindingList(b)
        expect(b, OpenSCADTypes.RPAREN, "Expected ')'")
        
        parseBlockOrSingleStatement(b)
        
        mark.done(OpenSCADTypes.FOR_STATEMENT)
        return true
    }
    
    // <intersection_for_statement> ::= "intersection_for" "(" <for_binding_list> ")" <block_or_single_statement>
    private fun parseIntersectionForStatement(b: PsiBuilder): Boolean {
        val mark = b.mark()
        b.advanceLexer() // 'intersection_for'
        
        expect(b, OpenSCADTypes.LPAREN, "Expected '('")
        parseForBindingList(b)
        expect(b, OpenSCADTypes.RPAREN, "Expected ')'")
        
        parseBlockOrSingleStatement(b)
        
        mark.done(OpenSCADTypes.INTERSECTION_FOR_STATEMENT)
        return true
    }
    
    // <let_statement> ::= "let" "(" <assignment_list> ")" <block_or_single_statement>
    private fun parseLetStatement(b: PsiBuilder): Boolean {
        val mark = b.mark()
        b.advanceLexer() // 'let'
        
        expect(b, OpenSCADTypes.LPAREN, "Expected '('")
        parseAssignmentList(b)
        expect(b, OpenSCADTypes.RPAREN, "Expected ')'")
        
        parseBlockOrSingleStatement(b)
        
        mark.done(OpenSCADTypes.LET_STATEMENT)
        return true
    }
    
    // <assert_statement> ::= "assert" "(" <expression> [ "," <expression> ] ")" <statement>
    private fun parseAssertStatement(b: PsiBuilder): Boolean {
        val mark = b.mark()
        b.advanceLexer() // 'assert'
        
        expect(b, OpenSCADTypes.LPAREN, "Expected '('")
        parseExpression(b) // condition
        
        // Optional message
        if (b.tokenType == OpenSCADTypes.COMMA) {
            b.advanceLexer()
            parseExpression(b) // message
        }
        
        expect(b, OpenSCADTypes.RPAREN, "Expected ')'")
        
        // Assert can be followed by a statement or just semicolon
        if (b.tokenType == OpenSCADTypes.SEMICOLON) {
            b.advanceLexer()
        } else if (b.tokenType != OpenSCADTypes.RBRACE && !b.eof()) {
            parseStatement(b)
        }
        
        mark.done(OpenSCADTypes.ASSERT_STATEMENT)
        return true
    }
    
    // <echo_statement> ::= "echo" "(" <argument_list> ")" [ <statement> | ";" ]
    private fun parseEchoStatement(b: PsiBuilder): Boolean {
        val mark = b.mark()
        b.advanceLexer() // 'echo'
        
        expect(b, OpenSCADTypes.LPAREN, "Expected '('")
        if (b.tokenType != OpenSCADTypes.RPAREN) {
            parseArgumentList(b)
        }
        expect(b, OpenSCADTypes.RPAREN, "Expected ')'")
        
        // Echo can be followed by a statement or just semicolon
        if (b.tokenType == OpenSCADTypes.SEMICOLON) {
            b.advanceLexer()
        } else if (b.tokenType != OpenSCADTypes.RBRACE && !b.eof()) {
            parseStatement(b)
        }
        
        mark.done(OpenSCADTypes.ASSERT_STATEMENT) // Reuse ASSERT_STATEMENT type for now
        return true
    }
    
    // <for_binding_list> ::= <for_binding> { "," <for_binding> } [ "," ]
    // Supports trailing commas
    private fun parseForBindingList(b: PsiBuilder) {
        val mark = b.mark()
        
        parseForBinding(b)
        
        while (b.tokenType == OpenSCADTypes.COMMA) {
            b.advanceLexer() // consume comma
            // Allow trailing comma - don't parse binding if we hit closing paren
            if (b.tokenType == OpenSCADTypes.RPAREN) break
            parseForBinding(b)
        }
        
        mark.done(OpenSCADTypes.FOR_BINDING_LIST)
    }
    
    // <for_binding> ::= IDENT "=" <expression> | <expression>
    // Supports both forms: for(i=[0:10]) and for([0:10])
    private fun parseForBinding(b: PsiBuilder) {
        val mark = b.mark()
        
        // Check for IDENT = expr or NUMBER IDENT = expr pattern
        if (b.tokenType == OpenSCADTypes.IDENT && b.lookAhead(1) == OpenSCADTypes.EQ) {
            b.advanceLexer() // IDENT
            b.advanceLexer() // =
            parseExpression(b)
        } else if (b.tokenType == OpenSCADTypes.NUMBER && b.lookAhead(1) == OpenSCADTypes.IDENT && b.lookAhead(2) == OpenSCADTypes.EQ) {
            b.advanceLexer() // NUMBER
            b.advanceLexer() // IDENT
            b.advanceLexer() // =
            parseExpression(b)
        } else {
            // Just an expression (range without variable binding)
            parseExpression(b)
        }
        
        mark.done(OpenSCADTypes.FOR_BINDING)
    }
    
    // <assignment_list> ::= IDENT "=" <expression> { "," IDENT "=" <expression> } [ "," ]
    // Supports trailing commas
    private fun parseAssignmentList(b: PsiBuilder) {
        // Parse first assignment
        parseAssignmentInList(b)
        
        while (b.tokenType == OpenSCADTypes.COMMA) {
            b.advanceLexer()
            // Allow trailing comma - don't parse assignment if we hit closing paren
            if (b.tokenType == OpenSCADTypes.RPAREN) break
            parseAssignmentInList(b)
        }
    }
    
    private fun parseAssignmentInList(b: PsiBuilder) {
        // Variable name can be: IDENT or NUMBER IDENT (digit-starting like "2D")
        if (b.tokenType == OpenSCADTypes.NUMBER && b.lookAhead(1) == OpenSCADTypes.IDENT) {
            b.advanceLexer() // number part
            b.advanceLexer() // ident part
        } else {
            expect(b, OpenSCADTypes.IDENT, "Expected identifier")
        }
        expect(b, OpenSCADTypes.EQ, "Expected '='")
        parseExpression(b)
    }
    
    private fun parseBlockOrSingleStatement(b: PsiBuilder) {
        if (b.tokenType == OpenSCADTypes.LBRACE) {
            parseBlock(b)
        } else {
            parseStatement(b)
        }
    }
    
    // Expression parsing with precedence
    
    // <expression> ::= <conditional_expression>
    private fun parseExpression(b: PsiBuilder) {
        parseConditionalExpression(b)
    }
    
    // <conditional_expression> ::= <logical_or_expression> [ "?" <expression> ":" <expression> ]
    private fun parseConditionalExpression(b: PsiBuilder) {
        val mark = b.mark()
        parseLogicalOrExpression(b)
        
        if (b.tokenType == OpenSCADTypes.QUESTION) {
            b.advanceLexer()
            parseExpression(b)
            expect(b, OpenSCADTypes.COLON, "Expected ':'")
            parseExpression(b)
            mark.done(OpenSCADTypes.CONDITIONAL_EXPRESSION)
        } else {
            mark.drop()
        }
    }
    
    // <logical_or_expression> ::= <logical_and_expression> { "||" <logical_and_expression> }
    private fun parseLogicalOrExpression(b: PsiBuilder) {
        val mark = b.mark()
        parseLogicalAndExpression(b)
        
        if (b.tokenType == OpenSCADTypes.OR_OR) {
            do {
                b.advanceLexer()
                parseLogicalAndExpression(b)
            } while (b.tokenType == OpenSCADTypes.OR_OR)
            mark.done(OpenSCADTypes.LOGICAL_OR_EXPRESSION)
        } else {
            mark.drop()
        }
    }
    
    // <logical_and_expression> ::= <equality_expression> { "&&" <equality_expression> }
    private fun parseLogicalAndExpression(b: PsiBuilder) {
        val mark = b.mark()
        parseEqualityExpression(b)
        
        if (b.tokenType == OpenSCADTypes.AND_AND) {
            do {
                b.advanceLexer()
                parseEqualityExpression(b)
            } while (b.tokenType == OpenSCADTypes.AND_AND)
            mark.done(OpenSCADTypes.LOGICAL_AND_EXPRESSION)
        } else {
            mark.drop()
        }
    }
    
    // <equality_expression> ::= <relational_expression> { ("==" | "!=") <relational_expression> }
    private fun parseEqualityExpression(b: PsiBuilder) {
        val mark = b.mark()
        parseRelationalExpression(b)
        
        if (b.tokenType in setOf(OpenSCADTypes.EQ_EQ, OpenSCADTypes.NOT_EQ)) {
            do {
                b.advanceLexer()
                parseRelationalExpression(b)
            } while (b.tokenType in setOf(OpenSCADTypes.EQ_EQ, OpenSCADTypes.NOT_EQ))
            mark.done(OpenSCADTypes.EQUALITY_EXPRESSION)
        } else {
            mark.drop()
        }
    }
    
    // <relational_expression> ::= <binary_or_expression> { ("<" | "<=" | ">" | ">=") <binary_or_expression> }
    private fun parseRelationalExpression(b: PsiBuilder) {
        val mark = b.mark()
        parseBinaryOrExpression(b)
        
        if (b.tokenType in setOf(OpenSCADTypes.LT, OpenSCADTypes.LE, OpenSCADTypes.GT, OpenSCADTypes.GE)) {
            do {
                b.advanceLexer()
                parseBinaryOrExpression(b)
            } while (b.tokenType in setOf(OpenSCADTypes.LT, OpenSCADTypes.LE, OpenSCADTypes.GT, OpenSCADTypes.GE))
            mark.done(OpenSCADTypes.RELATIONAL_EXPRESSION)
        } else {
            mark.drop()
        }
    }
    
    // <binary_or_expression> ::= <binary_and_expression> { "|" <binary_and_expression> }
    private fun parseBinaryOrExpression(b: PsiBuilder) {
        val mark = b.mark()
        parseBinaryAndExpression(b)
        
        if (b.tokenType == OpenSCADTypes.BITOR) {
            do {
                b.advanceLexer()
                parseBinaryAndExpression(b)
            } while (b.tokenType == OpenSCADTypes.BITOR)
            mark.done(OpenSCADTypes.BINARY_OR_EXPRESSION)
        } else {
            mark.drop()
        }
    }
    
    // <binary_and_expression> ::= <shift_expression> { "&" <shift_expression> }
    private fun parseBinaryAndExpression(b: PsiBuilder) {
        val mark = b.mark()
        parseShiftExpression(b)
        
        if (b.tokenType == OpenSCADTypes.BITAND) {
            do {
                b.advanceLexer()
                parseShiftExpression(b)
            } while (b.tokenType == OpenSCADTypes.BITAND)
            mark.done(OpenSCADTypes.BINARY_AND_EXPRESSION)
        } else {
            mark.drop()
        }
    }
    
    // <shift_expression> ::= <additive_expression> { ("<<" | ">>") <additive_expression> }
    private fun parseShiftExpression(b: PsiBuilder) {
        val mark = b.mark()
        parseAdditiveExpression(b)
        
        if (b.tokenType in setOf(OpenSCADTypes.LSH, OpenSCADTypes.RSH)) {
            do {
                b.advanceLexer()
                parseAdditiveExpression(b)
            } while (b.tokenType in setOf(OpenSCADTypes.LSH, OpenSCADTypes.RSH))
            mark.done(OpenSCADTypes.SHIFT_EXPRESSION)
        } else {
            mark.drop()
        }
    }
    
    // <additive_expression> ::= <multiplicative_expression> { ("+" | "-") <multiplicative_expression> }
    private fun parseAdditiveExpression(b: PsiBuilder) {
        val mark = b.mark()
        parseMultiplicativeExpression(b)
        
        if (b.tokenType in setOf(OpenSCADTypes.PLUS, OpenSCADTypes.MINUS)) {
            do {
                b.advanceLexer()
                parseMultiplicativeExpression(b)
            } while (b.tokenType in setOf(OpenSCADTypes.PLUS, OpenSCADTypes.MINUS))
            mark.done(OpenSCADTypes.ADDITIVE_EXPRESSION)
        } else {
            mark.drop()
        }
    }
    
    // <multiplicative_expression> ::= <power_expression> { ("*" | "/" | "%") <power_expression> }
    private fun parseMultiplicativeExpression(b: PsiBuilder) {
        val mark = b.mark()
        parsePowerExpression(b)
        
        if (b.tokenType in setOf(OpenSCADTypes.MUL, OpenSCADTypes.DIV, OpenSCADTypes.MOD)) {
            do {
                b.advanceLexer()
                parsePowerExpression(b)
            } while (b.tokenType in setOf(OpenSCADTypes.MUL, OpenSCADTypes.DIV, OpenSCADTypes.MOD))
            mark.done(OpenSCADTypes.MULTIPLICATIVE_EXPRESSION)
        } else {
            mark.drop()
        }
    }
    
    // <power_expression> ::= <unary_expression> { "^" <unary_expression> }
    private fun parsePowerExpression(b: PsiBuilder) {
        val mark = b.mark()
        parseUnaryExpression(b)
        
        if (b.tokenType == OpenSCADTypes.POW) {
            do {
                b.advanceLexer()
                parseUnaryExpression(b)
            } while (b.tokenType == OpenSCADTypes.POW)
            mark.done(OpenSCADTypes.MULTIPLICATIVE_EXPRESSION) // Reuse element type
        } else {
            mark.drop()
        }
    }
    
    // <unary_expression> ::= ("+" | "-" | "!" | "~") <unary_expression> | <postfix_expression>
    private fun parseUnaryExpression(b: PsiBuilder) {
        if (b.tokenType in setOf(OpenSCADTypes.PLUS, OpenSCADTypes.MINUS, OpenSCADTypes.NOT, OpenSCADTypes.BITNOT)) {
            val mark = b.mark()
            b.advanceLexer()
            parseUnaryExpression(b)
            mark.done(OpenSCADTypes.UNARY_EXPRESSION)
        } else {
            parsePostfixExpression(b)
        }
    }
    
    // <postfix_expression> ::= <primary_expression> { postfix_op }
    // Also handles echo/assert as expression prefixes: echo(...) expr, assert(...) expr
    private fun parsePostfixExpression(b: PsiBuilder) {
        val mark = b.mark()
        
        // Check if this is echo or assert used as expression prefix
        // Both 'echo' and 'assert' are keywords (ECHO_KW, ASSERT_KW)
        val isExpressionPrefix = b.tokenType == OpenSCADTypes.ECHO_KW ||
            b.tokenType == OpenSCADTypes.ASSERT_KW
        
        parsePrimaryExpression(b)
        
        var hasPostfix = false
        while (true) {
            when (b.tokenType) {
                OpenSCADTypes.LPAREN -> {
                    // Function call
                    b.advanceLexer()
                    if (b.tokenType != OpenSCADTypes.RPAREN) {
                        parseArgumentList(b)
                    }
                    expect(b, OpenSCADTypes.RPAREN, "Expected ')'")
                    hasPostfix = true
                    
                    // If this was echo(...) or assert(...), check if followed by another expression
                    // echo(x) y means "echo x, return y"
                    if (isExpressionPrefix && canStartExpression(b.tokenType)) {
                        parseExpression(b)
                    }
                }
                OpenSCADTypes.LBRACKET -> {
                    // Index or slice
                    b.advanceLexer()
                    parseExpression(b)
                    
                    if (b.tokenType == OpenSCADTypes.COLON) {
                        // Slice
                        b.advanceLexer()
                        parseExpression(b)
                        
                        if (b.tokenType == OpenSCADTypes.COLON) {
                            b.advanceLexer()
                            parseExpression(b)
                        }
                    }
                    
                    expect(b, OpenSCADTypes.RBRACKET, "Expected ']'")
                    hasPostfix = true
                }
                OpenSCADTypes.DOT -> {
                    // Member access: expr.x, expr.y, expr.z
                    b.advanceLexer()
                    expect(b, OpenSCADTypes.IDENT, "Expected identifier after '.'")
                    hasPostfix = true
                }
                else -> break
            }
        }
        
        if (hasPostfix) {
            mark.done(OpenSCADTypes.POSTFIX_EXPRESSION)
        } else {
            mark.drop()
        }
    }
    
    // <primary_expression> ::= literal | IDENT | "(" <expression> ")" | <vector_literal> | <let_expression>
    private fun parsePrimaryExpression(b: PsiBuilder) {
        val mark = b.mark()
        
        when (b.tokenType) {
            OpenSCADTypes.NUMBER -> {
                b.advanceLexer()
                // Check if this is a digit-starting identifier like "2D"
                if (b.tokenType == OpenSCADTypes.IDENT) {
                    b.advanceLexer() // consume the identifier part
                }
                mark.done(OpenSCADTypes.PRIMARY_EXPRESSION)
            }
            OpenSCADTypes.STRING, 
            OpenSCADTypes.BOOL_LITERAL, OpenSCADTypes.UNDEF_LITERAL -> {
                b.advanceLexer()
                mark.done(OpenSCADTypes.PRIMARY_EXPRESSION)
            }
            OpenSCADTypes.IDENT -> {
                b.advanceLexer()
                mark.done(OpenSCADTypes.PRIMARY_EXPRESSION)
            }
            OpenSCADTypes.LPAREN -> {
                b.advanceLexer()
                parseExpression(b)
                expect(b, OpenSCADTypes.RPAREN, "Expected ')'")
                mark.done(OpenSCADTypes.PRIMARY_EXPRESSION)
            }
            OpenSCADTypes.LBRACKET -> {
                parseVectorLiteral(b)
                mark.drop()
            }
            OpenSCADTypes.LET_KW -> {
                parseLetExpression(b)
                mark.drop()
            }
            OpenSCADTypes.FUNCTION_KW -> {
                // Function literal: function(params) expression
                parseFunctionLiteral(b)
                mark.drop()
            }
            OpenSCADTypes.ASSERT_KW, OpenSCADTypes.ECHO_KW -> {
                // Assert/echo as expression prefix: assert(cond) expr, echo(msg) expr
                // Just advance past keyword, the postfix handler will parse the call and continuation
                b.advanceLexer()
                mark.done(OpenSCADTypes.PRIMARY_EXPRESSION)
            }
            else -> {
                b.error("Expected expression")
                mark.drop()
            }
        }
    }
    
    // <function_literal> ::= "function" "(" [ <parameter_list> ] ")" <expression>
    private fun parseFunctionLiteral(b: PsiBuilder) {
        val mark = b.mark()
        b.advanceLexer() // 'function'
        
        if (!expect(b, OpenSCADTypes.LPAREN, "Expected '('")) {
            mark.done(OpenSCADTypes.FUNCTION_DECLARATION)
            return
        }
        
        if (b.tokenType != OpenSCADTypes.RPAREN) {
            parseParameterList(b)
        }
        
        if (!expect(b, OpenSCADTypes.RPAREN, "Expected ')'")) {
            mark.done(OpenSCADTypes.FUNCTION_DECLARATION)
            return
        }
        
        // Function literal body is an expression (not = expression ;)
        parseExpression(b)
        
        mark.done(OpenSCADTypes.FUNCTION_DECLARATION)
    }
    
    // <vector_literal> ::= "[" [ <expression_list> | <comprehension> | <range> ] "]"
    // Also handles generator expressions: [if(cond) expr, if(cond) expr, ...]
    private fun parseVectorLiteral(b: PsiBuilder) {
        val mark = b.mark()
        b.advanceLexer() // '['
        
        if (b.tokenType != OpenSCADTypes.RBRACKET) {
            // Check if this is a list comprehension (starts with 'for' or 'let')
            // Note: 'each' and 'if' are handled as vector elements since [each x, y] is valid
            if (b.tokenType == OpenSCADTypes.FOR_KW || 
                b.tokenType == OpenSCADTypes.LET_KW) {
                // List comprehension: [for (i = ...) expr] or [let(...) expr]
                parseListComprehension(b)
            } else {
                // Parse first element (could be expression or generator expression)
                parseVectorElement(b)
                
                // Check what follows
                when (b.tokenType) {
                    OpenSCADTypes.COLON -> {
                        // Range: [start:end] or [start:step:end]
                        b.advanceLexer() // consume ':'
                        parseExpression(b) // end value
                        
                        if (b.tokenType == OpenSCADTypes.COLON) {
                            // [start:step:end] - we actually parsed step, now get end
                            b.advanceLexer() // consume second ':'
                            parseExpression(b) // end value
                        }
                        
                        // Complete the range literal
                        expect(b, OpenSCADTypes.RBRACKET, "Expected ']'")
                        mark.done(OpenSCADTypes.RANGE_LITERAL)
                        return
                    }
                    OpenSCADTypes.COMMA -> {
                        // Expression list: [elem, elem, ...]
                        while (b.tokenType == OpenSCADTypes.COMMA) {
                            b.advanceLexer()
                            if (b.tokenType != OpenSCADTypes.RBRACKET) {
                                parseVectorElement(b)
                            }
                        }
                    }
                    // else: single expression vector [expr]
                }
            }
        }
        
        expect(b, OpenSCADTypes.RBRACKET, "Expected ']'")
        mark.done(OpenSCADTypes.VECTOR_LITERAL)
    }
    
    // Parse a vector element - can be a generator expression or regular expression
    // Generator: if(cond) expr, for(i=range) expr, let(assignments) expr, each expr
    private fun parseVectorElement(b: PsiBuilder) {
        when (b.tokenType) {
            OpenSCADTypes.IF_KW -> {
                // Generator expression: if(cond) expr [else expr]
                b.advanceLexer() // 'if'
                expect(b, OpenSCADTypes.LPAREN, "Expected '('")
                parseExpression(b)
                expect(b, OpenSCADTypes.RPAREN, "Expected ')'")
                parseVectorElement(b) // The value to include (can be nested)
                // Handle optional else clause
                if (b.tokenType == OpenSCADTypes.ELSE_KW) {
                    b.advanceLexer() // 'else'
                    parseVectorElement(b)
                }
            }
            OpenSCADTypes.FOR_KW -> {
                // Generator expression: for(i=range) expr or for(range) expr
                b.advanceLexer() // 'for'
                expect(b, OpenSCADTypes.LPAREN, "Expected '('")
                // Parse for bindings (can be multiple, comma-separated)
                parseForBindingList(b)
                expect(b, OpenSCADTypes.RPAREN, "Expected ')'")
                parseVectorElement(b) // can chain with let, if, etc.
            }
            OpenSCADTypes.LET_KW -> {
                // Generator expression: let(assignments) expr
                b.advanceLexer() // 'let'
                expect(b, OpenSCADTypes.LPAREN, "Expected '('")
                parseAssignmentList(b)
                expect(b, OpenSCADTypes.RPAREN, "Expected ')'")
                parseVectorElement(b)
            }
            OpenSCADTypes.EACH_KW -> {
                // Generator expression: each expr (can chain with for, if, let, or another each)
                b.advanceLexer() // 'each'
                parseVectorElement(b)
            }
            else -> {
                // Regular expression
                parseExpression(b)
            }
        }
    }
    
    // <list_comprehension> ::= "for" "(" IDENT "=" <expression> ")" <expression>
    //                        | "if" "(" <expression> ")" <expression>
    //                        | "let" "(" <assignment_list> ")" <expression>
    //                        | "each" <expression>
    private fun parseListComprehension(b: PsiBuilder) {
        val mark = b.mark()
        
        // Parse for/if/let/each clauses
        while (b.tokenType == OpenSCADTypes.FOR_KW || b.tokenType == OpenSCADTypes.IF_KW ||
               b.tokenType == OpenSCADTypes.LET_KW || b.tokenType == OpenSCADTypes.EACH_KW) {
            when (b.tokenType) {
                OpenSCADTypes.FOR_KW -> {
                    b.advanceLexer() // 'for'
                    if (!expect(b, OpenSCADTypes.LPAREN, "Expected '(' after 'for'")) {
                        mark.drop()
                        return
                    }
                    // Use parseForBindingList to handle all for binding patterns
                    parseForBindingList(b)
                    if (!expect(b, OpenSCADTypes.RPAREN, "Expected ')' after for clause")) {
                        mark.drop()
                        return
                    }
                }
                OpenSCADTypes.IF_KW -> {
                    b.advanceLexer() // 'if'
                    if (!expect(b, OpenSCADTypes.LPAREN, "Expected '(' after 'if'")) {
                        mark.drop()
                        return
                    }
                    parseExpression(b)
                    if (!expect(b, OpenSCADTypes.RPAREN, "Expected ')' after if condition")) {
                        mark.drop()
                        return
                    }
                }
                OpenSCADTypes.LET_KW -> {
                    b.advanceLexer() // 'let'
                    if (!expect(b, OpenSCADTypes.LPAREN, "Expected '(' after 'let'")) {
                        mark.drop()
                        return
                    }
                    parseAssignmentList(b)
                    if (!expect(b, OpenSCADTypes.RPAREN, "Expected ')' after let assignments")) {
                        mark.drop()
                        return
                    }
                }
                OpenSCADTypes.EACH_KW -> {
                    b.advanceLexer() // 'each'
                    // 'each' can be followed by for/if/let/each or an expression
                    // Continue the while loop if followed by another generator keyword
                    if (b.tokenType in setOf(OpenSCADTypes.FOR_KW, OpenSCADTypes.IF_KW, 
                                             OpenSCADTypes.LET_KW, OpenSCADTypes.EACH_KW)) {
                        // Continue parsing chained generators
                        continue
                    }
                    // Otherwise parse as vector element (handles [vector] or (expr) after each)
                    parseVectorElement(b)
                    // Break after parsing expression - don't try to parse another one
                    break
                }
                else -> break
            }
        }
        
        // Parse the expression that generates values
        if (b.tokenType != OpenSCADTypes.RBRACKET && !b.eof()) {
            parseVectorElement(b)
        }
        
        // Handle else clause for if in comprehension: [for(...) if(cond) expr else expr]
        if (b.tokenType == OpenSCADTypes.ELSE_KW) {
            b.advanceLexer() // 'else'
            parseVectorElement(b)
        }
        
        // Handle trailing elements after comprehension: [for(i=...) expr, [x,0], [0,0]]
        while (b.tokenType == OpenSCADTypes.COMMA) {
            b.advanceLexer()
            if (b.tokenType != OpenSCADTypes.RBRACKET) {
                parseVectorElement(b)
            }
        }
        
        mark.done(OpenSCADTypes.LIST_COMPREHENSION)
    }
    
    // <comprehension_clause> ::= "for" IDENT "=" <expression> | "if" <expression>
    private fun parseComprehensionClauses(b: PsiBuilder) {
        while (b.tokenType == OpenSCADTypes.FOR_KW || b.tokenType == OpenSCADTypes.IF_KW) {
            val mark = b.mark()
            
            if (b.tokenType == OpenSCADTypes.FOR_KW) {
                b.advanceLexer()
                expect(b, OpenSCADTypes.IDENT, "Expected identifier")
                expect(b, OpenSCADTypes.EQ, "Expected '='")
                parseExpression(b)
            } else {
                b.advanceLexer() // 'if'
                parseExpression(b)
            }
            
            mark.done(OpenSCADTypes.COMPREHENSION_CLAUSE)
        }
    }
    
    // <let_expression> ::= "let" "(" <assignment_list> ")" <expression>
    private fun parseLetExpression(b: PsiBuilder) {
        val mark = b.mark()
        b.advanceLexer() // 'let'
        
        expect(b, OpenSCADTypes.LPAREN, "Expected '('")
        parseAssignmentList(b)
        expect(b, OpenSCADTypes.RPAREN, "Expected ')'")
        
        parseExpression(b)
        
        mark.done(OpenSCADTypes.LET_EXPRESSION)
    }
    
    // Helper functions
    
    private fun expect(b: PsiBuilder, type: IElementType, message: String): Boolean {
        if (b.tokenType == type) {
            b.advanceLexer()
            return true
        }
        b.error(message)
        return false
    }
    
    // Check if token can start an expression (for echo/assert prefix handling)
    private fun canStartExpression(tokenType: IElementType?): Boolean {
        return tokenType in setOf(
            OpenSCADTypes.NUMBER,
            OpenSCADTypes.STRING,
            OpenSCADTypes.BOOL_LITERAL,
            OpenSCADTypes.UNDEF_LITERAL,
            OpenSCADTypes.IDENT,
            OpenSCADTypes.LPAREN,
            OpenSCADTypes.LBRACKET,
            OpenSCADTypes.PLUS,
            OpenSCADTypes.MINUS,
            OpenSCADTypes.NOT,
            OpenSCADTypes.BITNOT,
            OpenSCADTypes.LET_KW,
            OpenSCADTypes.FUNCTION_KW,
            OpenSCADTypes.ASSERT_KW,
            OpenSCADTypes.ECHO_KW
        )
    }
}
