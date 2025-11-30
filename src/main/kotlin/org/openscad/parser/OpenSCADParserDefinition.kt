package org.openscad.parser

import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiParser
import com.intellij.lexer.FlexAdapter
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet
import org.openscad.OpenSCADLanguage
import org.openscad.lexer.OpenSCADLexer
import org.openscad.psi.OpenSCADFile
import org.openscad.psi.OpenSCADTypes

class OpenSCADParserDefinition : ParserDefinition {
    override fun createLexer(project: Project?): Lexer = OpenSCADLexer()

    override fun createParser(project: Project?): PsiParser = OpenSCADParser()

    override fun getFileNodeType(): IFileElementType = FILE

    override fun getWhitespaceTokens(): TokenSet = WHITE_SPACES

    override fun getCommentTokens(): TokenSet = COMMENTS

    override fun getStringLiteralElements(): TokenSet = STRING_LITERALS

    override fun createElement(node: ASTNode?): PsiElement = OpenSCADTypes.Factory.createElement(node)

    override fun createFile(viewProvider: FileViewProvider): PsiFile = OpenSCADFile(viewProvider)

    companion object {
        val FILE = IFileElementType(OpenSCADLanguage.INSTANCE)
        
        val WHITE_SPACES = TokenSet.create(
            com.intellij.psi.TokenType.WHITE_SPACE
        )
        
        val COMMENTS = TokenSet.create(
            OpenSCADTypes.LINE_COMMENT,
            OpenSCADTypes.BLOCK_COMMENT
        )
        
        val STRING_LITERALS = TokenSet.create(
            OpenSCADTypes.STRING
        )
    }
}
