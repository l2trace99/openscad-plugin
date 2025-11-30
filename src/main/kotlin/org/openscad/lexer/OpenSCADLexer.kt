package org.openscad.lexer

import com.intellij.lexer.FlexAdapter

/**
 * OpenSCAD lexer adapter that wraps the JFlex-generated lexer implementation.
 * The actual OpenSCADLexerImpl class will be generated from OpenSCADLexer.flex
 * by the Grammar-Kit plugin during the build process.
 */
class OpenSCADLexer : FlexAdapter(OpenSCADLexerImpl())
