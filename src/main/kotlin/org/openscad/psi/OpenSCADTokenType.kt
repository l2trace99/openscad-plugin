package org.openscad.psi

import com.intellij.psi.tree.IElementType
import org.openscad.OpenSCADLanguage

class OpenSCADTokenType(debugName: String) : IElementType(debugName, OpenSCADLanguage.INSTANCE)
