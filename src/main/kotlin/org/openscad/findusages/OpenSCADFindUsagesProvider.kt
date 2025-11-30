package org.openscad.findusages

import com.intellij.lang.cacheBuilder.DefaultWordsScanner
import com.intellij.lang.cacheBuilder.WordsScanner
import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.tree.TokenSet
import org.openscad.lexer.OpenSCADLexer
import org.openscad.psi.OpenSCADFunctionDeclaration
import org.openscad.psi.OpenSCADModuleDeclaration
import org.openscad.psi.OpenSCADTypes

class OpenSCADFindUsagesProvider : FindUsagesProvider {
    override fun getWordsScanner(): WordsScanner {
        return DefaultWordsScanner(
            OpenSCADLexer(),
            TokenSet.create(OpenSCADTypes.IDENT),
            TokenSet.create(OpenSCADTypes.LINE_COMMENT, OpenSCADTypes.BLOCK_COMMENT),
            TokenSet.create(OpenSCADTypes.STRING, OpenSCADTypes.NUMBER)
        )
    }
    
    override fun canFindUsagesFor(psiElement: PsiElement): Boolean {
        return psiElement is OpenSCADModuleDeclaration || psiElement is OpenSCADFunctionDeclaration
    }
    
    override fun getHelpId(psiElement: PsiElement): String? = null
    
    override fun getType(element: PsiElement): String {
        return when (element) {
            is OpenSCADModuleDeclaration -> "module"
            is OpenSCADFunctionDeclaration -> "function"
            else -> ""
        }
    }
    
    override fun getDescriptiveName(element: PsiElement): String {
        return when (element) {
            is PsiNamedElement -> element.name ?: ""
            else -> ""
        }
    }
    
    override fun getNodeText(element: PsiElement, useFullName: Boolean): String {
        return when (element) {
            is PsiNamedElement -> element.name ?: ""
            else -> element.text
        }
    }
}
