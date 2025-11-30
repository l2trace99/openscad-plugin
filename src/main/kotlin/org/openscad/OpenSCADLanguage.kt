package org.openscad

import com.intellij.lang.Language

class OpenSCADLanguage private constructor() : Language("OpenSCAD") {
    companion object {
        @JvmField
        val INSTANCE = OpenSCADLanguage()
    }
}
