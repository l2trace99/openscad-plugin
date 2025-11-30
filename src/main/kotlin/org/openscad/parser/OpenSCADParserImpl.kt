package org.openscad.parser

import com.intellij.lang.ASTNode
import com.intellij.lang.PsiBuilder
import com.intellij.lang.PsiParser
import com.intellij.psi.tree.IElementType
import org.openscad.psi.OpenSCADTypes

/**
 * Complete OpenSCAD parser implementation following the BNF grammar
 */
class OpenSCADParserImpl : PsiParser {
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
            OpenSCADTypes.HASH, OpenSCADTypes.NOT, OpenSCADTypes.MOD, OpenSCADTypes.MUL -> 
                parseModuleInstantiation(b)
            OpenSCADTypes.IDENT -> {
                // Could be assignment, module instantiation, or expression statement
                val mark = b.mark()
                b.advanceLexer()
                when (b.tokenType) {
                    OpenSCADTypes.EQ -> {
                        mark.rollbackTo()
                        parseAssignmentStatement(b)
                    }
                    OpenSCADTypes.SEMICOLON -> {
                        // Expression statement: identifier followed by semicolon (e.g., "s;")
                        mark.rollbackTo()
                        parseExpressionStatement(b)
                    }
                    else -> {
                        mark.rollbackTo()
                        parseModuleInstantiation(b)
                    }
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
        
        if (!expect(b, OpenSCADTypes.IDENT, "Expected module name")) {
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
        
        if (!expect(b, OpenSCADTypes.IDENT, "Expected function name")) {
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
        
        if (expect(b, OpenSCADTypes.IDENT, "Expected parameter name")) {
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
        
        expect(b, OpenSCADTypes.IDENT, "Expected identifier")
        expect(b, OpenSCADTypes.EQ, "Expected '='")
        parseExpression(b)
        expect(b, OpenSCADTypes.SEMICOLON, "Expected ';'")
        
        mark.done(OpenSCADTypes.ASSIGNMENT_STATEMENT)
        return true
    }
    
    // <expression_statement> ::= <expression> ";"
    // Used for standalone expressions like "s;" or "cube(10);" in module bodies
    private fun parseExpressionStatement(b: PsiBuilder): Boolean {
        val mark = b.mark()
        
        parseExpression(b)
        expect(b, OpenSCADTypes.SEMICOLON, "Expected ';'")
        
        mark.done(OpenSCADTypes.MODULE_INSTANTIATION)
        return true
    }
    
    // <module_instantiation_statement> ::= ...
    private fun parseModuleInstantiation(b: PsiBuilder): Boolean {
        val mark = b.mark()
        
        // Optional object modifier
        if (b.tokenType in setOf(OpenSCADTypes.HASH, OpenSCADTypes.NOT, OpenSCADTypes.MOD, OpenSCADTypes.MUL)) {
            b.advanceLexer()
        }
        
        // Module call or block
        if (b.tokenType == OpenSCADTypes.IDENT) {
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
                    // Could be a chained statement (another module call, if, for, etc.)
                    // Try to parse it as a child statement
                    if (b.tokenType == OpenSCADTypes.IDENT || 
                        b.tokenType == OpenSCADTypes.IF_KW ||
                        b.tokenType == OpenSCADTypes.FOR_KW ||
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
        
        expect(b, OpenSCADTypes.IDENT, "Expected module name")
        
        if (expect(b, OpenSCADTypes.LPAREN, "Expected '('")) {
            if (b.tokenType != OpenSCADTypes.RPAREN) {
                parseArgumentList(b)
            }
            expect(b, OpenSCADTypes.RPAREN, "Expected ')'")
        }
        
        mark.done(OpenSCADTypes.MODULE_CALL)
    }
    
    // <argument_list> ::= <argument> { "," <argument> }
    private fun parseArgumentList(b: PsiBuilder) {
        val mark = b.mark()
        
        do {
            parseArgument(b)
        } while (b.tokenType == OpenSCADTypes.COMMA && b.advanceLexer() != null)
        
        mark.done(OpenSCADTypes.ARGUMENT_LIST)
    }
    
    // <argument> ::= [ IDENT "=" ] <expression>
    private fun parseArgument(b: PsiBuilder) {
        val mark = b.mark()
        
        // Check for named argument
        if (b.tokenType == OpenSCADTypes.IDENT) {
            val lookAhead = b.lookAhead(1)
            if (lookAhead == OpenSCADTypes.EQ) {
                b.advanceLexer() // IDENT
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
    
    // <for_binding_list> ::= <for_binding> { "," <for_binding> }
    private fun parseForBindingList(b: PsiBuilder) {
        val mark = b.mark()
        
        parseForBinding(b)
        
        while (b.tokenType == OpenSCADTypes.COMMA) {
            b.advanceLexer() // consume comma
            parseForBinding(b)
        }
        
        mark.done(OpenSCADTypes.FOR_BINDING_LIST)
    }
    
    // <for_binding> ::= IDENT "=" <expression>
    private fun parseForBinding(b: PsiBuilder) {
        val mark = b.mark()
        
        expect(b, OpenSCADTypes.IDENT, "Expected identifier")
        expect(b, OpenSCADTypes.EQ, "Expected '='")
        parseExpression(b)
        
        mark.done(OpenSCADTypes.FOR_BINDING)
    }
    
    // <assignment_list> ::= IDENT "=" <expression> { "," IDENT "=" <expression> }
    private fun parseAssignmentList(b: PsiBuilder) {
        do {
            expect(b, OpenSCADTypes.IDENT, "Expected identifier")
            expect(b, OpenSCADTypes.EQ, "Expected '='")
            parseExpression(b)
        } while (b.tokenType == OpenSCADTypes.COMMA && b.advanceLexer() != null)
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
    
    // <relational_expression> ::= <additive_expression> { ("<" | "<=" | ">" | ">=") <additive_expression> }
    private fun parseRelationalExpression(b: PsiBuilder) {
        val mark = b.mark()
        parseAdditiveExpression(b)
        
        if (b.tokenType in setOf(OpenSCADTypes.LT, OpenSCADTypes.LE, OpenSCADTypes.GT, OpenSCADTypes.GE)) {
            do {
                b.advanceLexer()
                parseAdditiveExpression(b)
            } while (b.tokenType in setOf(OpenSCADTypes.LT, OpenSCADTypes.LE, OpenSCADTypes.GT, OpenSCADTypes.GE))
            mark.done(OpenSCADTypes.RELATIONAL_EXPRESSION)
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
    
    // <multiplicative_expression> ::= <unary_expression> { ("*" | "/" | "%") <unary_expression> }
    private fun parseMultiplicativeExpression(b: PsiBuilder) {
        val mark = b.mark()
        parseUnaryExpression(b)
        
        if (b.tokenType in setOf(OpenSCADTypes.MUL, OpenSCADTypes.DIV, OpenSCADTypes.MOD)) {
            do {
                b.advanceLexer()
                parseUnaryExpression(b)
            } while (b.tokenType in setOf(OpenSCADTypes.MUL, OpenSCADTypes.DIV, OpenSCADTypes.MOD))
            mark.done(OpenSCADTypes.MULTIPLICATIVE_EXPRESSION)
        } else {
            mark.drop()
        }
    }
    
    // <unary_expression> ::= ("+" | "-" | "!") <unary_expression> | <postfix_expression>
    private fun parseUnaryExpression(b: PsiBuilder) {
        if (b.tokenType in setOf(OpenSCADTypes.PLUS, OpenSCADTypes.MINUS, OpenSCADTypes.NOT)) {
            val mark = b.mark()
            b.advanceLexer()
            parseUnaryExpression(b)
            mark.done(OpenSCADTypes.UNARY_EXPRESSION)
        } else {
            parsePostfixExpression(b)
        }
    }
    
    // <postfix_expression> ::= <primary_expression> { postfix_op }
    private fun parsePostfixExpression(b: PsiBuilder) {
        val mark = b.mark()
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
            OpenSCADTypes.NUMBER, OpenSCADTypes.STRING, 
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
            else -> {
                b.error("Expected expression")
                mark.drop()
            }
        }
    }
    
    // <vector_literal> ::= "[" [ <expression_list> | <comprehension> | <range> ] "]"
    private fun parseVectorLiteral(b: PsiBuilder) {
        val mark = b.mark()
        b.advanceLexer() // '['
        
        if (b.tokenType != OpenSCADTypes.RBRACKET) {
            // Check if this is a list comprehension (starts with 'for' or 'if')
            if (b.tokenType == OpenSCADTypes.FOR_KW || b.tokenType == OpenSCADTypes.IF_KW) {
                // List comprehension: [for (i = ...) expr] or [if (cond) expr]
                parseListComprehension(b)
            } else {
                // Parse first expression
                parseExpression(b)
                
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
                        // Expression list: [expr, expr, ...]
                        while (b.tokenType == OpenSCADTypes.COMMA) {
                            b.advanceLexer()
                            if (b.tokenType != OpenSCADTypes.RBRACKET) {
                                parseExpression(b)
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
    
    // <list_comprehension> ::= "for" "(" IDENT "=" <expression> ")" <expression>
    //                        | "if" "(" <expression> ")" <expression>
    private fun parseListComprehension(b: PsiBuilder) {
        val mark = b.mark()
        
        // Parse for/if clauses
        while (b.tokenType == OpenSCADTypes.FOR_KW || b.tokenType == OpenSCADTypes.IF_KW) {
            if (b.tokenType == OpenSCADTypes.FOR_KW) {
                b.advanceLexer() // 'for'
                if (!expect(b, OpenSCADTypes.LPAREN, "Expected '(' after 'for'")) {
                    mark.drop()
                    return
                }
                if (!expect(b, OpenSCADTypes.IDENT, "Expected identifier")) {
                    mark.drop()
                    return
                }
                if (!expect(b, OpenSCADTypes.EQ, "Expected '='")) {
                    mark.drop()
                    return
                }
                parseExpression(b)
                if (!expect(b, OpenSCADTypes.RPAREN, "Expected ')' after for clause")) {
                    mark.drop()
                    return
                }
            } else {
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
        }
        
        // Parse the expression that generates values (e.g., [i*2, i*i, 0])
        // This should NOT consume the closing ] of the list comprehension
        if (b.tokenType != OpenSCADTypes.RBRACKET && !b.eof()) {
            parseExpression(b)
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
}
