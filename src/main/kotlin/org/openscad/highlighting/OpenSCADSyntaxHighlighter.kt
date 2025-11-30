package org.openscad.highlighting

import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.tree.IElementType
import org.openscad.lexer.OpenSCADLexer
import org.openscad.psi.OpenSCADTypes

class OpenSCADSyntaxHighlighter : SyntaxHighlighterBase() {
    override fun getHighlightingLexer(): Lexer = OpenSCADLexer()

    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> {
        return when (tokenType) {
            // Keywords
            OpenSCADTypes.MODULE_KW, OpenSCADTypes.FUNCTION_KW,
            OpenSCADTypes.INCLUDE_KW, OpenSCADTypes.USE_KW,
            OpenSCADTypes.IF_KW, OpenSCADTypes.ELSE_KW,
            OpenSCADTypes.FOR_KW, OpenSCADTypes.INTERSECTION_FOR_KW,
            OpenSCADTypes.LET_KW -> pack(KEYWORD)
            
            // Literals
            OpenSCADTypes.NUMBER -> pack(NUMBER)
            OpenSCADTypes.STRING -> pack(STRING)
            OpenSCADTypes.BOOL_LITERAL -> pack(KEYWORD)
            OpenSCADTypes.UNDEF_LITERAL -> pack(KEYWORD)
            
            // Comments
            OpenSCADTypes.LINE_COMMENT, OpenSCADTypes.BLOCK_COMMENT -> pack(COMMENT)
            
            // Operators
            OpenSCADTypes.EQ, OpenSCADTypes.EQ_EQ, OpenSCADTypes.NOT_EQ,
            OpenSCADTypes.LT, OpenSCADTypes.LE, OpenSCADTypes.GT, OpenSCADTypes.GE,
            OpenSCADTypes.AND_AND, OpenSCADTypes.OR_OR, OpenSCADTypes.NOT,
            OpenSCADTypes.PLUS, OpenSCADTypes.MINUS, OpenSCADTypes.MUL,
            OpenSCADTypes.DIV, OpenSCADTypes.MOD, OpenSCADTypes.POW,
            OpenSCADTypes.QUESTION, OpenSCADTypes.COLON -> pack(OPERATOR)
            
            // Object modifiers
            OpenSCADTypes.HASH, OpenSCADTypes.MOD, OpenSCADTypes.MUL -> pack(OPERATOR)
            
            // Punctuation
            OpenSCADTypes.LPAREN, OpenSCADTypes.RPAREN,
            OpenSCADTypes.LBRACE, OpenSCADTypes.RBRACE,
            OpenSCADTypes.LBRACKET, OpenSCADTypes.RBRACKET,
            OpenSCADTypes.SEMICOLON, OpenSCADTypes.COMMA -> pack(PARENTHESES)
            
            // Identifiers (including special variables starting with $)
            OpenSCADTypes.IDENT -> pack(IDENTIFIER)
            
            else -> emptyArray()
        }
    }

    companion object {
        val KEYWORD = TextAttributesKey.createTextAttributesKey(
            "OPENSCAD_KEYWORD",
            DefaultLanguageHighlighterColors.KEYWORD
        )
        
        val NUMBER = TextAttributesKey.createTextAttributesKey(
            "OPENSCAD_NUMBER",
            DefaultLanguageHighlighterColors.NUMBER
        )
        
        val STRING = TextAttributesKey.createTextAttributesKey(
            "OPENSCAD_STRING",
            DefaultLanguageHighlighterColors.STRING
        )
        
        val COMMENT = TextAttributesKey.createTextAttributesKey(
            "OPENSCAD_COMMENT",
            DefaultLanguageHighlighterColors.LINE_COMMENT
        )
        
        val OPERATOR = TextAttributesKey.createTextAttributesKey(
            "OPENSCAD_OPERATOR",
            DefaultLanguageHighlighterColors.OPERATION_SIGN
        )
        
        val PARENTHESES = TextAttributesKey.createTextAttributesKey(
            "OPENSCAD_PARENTHESES",
            DefaultLanguageHighlighterColors.PARENTHESES
        )
        
        val IDENTIFIER = TextAttributesKey.createTextAttributesKey(
            "OPENSCAD_IDENTIFIER",
            DefaultLanguageHighlighterColors.IDENTIFIER
        )
    }
}
