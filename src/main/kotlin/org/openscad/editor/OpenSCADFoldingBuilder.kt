package org.openscad.editor

import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.openscad.psi.OpenSCADFunctionDeclaration
import org.openscad.psi.OpenSCADModuleDeclaration
import org.openscad.psi.OpenSCADTypes

class OpenSCADFoldingBuilder : FoldingBuilderEx() {
    override fun buildFoldRegions(root: PsiElement, document: Document, quick: Boolean): Array<FoldingDescriptor> {
        val descriptors = mutableListOf<FoldingDescriptor>()
        
        // Fold module declarations
        PsiTreeUtil.findChildrenOfType(root, OpenSCADModuleDeclaration::class.java).forEach { module ->
            val block = module.node.findChildByType(OpenSCADTypes.BLOCK)
            if (block != null) {
                descriptors.add(FoldingDescriptor(module.node, block.textRange))
            }
        }
        
        // Fold function declarations
        PsiTreeUtil.findChildrenOfType(root, OpenSCADFunctionDeclaration::class.java).forEach { function ->
            val range = function.textRange
            if (range.length > 20) { // Only fold if reasonably long
                descriptors.add(FoldingDescriptor(function.node, range))
            }
        }
        
        // Fold block comments
        root.node.let { node ->
            collectCommentFolds(node, descriptors)
        }
        
        return descriptors.toTypedArray()
    }
    
    private fun collectCommentFolds(node: ASTNode, descriptors: MutableList<FoldingDescriptor>) {
        if (node.elementType == OpenSCADTypes.BLOCK_COMMENT) {
            val range = node.textRange
            if (range.length > 10) {
                descriptors.add(FoldingDescriptor(node, range))
            }
        }
        
        node.getChildren(null).forEach { child ->
            collectCommentFolds(child, descriptors)
        }
    }
    
    override fun getPlaceholderText(node: ASTNode): String {
        return when (node.elementType) {
            OpenSCADTypes.BLOCK -> "{...}"
            OpenSCADTypes.BLOCK_COMMENT -> "/*...*/"
            else -> "..."
        }
    }
    
    override fun isCollapsedByDefault(node: ASTNode): Boolean = false
}
