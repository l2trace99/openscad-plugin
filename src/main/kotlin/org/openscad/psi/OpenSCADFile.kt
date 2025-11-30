package org.openscad.psi

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.FileViewProvider
import org.openscad.file.OpenSCADFileType
import org.openscad.OpenSCADLanguage

class OpenSCADFile(viewProvider: FileViewProvider) : 
    PsiFileBase(viewProvider, OpenSCADLanguage.INSTANCE) {
    
    override fun getFileType(): FileType = OpenSCADFileType.INSTANCE
    
    override fun toString(): String = "OpenSCAD File"
}
