package org.openscad.psi

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiNameIdentifierOwner
import org.openscad.OpenSCADLanguage

/**
 * Base class for all OpenSCAD PSI elements
 */
open class OpenSCADPsiElement(node: ASTNode) : ASTWrapperPsiElement(node)

/**
 * Base class for named OpenSCAD PSI elements (modules, functions, variables)
 */
abstract class OpenSCADNamedElement(node: ASTNode) : OpenSCADPsiElement(node), PsiNameIdentifierOwner {
    override fun getName(): String? {
        val nameNode = node.findChildByType(OpenSCADTypes.IDENT)
        return nameNode?.text
    }
    
    override fun getNameIdentifier(): PsiElement? {
        val identNode = node.findChildByType(OpenSCADTypes.IDENT)
        return identNode?.psi
    }
    
    override fun setName(name: String): PsiElement {
        val identNode = node.findChildByType(OpenSCADTypes.IDENT) ?: return this
        
        // Create a dummy file with a module declaration containing the new name
        // to get a properly parsed identifier node
        val dummyFile = PsiFileFactory.getInstance(project)
            .createFileFromText("dummy.scad", OpenSCADLanguage.INSTANCE, "module $name() {}")
        
        // Find the new identifier in the dummy file
        val newIdentNode = findIdentifierInTree(dummyFile.node)
        
        if (newIdentNode != null) {
            // Replace the old identifier with the new one
            node.replaceChild(identNode, newIdentNode.copyElement())
        }
        
        return this
    }
    
    private fun findIdentifierInTree(node: ASTNode): ASTNode? {
        if (node.elementType == OpenSCADTypes.IDENT) {
            return node
        }
        var child = node.firstChildNode
        while (child != null) {
            val result = findIdentifierInTree(child)
            if (result != null) return result
            child = child.treeNext
        }
        return null
    }
    
    override fun getTextOffset(): Int {
        return getNameIdentifier()?.textOffset ?: super.getTextOffset()
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
