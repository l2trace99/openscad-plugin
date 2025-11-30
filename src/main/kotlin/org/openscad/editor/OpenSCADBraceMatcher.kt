package org.openscad.editor

import com.intellij.lang.BracePair
import com.intellij.lang.PairedBraceMatcher
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType
import org.openscad.psi.OpenSCADTypes

class OpenSCADBraceMatcher : PairedBraceMatcher {
    override fun getPairs(): Array<BracePair> = PAIRS
    
    override fun isPairedBracesAllowedBeforeType(lbraceType: IElementType, contextType: IElementType?): Boolean = true
    
    override fun getCodeConstructStart(file: PsiFile?, openingBraceOffset: Int): Int = openingBraceOffset
    
    companion object {
        private val PAIRS = arrayOf(
            BracePair(OpenSCADTypes.LPAREN, OpenSCADTypes.RPAREN, false),
            BracePair(OpenSCADTypes.LBRACE, OpenSCADTypes.RBRACE, true),
            BracePair(OpenSCADTypes.LBRACKET, OpenSCADTypes.RBRACKET, false)
        )
    }
}
