package org.openscad.lexer

import com.intellij.psi.TokenType
import org.openscad.psi.OpenSCADTypes

/**
 * Lightweight parser validator that simulates parsing and detects syntax errors.
 * This works without requiring the full IntelliJ platform infrastructure.
 */
data class ValidationError(val offset: Int, val message: String)

object LightweightParserValidator {
    
    fun validate(code: String): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()
        val lexer = OpenSCADLexerImpl()
        lexer.reset(code, 0, code.length, 0)
        
        val tokens = mutableListOf<Token>()
        var tokenType = lexer.advance()
        while (tokenType != null) {
            if (tokenType != TokenType.WHITE_SPACE && 
                tokenType != OpenSCADTypes.LINE_COMMENT && 
                tokenType != OpenSCADTypes.BLOCK_COMMENT) {
                tokens.add(Token(tokenType, lexer.yytext().toString(), lexer.tokenStart))
            }
            if (tokenType == TokenType.BAD_CHARACTER) {
                errors.add(ValidationError(lexer.tokenStart, "Bad character: '${lexer.yytext()}'"))
            }
            tokenType = lexer.advance()
        }
        
        // Validate token structure
        val parser = TokenParser(tokens, errors)
        parser.parseProgram()
        
        return errors
    }
    
    private data class Token(val type: com.intellij.psi.tree.IElementType, val text: String, val offset: Int)
    
    private class TokenParser(
        private val tokens: List<Token>,
        private val errors: MutableList<ValidationError>
    ) {
        private var pos = 0
        
        private fun current(): Token? = tokens.getOrNull(pos)
        private fun currentType() = current()?.type
        private fun advance() { pos++ }
        private fun lookAhead(n: Int) = tokens.getOrNull(pos + n)?.type
        
        private fun expect(type: com.intellij.psi.tree.IElementType, message: String): Boolean {
            if (currentType() == type) {
                advance()
                return true
            }
            current()?.let { errors.add(ValidationError(it.offset, message)) }
            return false
        }
        
        fun parseProgram() {
            while (current() != null) {
                if (!parseStatement()) {
                    current()?.let { errors.add(ValidationError(it.offset, "Expected statement, got '${it.text}'")) }
                    advance()
                }
            }
        }
        
        private fun parseStatement(): Boolean {
            return when (currentType()) {
                OpenSCADTypes.SEMICOLON -> { advance(); true }
                OpenSCADTypes.MODULE_KW -> parseModuleDeclaration()
                OpenSCADTypes.FUNCTION_KW -> parseFunctionDeclaration()
                OpenSCADTypes.INCLUDE_KW -> parseIncludeStatement()
                OpenSCADTypes.USE_KW -> parseUseStatement()
                OpenSCADTypes.IF_KW -> parseIfStatement()
                OpenSCADTypes.FOR_KW -> parseForStatement()
                OpenSCADTypes.INTERSECTION_FOR_KW -> parseIntersectionForStatement()
                OpenSCADTypes.LET_KW -> parseLetStatement()
                OpenSCADTypes.ASSERT_KW -> parseAssertStatement()
                OpenSCADTypes.ECHO_KW -> parseEchoStatement()
                OpenSCADTypes.HASH, OpenSCADTypes.NOT, OpenSCADTypes.MOD, OpenSCADTypes.MUL -> 
                    parseModuleInstantiation()
                OpenSCADTypes.IDENT -> {
                    if (lookAhead(1) == OpenSCADTypes.EQ) {
                        parseAssignmentStatement()
                    } else {
                        parseModuleInstantiation()
                    }
                }
                OpenSCADTypes.NUMBER -> {
                    // Could be digit-starting identifier: assignment "2D = value" or module call "2Dpipe(...)"
                    if (lookAhead(1) == OpenSCADTypes.IDENT) {
                        when (lookAhead(2)) {
                            OpenSCADTypes.EQ -> parseAssignmentStatement()
                            OpenSCADTypes.LPAREN, OpenSCADTypes.LBRACE, OpenSCADTypes.SEMICOLON -> parseModuleInstantiation()
                            else -> return false
                        }
                    } else {
                        return false
                    }
                }
                OpenSCADTypes.LBRACE -> parseBlock()
                else -> false
            }
        }
        
        private fun parseModuleDeclaration(): Boolean {
            advance() // 'module'
            // Module name can be:
            // 1. IDENT (standard)
            // 2. NUMBER IDENT (digit-starting like "2Dpipe")
            if (currentType() == OpenSCADTypes.NUMBER && lookAhead(1) == OpenSCADTypes.IDENT) {
                advance() // number part
                advance() // ident part
            } else if (currentType() == OpenSCADTypes.IDENT) {
                advance()
            } else {
                current()?.let { errors.add(ValidationError(it.offset, "Expected module name")) }
                return false
            }
            expect(OpenSCADTypes.LPAREN, "Expected '('")
            if (currentType() != OpenSCADTypes.RPAREN) {
                parseParameterList()
            }
            expect(OpenSCADTypes.RPAREN, "Expected ')'")
            parseStatement()
            return true
        }
        
        private fun parseFunctionDeclaration(): Boolean {
            advance() // 'function'
            // Function name can be:
            // 1. IDENT (standard)
            // 2. NUMBER IDENT (digit-starting like "5gon")
            if (currentType() == OpenSCADTypes.NUMBER && lookAhead(1) == OpenSCADTypes.IDENT) {
                advance() // number part
                advance() // ident part
            } else if (currentType() == OpenSCADTypes.IDENT) {
                advance()
            } else {
                current()?.let { errors.add(ValidationError(it.offset, "Expected function name")) }
                return false
            }
            expect(OpenSCADTypes.LPAREN, "Expected '('")
            if (currentType() != OpenSCADTypes.RPAREN) {
                parseParameterList()
            }
            expect(OpenSCADTypes.RPAREN, "Expected ')'")
            expect(OpenSCADTypes.EQ, "Expected '='")
            parseExpression()
            expect(OpenSCADTypes.SEMICOLON, "Expected ';'")
            return true
        }
        
        private fun parseParameterList() {
            parseParameter()
            while (currentType() == OpenSCADTypes.COMMA) {
                advance()
                parseParameter()
            }
        }
        
        private fun parseParameter() {
            // Parameter name can be:
            // 1. IDENT (standard)
            // 2. NUMBER IDENT (digit-starting like "2D")
            if (currentType() == OpenSCADTypes.NUMBER && lookAhead(1) == OpenSCADTypes.IDENT) {
                advance() // number part
                advance() // ident part
                if (currentType() == OpenSCADTypes.EQ) {
                    advance()
                    parseExpression()
                }
            } else if (currentType() == OpenSCADTypes.IDENT) {
                advance()
                if (currentType() == OpenSCADTypes.EQ) {
                    advance()
                    parseExpression()
                }
            }
        }
        
        private fun parseIncludeStatement(): Boolean {
            advance() // 'include'
            expect(OpenSCADTypes.LT, "Expected '<'")
            // Skip until >
            while (currentType() != null && currentType() != OpenSCADTypes.GT) {
                advance()
            }
            expect(OpenSCADTypes.GT, "Expected '>'")
            return true
        }
        
        private fun parseUseStatement(): Boolean {
            advance() // 'use'
            expect(OpenSCADTypes.LT, "Expected '<'")
            // Skip until >
            while (currentType() != null && currentType() != OpenSCADTypes.GT) {
                advance()
            }
            expect(OpenSCADTypes.GT, "Expected '>'")
            return true
        }
        
        private fun parseIfStatement(): Boolean {
            advance() // 'if'
            expect(OpenSCADTypes.LPAREN, "Expected '('")
            parseExpression()
            expect(OpenSCADTypes.RPAREN, "Expected ')'")
            parseStatement()
            if (currentType() == OpenSCADTypes.ELSE_KW) {
                advance()
                parseStatement()
            }
            return true
        }
        
        private fun parseForStatement(): Boolean {
            advance() // 'for'
            expect(OpenSCADTypes.LPAREN, "Expected '('")
            parseForBindings()
            expect(OpenSCADTypes.RPAREN, "Expected ')'")
            parseStatement()
            return true
        }
        
        private fun parseIntersectionForStatement(): Boolean {
            advance() // 'intersection_for'
            expect(OpenSCADTypes.LPAREN, "Expected '('")
            parseForBindings()
            expect(OpenSCADTypes.RPAREN, "Expected ')'")
            parseStatement()
            return true
        }
        
        private fun parseForBindings() {
            parseForBinding()
            while (currentType() == OpenSCADTypes.COMMA) {
                advance()
                // Allow trailing comma - don't parse binding if we hit closing paren
                if (currentType() == OpenSCADTypes.RPAREN) break
                parseForBinding()
            }
        }
        
        private fun parseForBinding() {
            // Two forms:
            // 1. IDENT = expr (standard: for(i=range))
            // 2. expr (range only: for([0:10]))
            if (currentType() == OpenSCADTypes.IDENT && lookAhead(1) == OpenSCADTypes.EQ) {
                advance() // identifier
                advance() // =
                parseExpression()
            } else {
                // Just an expression (range)
                parseExpression()
            }
        }
        
        private fun parseLetStatement(): Boolean {
            advance() // 'let'
            expect(OpenSCADTypes.LPAREN, "Expected '('")
            parseAssignments()
            expect(OpenSCADTypes.RPAREN, "Expected ')'")
            parseStatement()
            return true
        }
        
        private fun parseAssignments() {
            parseAssignment()
            while (currentType() == OpenSCADTypes.COMMA) {
                advance()
                // Allow trailing comma - don't parse assignment if we hit closing paren
                if (currentType() == OpenSCADTypes.RPAREN) break
                parseAssignment()
            }
        }
        
        private fun parseAssignment() {
            // Variable name can be:
            // 1. IDENT (standard)
            // 2. NUMBER IDENT (digit-starting like "2D")
            if (currentType() == OpenSCADTypes.NUMBER && lookAhead(1) == OpenSCADTypes.IDENT) {
                advance() // number part
                advance() // ident part
                expect(OpenSCADTypes.EQ, "Expected '='")
                parseExpression()
            } else if (currentType() == OpenSCADTypes.IDENT) {
                advance()
                expect(OpenSCADTypes.EQ, "Expected '='")
                parseExpression()
            }
        }
        
        private fun parseAssertStatement(): Boolean {
            advance() // 'assert'
            expect(OpenSCADTypes.LPAREN, "Expected '('")
            parseArgumentList()
            expect(OpenSCADTypes.RPAREN, "Expected ')'")
            // assert can be followed by statement or semicolon
            if (currentType() == OpenSCADTypes.SEMICOLON) {
                advance()
            } else if (canStartStatement()) {
                parseStatement()
            }
            return true
        }
        
        private fun parseEchoStatement(): Boolean {
            advance() // 'echo'
            expect(OpenSCADTypes.LPAREN, "Expected '('")
            parseArgumentList()
            expect(OpenSCADTypes.RPAREN, "Expected ')'")
            // echo can be followed by statement or semicolon
            if (currentType() == OpenSCADTypes.SEMICOLON) {
                advance()
            } else if (canStartStatement()) {
                parseStatement()
            }
            return true
        }
        
        private fun parseModuleInstantiation(): Boolean {
            // Skip modifiers
            while (currentType() in setOf(OpenSCADTypes.HASH, OpenSCADTypes.NOT, OpenSCADTypes.MOD, OpenSCADTypes.MUL)) {
                advance()
            }
            
            // Module name can be: IDENT, NUMBER+IDENT (digit-starting), or control flow keywords
            if (currentType() == OpenSCADTypes.NUMBER && lookAhead(1) == OpenSCADTypes.IDENT) {
                advance() // number part
                advance() // ident part
            } else if (currentType() == OpenSCADTypes.IDENT) {
                advance()
            } else if (currentType() in setOf(OpenSCADTypes.IF_KW, OpenSCADTypes.FOR_KW, OpenSCADTypes.LET_KW, OpenSCADTypes.INTERSECTION_FOR_KW)) {
                advance()
            } else {
                return false
            }
            
            if (currentType() == OpenSCADTypes.LPAREN) {
                advance()
                if (currentType() != OpenSCADTypes.RPAREN) {
                    parseArgumentList()
                }
                expect(OpenSCADTypes.RPAREN, "Expected ')'")
            }
            
            // Children: block, statement, or semicolon
            when (currentType()) {
                OpenSCADTypes.LBRACE -> parseBlock()
                OpenSCADTypes.SEMICOLON -> advance()
                else -> if (canStartStatement()) parseStatement()
            }
            
            return true
        }
        
        private fun parseAssignmentStatement(): Boolean {
            // Variable name can be:
            // 1. IDENT (standard)
            // 2. NUMBER IDENT (digit-starting like "2D")
            if (currentType() == OpenSCADTypes.NUMBER && lookAhead(1) == OpenSCADTypes.IDENT) {
                advance() // number part
                advance() // ident part
            } else {
                advance() // identifier
            }
            expect(OpenSCADTypes.EQ, "Expected '='")
            parseExpression()
            expect(OpenSCADTypes.SEMICOLON, "Expected ';'")
            return true
        }
        
        private fun parseBlock(): Boolean {
            expect(OpenSCADTypes.LBRACE, "Expected '{'")
            while (currentType() != null && currentType() != OpenSCADTypes.RBRACE) {
                if (!parseStatement()) {
                    current()?.let { errors.add(ValidationError(it.offset, "Expected statement in block")) }
                    advance()
                }
            }
            expect(OpenSCADTypes.RBRACE, "Expected '}'")
            return true
        }
        
        private fun parseArgumentList() {
            if (currentType() == OpenSCADTypes.RPAREN) return
            parseArgument()
            while (currentType() == OpenSCADTypes.COMMA) {
                advance()
                // Allow trailing comma - don't parse argument if we hit closing paren
                if (currentType() == OpenSCADTypes.RPAREN) break
                parseArgument()
            }
        }
        
        private fun parseArgument() {
            // Check for named argument patterns:
            // 1. IDENT = expr (standard: name=value)
            // 2. NUMBER IDENT = expr (digit-starting: 2D=value)
            // 3. NUMBER = expr (numeric param name: 0=value)
            if (currentType() == OpenSCADTypes.IDENT && lookAhead(1) == OpenSCADTypes.EQ) {
                advance() // name
                advance() // =
            } else if (currentType() == OpenSCADTypes.NUMBER) {
                // Could be digit-starting parameter name like "2D=value" or "0=value"
                if (lookAhead(1) == OpenSCADTypes.IDENT && lookAhead(2) == OpenSCADTypes.EQ) {
                    advance() // number part (e.g., "2")
                    advance() // ident part (e.g., "D")
                    advance() // =
                } else if (lookAhead(1) == OpenSCADTypes.EQ) {
                    advance() // number (e.g., "0")
                    advance() // =
                }
            }
            parseExpression()
        }
        
        private fun parseExpression() {
            // Handle function literals and let/assert/echo prefixes
            when (currentType()) {
                OpenSCADTypes.FUNCTION_KW -> {
                    parseFunctionLiteral()
                    return
                }
                OpenSCADTypes.LET_KW -> {
                    parseLetExpression()
                    return
                }
                OpenSCADTypes.ASSERT_KW, OpenSCADTypes.ECHO_KW -> {
                    advance()
                    expect(OpenSCADTypes.LPAREN, "Expected '('")
                    parseArgumentList()
                    expect(OpenSCADTypes.RPAREN, "Expected ')'")
                    if (canStartExpression()) {
                        parseExpression()
                    }
                    return
                }
                else -> {}
            }
            
            parseConditionalExpression()
        }
        
        private fun parseFunctionLiteral() {
            advance() // 'function'
            expect(OpenSCADTypes.LPAREN, "Expected '('")
            if (currentType() != OpenSCADTypes.RPAREN) {
                parseParameterList()
            }
            expect(OpenSCADTypes.RPAREN, "Expected ')'")
            parseExpression()
        }
        
        private fun parseLetExpression() {
            advance() // 'let'
            expect(OpenSCADTypes.LPAREN, "Expected '('")
            parseAssignments()
            expect(OpenSCADTypes.RPAREN, "Expected ')'")
            parseExpression()
        }
        
        private fun parseConditionalExpression() {
            parseLogicalOrExpression()
            if (currentType() == OpenSCADTypes.QUESTION) {
                advance()
                parseExpression()
                expect(OpenSCADTypes.COLON, "Expected ':'")
                parseExpression()
            }
        }
        
        private fun parseLogicalOrExpression() {
            parseLogicalAndExpression()
            while (currentType() == OpenSCADTypes.OR_OR) {
                advance()
                parseLogicalAndExpression()
            }
        }
        
        private fun parseLogicalAndExpression() {
            parseEqualityExpression()
            while (currentType() == OpenSCADTypes.AND_AND) {
                advance()
                parseEqualityExpression()
            }
        }
        
        private fun parseEqualityExpression() {
            parseRelationalExpression()
            while (currentType() in setOf(OpenSCADTypes.EQ_EQ, OpenSCADTypes.NOT_EQ)) {
                advance()
                parseRelationalExpression()
            }
        }
        
        private fun parseRelationalExpression() {
            parseBinaryOrExpression()
            while (currentType() in setOf(OpenSCADTypes.LT, OpenSCADTypes.LE, OpenSCADTypes.GT, OpenSCADTypes.GE)) {
                advance()
                parseBinaryOrExpression()
            }
        }
        
        private fun parseBinaryOrExpression() {
            parseBinaryAndExpression()
            while (currentType() == OpenSCADTypes.BITOR) {
                advance()
                parseBinaryAndExpression()
            }
        }
        
        private fun parseBinaryAndExpression() {
            parseShiftExpression()
            while (currentType() == OpenSCADTypes.BITAND) {
                advance()
                parseShiftExpression()
            }
        }
        
        private fun parseShiftExpression() {
            parseAdditiveExpression()
            while (currentType() in setOf(OpenSCADTypes.LSH, OpenSCADTypes.RSH)) {
                advance()
                parseAdditiveExpression()
            }
        }
        
        private fun parseAdditiveExpression() {
            parseMultiplicativeExpression()
            while (currentType() in setOf(OpenSCADTypes.PLUS, OpenSCADTypes.MINUS)) {
                advance()
                parseMultiplicativeExpression()
            }
        }
        
        private fun parseMultiplicativeExpression() {
            parsePowerExpression()
            while (currentType() in setOf(OpenSCADTypes.MUL, OpenSCADTypes.DIV, OpenSCADTypes.MOD)) {
                advance()
                parsePowerExpression()
            }
        }
        
        private fun parsePowerExpression() {
            parseUnaryExpression()
            while (currentType() == OpenSCADTypes.POW) {
                advance()
                parseUnaryExpression()
            }
        }
        
        private fun parseUnaryExpression() {
            if (currentType() in setOf(OpenSCADTypes.PLUS, OpenSCADTypes.MINUS, OpenSCADTypes.NOT, OpenSCADTypes.BITNOT)) {
                advance()
                parseUnaryExpression()
            } else {
                parsePostfixExpression()
            }
        }
        
        private fun parsePostfixExpression() {
            parsePrimaryExpression()
            while (true) {
                when (currentType()) {
                    OpenSCADTypes.LPAREN -> {
                        advance()
                        if (currentType() != OpenSCADTypes.RPAREN) {
                            parseArgumentList()
                        }
                        expect(OpenSCADTypes.RPAREN, "Expected ')'")
                    }
                    OpenSCADTypes.LBRACKET -> {
                        advance()
                        parseExpression()
                        expect(OpenSCADTypes.RBRACKET, "Expected ']'")
                    }
                    OpenSCADTypes.DOT -> {
                        advance()
                        if (currentType() == OpenSCADTypes.IDENT) {
                            advance()
                        }
                    }
                    else -> break
                }
            }
        }
        
        private fun parsePrimaryExpression() {
            when (currentType()) {
                OpenSCADTypes.NUMBER -> {
                    advance()
                    // Check if this is a digit-starting identifier like "2D"
                    if (currentType() == OpenSCADTypes.IDENT) {
                        advance() // consume the identifier part
                    }
                }
                OpenSCADTypes.STRING, OpenSCADTypes.BOOL_LITERAL, 
                OpenSCADTypes.UNDEF_LITERAL, OpenSCADTypes.IDENT -> advance()
                OpenSCADTypes.LPAREN -> {
                    advance()
                    parseExpression()
                    expect(OpenSCADTypes.RPAREN, "Expected ')'")
                }
                OpenSCADTypes.LBRACKET -> parseVectorOrRange()
                else -> {
                    current()?.let { errors.add(ValidationError(it.offset, "Expected expression, got '${it.text}'")) }
                }
            }
        }
        
        private fun parseVectorOrRange() {
            advance() // '['
            if (currentType() == OpenSCADTypes.RBRACKET) {
                advance()
                return
            }
            
            // Parse first element or check for generators
            parseVectorElement()
            
            // Check for range (..)
            if (currentType() == OpenSCADTypes.COLON || currentType() == OpenSCADTypes.DOT_DOT) {
                advance()
                parseExpression()
                if (currentType() == OpenSCADTypes.COLON || currentType() == OpenSCADTypes.DOT_DOT) {
                    advance()
                    parseExpression()
                }
            } else {
                // Regular vector/list
                while (currentType() == OpenSCADTypes.COMMA) {
                    advance()
                    if (currentType() == OpenSCADTypes.RBRACKET) break
                    parseVectorElement()
                }
            }
            
            expect(OpenSCADTypes.RBRACKET, "Expected ']'")
        }
        
        private fun parseVectorElement() {
            when (currentType()) {
                OpenSCADTypes.IF_KW -> {
                    advance()
                    expect(OpenSCADTypes.LPAREN, "Expected '('")
                    parseExpression()
                    expect(OpenSCADTypes.RPAREN, "Expected ')'")
                    parseVectorElement()
                    if (currentType() == OpenSCADTypes.ELSE_KW) {
                        advance()
                        parseVectorElement()
                    }
                }
                OpenSCADTypes.FOR_KW -> {
                    advance()
                    expect(OpenSCADTypes.LPAREN, "Expected '('")
                    parseForBindings()
                    expect(OpenSCADTypes.RPAREN, "Expected ')'")
                    parseVectorElement()
                }
                OpenSCADTypes.LET_KW -> {
                    advance()
                    expect(OpenSCADTypes.LPAREN, "Expected '('")
                    parseAssignments()
                    expect(OpenSCADTypes.RPAREN, "Expected ')'")
                    parseVectorElement()
                }
                OpenSCADTypes.EACH_KW -> {
                    advance()
                    parseVectorElement()
                }
                else -> parseExpression()
            }
        }
        
        private fun canStartStatement(): Boolean {
            return currentType() in setOf(
                OpenSCADTypes.SEMICOLON, OpenSCADTypes.MODULE_KW, OpenSCADTypes.FUNCTION_KW,
                OpenSCADTypes.INCLUDE_KW, OpenSCADTypes.USE_KW, OpenSCADTypes.IF_KW,
                OpenSCADTypes.FOR_KW, OpenSCADTypes.INTERSECTION_FOR_KW, OpenSCADTypes.LET_KW,
                OpenSCADTypes.ASSERT_KW, OpenSCADTypes.ECHO_KW, OpenSCADTypes.HASH,
                OpenSCADTypes.NOT, OpenSCADTypes.MOD, OpenSCADTypes.MUL, OpenSCADTypes.IDENT,
                OpenSCADTypes.LBRACE
            )
        }
        
        private fun canStartExpression(): Boolean {
            return currentType() in setOf(
                OpenSCADTypes.NUMBER, OpenSCADTypes.STRING, OpenSCADTypes.BOOL_LITERAL,
                OpenSCADTypes.UNDEF_LITERAL, OpenSCADTypes.IDENT, OpenSCADTypes.LPAREN,
                OpenSCADTypes.LBRACKET, OpenSCADTypes.PLUS, OpenSCADTypes.MINUS,
                OpenSCADTypes.NOT, OpenSCADTypes.BITNOT, OpenSCADTypes.LET_KW,
                OpenSCADTypes.FUNCTION_KW, OpenSCADTypes.ASSERT_KW, OpenSCADTypes.ECHO_KW
            )
        }
    }
}
