package org.openscad.psi

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement

/**
 * Base class for all OpenSCAD PSI elements
 */
open class OpenSCADPsiElement(node: ASTNode) : ASTWrapperPsiElement(node)

/**
 * Base class for named OpenSCAD PSI elements (modules, functions, variables)
 */
abstract class OpenSCADNamedElement(node: ASTNode) : OpenSCADPsiElement(node), PsiNamedElement {
    override fun getName(): String? {
        val nameNode = node.findChildByType(OpenSCADTypes.IDENT)
        return nameNode?.text
    }
    
    override fun setName(name: String): PsiElement {
        // TODO: Implement rename refactoring
        return this
    }
}

/**
 * Module declaration PSI element
 */
class OpenSCADModuleDeclaration(node: ASTNode) : OpenSCADNamedElement(node) {
    fun getParameterList(): OpenSCADPsiElement? {
        return node.findChildByType(OpenSCADTypes.PARAMETER_LIST)?.psi as? OpenSCADPsiElement
    }
    
    fun getBlock(): OpenSCADPsiElement? {
        return node.findChildByType(OpenSCADTypes.BLOCK)?.psi as? OpenSCADPsiElement
    }
}

/**
 * Function declaration PSI element
 */
class OpenSCADFunctionDeclaration(node: ASTNode) : OpenSCADNamedElement(node) {
    fun getParameterList(): OpenSCADPsiElement? {
        return node.findChildByType(OpenSCADTypes.PARAMETER_LIST)?.psi as? OpenSCADPsiElement
    }
    
    fun getExpression(): OpenSCADPsiElement? {
        return node.findChildByType(OpenSCADTypes.EXPRESSION)?.psi as? OpenSCADPsiElement
    }
}
