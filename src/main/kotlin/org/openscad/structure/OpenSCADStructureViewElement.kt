package org.openscad.structure

import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.ide.util.treeView.smartTree.SortableTreeElement
import com.intellij.ide.util.treeView.smartTree.TreeElement
import com.intellij.navigation.ItemPresentation
import com.intellij.navigation.NavigationItem
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.util.PsiTreeUtil
import org.openscad.psi.OpenSCADFile
import org.openscad.psi.OpenSCADFunctionDeclaration
import org.openscad.psi.OpenSCADModuleDeclaration
import javax.swing.Icon

class OpenSCADStructureViewElement(private val element: PsiElement) :
    StructureViewTreeElement, SortableTreeElement {
    
    override fun getValue(): Any = element
    
    override fun navigate(requestFocus: Boolean) {
        if (element is NavigationItem) {
            (element as NavigationItem).navigate(requestFocus)
        }
    }
    
    override fun canNavigate(): Boolean {
        return element is NavigationItem && (element as NavigationItem).canNavigate()
    }
    
    override fun canNavigateToSource(): Boolean {
        return element is NavigationItem && (element as NavigationItem).canNavigateToSource()
    }
    
    override fun getAlphaSortKey(): String {
        return when (element) {
            is PsiNamedElement -> element.name ?: ""
            else -> ""
        }
    }
    
    override fun getPresentation(): ItemPresentation {
        return when (element) {
            is OpenSCADModuleDeclaration -> OpenSCADItemPresentation(element, "module")
            is OpenSCADFunctionDeclaration -> OpenSCADItemPresentation(element, "function")
            is OpenSCADFile -> OpenSCADFilePresentation(element)
            else -> object : ItemPresentation {
                override fun getPresentableText(): String? = element.text
                override fun getLocationString(): String? = null
                override fun getIcon(unused: Boolean): Icon? = null
            }
        }
    }
    
    override fun getChildren(): Array<TreeElement> {
        if (element is OpenSCADFile) {
            val modules = PsiTreeUtil.getChildrenOfTypeAsList(element, OpenSCADModuleDeclaration::class.java)
            val functions = PsiTreeUtil.getChildrenOfTypeAsList(element, OpenSCADFunctionDeclaration::class.java)
            
            return (modules + functions)
                .map { OpenSCADStructureViewElement(it) }
                .toTypedArray()
        }
        return emptyArray()
    }
}

class OpenSCADItemPresentation(
    private val element: PsiNamedElement,
    private val type: String
) : ItemPresentation {
    override fun getPresentableText(): String? = element.name
    
    override fun getLocationString(): String? = type
    
    override fun getIcon(unused: Boolean): Icon? = null
}

class OpenSCADFilePresentation(private val file: OpenSCADFile) : ItemPresentation {
    override fun getPresentableText(): String? = file.name
    
    override fun getLocationString(): String? = null
    
    override fun getIcon(unused: Boolean): Icon? = file.getIcon(0)
}
