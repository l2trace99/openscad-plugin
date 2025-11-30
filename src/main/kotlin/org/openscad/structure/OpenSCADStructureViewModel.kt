package org.openscad.structure

import com.intellij.ide.structureView.StructureViewModel
import com.intellij.ide.structureView.StructureViewModelBase
import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.ide.structureView.TreeBasedStructureViewBuilder
import com.intellij.ide.util.treeView.smartTree.Sorter
import com.intellij.lang.PsiStructureViewFactory
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile
import org.openscad.psi.OpenSCADFile
import org.openscad.psi.OpenSCADFunctionDeclaration
import org.openscad.psi.OpenSCADModuleDeclaration

class OpenSCADStructureViewFactory : PsiStructureViewFactory {
    override fun getStructureViewBuilder(psiFile: PsiFile) = object : TreeBasedStructureViewBuilder() {
        override fun createStructureViewModel(editor: Editor?): StructureViewModel {
            return OpenSCADStructureViewModel(psiFile as OpenSCADFile, editor)
        }
    }
}

class OpenSCADStructureViewModel(psiFile: OpenSCADFile, editor: Editor?) :
    StructureViewModelBase(psiFile, editor, OpenSCADStructureViewElement(psiFile)),
    StructureViewModel.ElementInfoProvider {
    
    init {
        withSorters(Sorter.ALPHA_SORTER)
    }
    
    override fun isAlwaysShowsPlus(element: StructureViewTreeElement): Boolean = false
    
    override fun isAlwaysLeaf(element: StructureViewTreeElement): Boolean {
        val value = element.value
        return value is OpenSCADFunctionDeclaration
    }
    
    override fun getSuitableClasses(): Array<Class<*>> = arrayOf(
        OpenSCADModuleDeclaration::class.java,
        OpenSCADFunctionDeclaration::class.java
    )
}
