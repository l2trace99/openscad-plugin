package org.openscad.psi

import com.intellij.psi.tree.IElementType
import org.openscad.OpenSCADLanguage

/**
 * Element type for OpenSCAD PSI tree nodes
 */
class OpenSCADElementType(debugName: String) : IElementType(debugName, OpenSCADLanguage.INSTANCE) {
    override fun toString(): String = "OpenSCAD:${super.toString()}"
}
