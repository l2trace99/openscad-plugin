package org.openscad.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType

/**
 * Token types and element types for OpenSCAD language
 */
object OpenSCADTypes {
    // Keywords
    @JvmField val MODULE_KW = OpenSCADTokenType("MODULE")
    @JvmField val FUNCTION_KW = OpenSCADTokenType("FUNCTION")
    @JvmField val INCLUDE_KW = OpenSCADTokenType("INCLUDE")
    @JvmField val USE_KW = OpenSCADTokenType("USE")
    @JvmField val IF_KW = OpenSCADTokenType("IF")
    @JvmField val ELSE_KW = OpenSCADTokenType("ELSE")
    @JvmField val FOR_KW = OpenSCADTokenType("FOR")
    @JvmField val INTERSECTION_FOR_KW = OpenSCADTokenType("INTERSECTION_FOR")
    @JvmField val LET_KW = OpenSCADTokenType("LET")
    @JvmField val EACH_KW = OpenSCADTokenType("EACH")
    @JvmField val ASSERT_KW = OpenSCADTokenType("ASSERT")
    @JvmField val ECHO_KW = OpenSCADTokenType("ECHO")
    
    // Literals
    @JvmField val NUMBER = OpenSCADTokenType("NUMBER")
    @JvmField val STRING = OpenSCADTokenType("STRING")
    @JvmField val BOOL_LITERAL = OpenSCADTokenType("BOOL_LITERAL")
    @JvmField val UNDEF_LITERAL = OpenSCADTokenType("UNDEF_LITERAL")
    @JvmField val IDENT = OpenSCADTokenType("IDENT")
    
    // Operators
    @JvmField val EQ = OpenSCADTokenType("EQ")              // =
    @JvmField val EQ_EQ = OpenSCADTokenType("EQ_EQ")        // ==
    @JvmField val NOT_EQ = OpenSCADTokenType("NOT_EQ")      // !=
    @JvmField val LT = OpenSCADTokenType("LT")              // <
    @JvmField val LE = OpenSCADTokenType("LE")              // <=
    @JvmField val GT = OpenSCADTokenType("GT")              // >
    @JvmField val GE = OpenSCADTokenType("GE")              // >=
    @JvmField val AND_AND = OpenSCADTokenType("AND_AND")    // &&
    @JvmField val OR_OR = OpenSCADTokenType("OR_OR")        // ||
    @JvmField val NOT = OpenSCADTokenType("NOT")            // !
    @JvmField val BITNOT = OpenSCADTokenType("BITNOT")      // ~
    @JvmField val BITOR = OpenSCADTokenType("BITOR")        // |
    @JvmField val BITAND = OpenSCADTokenType("BITAND")      // &
    @JvmField val LSH = OpenSCADTokenType("LSH")            // <<
    @JvmField val RSH = OpenSCADTokenType("RSH")            // >>
    @JvmField val QUESTION = OpenSCADTokenType("QUESTION")  // ?
    @JvmField val COLON = OpenSCADTokenType("COLON")        // :
    @JvmField val DOT = OpenSCADTokenType("DOT")            // .
    @JvmField val DOT_DOT = OpenSCADTokenType("DOT_DOT")    // ..
    @JvmField val PLUS = OpenSCADTokenType("PLUS")          // +
    @JvmField val MINUS = OpenSCADTokenType("MINUS")        // -
    @JvmField val MUL = OpenSCADTokenType("MUL")            // *
    @JvmField val DIV = OpenSCADTokenType("DIV")            // /
    @JvmField val MOD = OpenSCADTokenType("MOD")            // %
    @JvmField val POW = OpenSCADTokenType("POW")            // ^
    @JvmField val DOLLAR = OpenSCADTokenType("DOLLAR")      // $
    
    // Punctuation
    @JvmField val LPAREN = OpenSCADTokenType("LPAREN")      // (
    @JvmField val RPAREN = OpenSCADTokenType("RPAREN")      // )
    @JvmField val LBRACE = OpenSCADTokenType("LBRACE")      // {
    @JvmField val RBRACE = OpenSCADTokenType("RBRACE")      // }
    @JvmField val LBRACKET = OpenSCADTokenType("LBRACKET")  // [
    @JvmField val RBRACKET = OpenSCADTokenType("RBRACKET")  // ]
    @JvmField val SEMICOLON = OpenSCADTokenType("SEMICOLON")// ;
    @JvmField val COMMA = OpenSCADTokenType("COMMA")        // ,
    
    // Object modifiers
    @JvmField val HASH = OpenSCADTokenType("HASH")          // #
    @JvmField val EXCL = OpenSCADTokenType("EXCL")          // ! (as modifier)
    @JvmField val PERCENT = OpenSCADTokenType("PERCENT")    // % (as modifier)
    @JvmField val STAR = OpenSCADTokenType("STAR")          // * (as modifier)
    
    // Comments
    @JvmField val LINE_COMMENT = OpenSCADTokenType("LINE_COMMENT")
    @JvmField val BLOCK_COMMENT = OpenSCADTokenType("BLOCK_COMMENT")
    @JvmField val COMMENT = OpenSCADTokenType("COMMENT")
    
    // Element types
    val MODULE_DECLARATION = OpenSCADElementType("MODULE_DECLARATION")
    val FUNCTION_DECLARATION = OpenSCADElementType("FUNCTION_DECLARATION")
    val PARAMETER_LIST = OpenSCADElementType("PARAMETER_LIST")
    val PARAMETER = OpenSCADElementType("PARAMETER")
    val BLOCK = OpenSCADElementType("BLOCK")
    val ARGUMENT_LIST = OpenSCADElementType("ARGUMENT_LIST")
    val ARGUMENT = OpenSCADElementType("ARGUMENT")
    val EXPRESSION = OpenSCADElementType("EXPRESSION")
    val ASSIGNMENT_STATEMENT = OpenSCADElementType("ASSIGNMENT_STATEMENT")
    val MODULE_INSTANTIATION = OpenSCADElementType("MODULE_INSTANTIATION")
    val MODULE_CALL = OpenSCADElementType("MODULE_CALL")
    val IF_STATEMENT = OpenSCADElementType("IF_STATEMENT")
    val FOR_STATEMENT = OpenSCADElementType("FOR_STATEMENT")
    val INTERSECTION_FOR_STATEMENT = OpenSCADElementType("INTERSECTION_FOR_STATEMENT")
    val LET_STATEMENT = OpenSCADElementType("LET_STATEMENT")
    val ASSERT_STATEMENT = OpenSCADElementType("ASSERT_STATEMENT")
    val EACH_EXPRESSION = OpenSCADElementType("EACH_EXPRESSION")
    val INCLUDE_STATEMENT = OpenSCADElementType("INCLUDE_STATEMENT")
    val USE_STATEMENT = OpenSCADElementType("USE_STATEMENT")
    val CONDITIONAL_EXPRESSION = OpenSCADElementType("CONDITIONAL_EXPRESSION")
    val LOGICAL_OR_EXPRESSION = OpenSCADElementType("LOGICAL_OR_EXPRESSION")
    val LOGICAL_AND_EXPRESSION = OpenSCADElementType("LOGICAL_AND_EXPRESSION")
    val EQUALITY_EXPRESSION = OpenSCADElementType("EQUALITY_EXPRESSION")
    val RELATIONAL_EXPRESSION = OpenSCADElementType("RELATIONAL_EXPRESSION")
    val BINARY_OR_EXPRESSION = OpenSCADElementType("BINARY_OR_EXPRESSION")
    val BINARY_AND_EXPRESSION = OpenSCADElementType("BINARY_AND_EXPRESSION")
    val SHIFT_EXPRESSION = OpenSCADElementType("SHIFT_EXPRESSION")
    val ADDITIVE_EXPRESSION = OpenSCADElementType("ADDITIVE_EXPRESSION")
    val MULTIPLICATIVE_EXPRESSION = OpenSCADElementType("MULTIPLICATIVE_EXPRESSION")
    val UNARY_EXPRESSION = OpenSCADElementType("UNARY_EXPRESSION")
    val POSTFIX_EXPRESSION = OpenSCADElementType("POSTFIX_EXPRESSION")
    val PRIMARY_EXPRESSION = OpenSCADElementType("PRIMARY_EXPRESSION")
    val VECTOR_LITERAL = OpenSCADElementType("VECTOR_LITERAL")
    val RANGE_LITERAL = OpenSCADElementType("RANGE_LITERAL")
    val LET_EXPRESSION = OpenSCADElementType("LET_EXPRESSION")
    val LIST_COMPREHENSION = OpenSCADElementType("LIST_COMPREHENSION")
    val COMPREHENSION = OpenSCADElementType("COMPREHENSION")
    val COMPREHENSION_CLAUSE = OpenSCADElementType("COMPREHENSION_CLAUSE")
    val FOR_BINDING = OpenSCADElementType("FOR_BINDING")
    val FOR_BINDING_LIST = OpenSCADElementType("FOR_BINDING_LIST")
    val FUNCTION_CALL = OpenSCADElementType("FUNCTION_CALL")
    val INDEX_EXPRESSION = OpenSCADElementType("INDEX_EXPRESSION")
    val SLICE_EXPRESSION = OpenSCADElementType("SLICE_EXPRESSION")
    val MEMBER_ACCESS = OpenSCADElementType("MEMBER_ACCESS")
    
    object Factory {
        fun createElement(node: ASTNode?): PsiElement {
            if (node == null) {
                throw IllegalArgumentException("Cannot create element from null node")
            }
            
            return when (node.elementType) {
                MODULE_DECLARATION -> OpenSCADModuleDeclaration(node)
                FUNCTION_DECLARATION -> OpenSCADFunctionDeclaration(node)
                // For now, return a generic wrapper for other types
                else -> OpenSCADPsiElement(node)
            }
        }
    }
}
