package org.openscad.highlighting

import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import org.openscad.file.OpenSCADFileType
import javax.swing.Icon

class OpenSCADColorSettingsPage : ColorSettingsPage {
    
    override fun getIcon(): Icon = OpenSCADFileType.INSTANCE.icon
    
    override fun getHighlighter(): SyntaxHighlighter = OpenSCADSyntaxHighlighter()
    
    override fun getDemoText(): String = """
        // OpenSCAD Color Settings Demo
        
        <special_var>${'$'}fn</special_var> = 50;
        <special_var>${'$'}fa</special_var> = 12;
        
        module box(width=10, height=10) {
            <builtin_func>cube</builtin_func>([width, height, 10]);
        }
        
        function distance(p1, p2) = 
            <builtin_func>sqrt</builtin_func>(<builtin_func>pow</builtin_func>(p2[0]-p1[0], 2) + <builtin_func>pow</builtin_func>(p2[1]-p1[1], 2));
        
        <transform>translate</transform>([10, 0, 0]) {
            <transform>rotate</transform>([0, 0, 45])
                <builtin_func>sphere</builtin_func>(r=5);
        }
        
        <transform>difference</transform>() {
            <builtin_func>cube</builtin_func>(20, center=true);
            <builtin_func>cylinder</builtin_func>(h=30, r=5, center=true);
        }
        
        for (i = [0:5]) {
            <transform>translate</transform>([i*10, 0, 0])
                <builtin_func>cube</builtin_func>(5);
        }
        
        <builtin_func>echo</builtin_func>("Length:", <builtin_func>len</builtin_func>([1,2,3]));
    """.trimIndent()
    
    override fun getAdditionalHighlightingTagToDescriptorMap(): Map<String, TextAttributesKey> = mapOf(
        "builtin_func" to OpenSCADAnnotator.BUILTIN_FUNCTION,
        "transform" to OpenSCADAnnotator.BUILTIN_TRANSFORMATION,
        "special_var" to OpenSCADAnnotator.SPECIAL_VARIABLE
    )
    
    override fun getAttributeDescriptors(): Array<AttributesDescriptor> = arrayOf(
        AttributesDescriptor("Keywords", OpenSCADSyntaxHighlighter.KEYWORD),
        AttributesDescriptor("Numbers", OpenSCADSyntaxHighlighter.NUMBER),
        AttributesDescriptor("Strings", OpenSCADSyntaxHighlighter.STRING),
        AttributesDescriptor("Comments", OpenSCADSyntaxHighlighter.COMMENT),
        AttributesDescriptor("Operators", OpenSCADSyntaxHighlighter.OPERATOR),
        AttributesDescriptor("Parentheses", OpenSCADSyntaxHighlighter.PARENTHESES),
        AttributesDescriptor("Identifiers", OpenSCADSyntaxHighlighter.IDENTIFIER),
        AttributesDescriptor("Built-in Functions//Primitives (cube, sphere, etc.)", OpenSCADAnnotator.BUILTIN_FUNCTION),
        AttributesDescriptor("Built-in Functions//Transformations (translate, rotate, etc.)", OpenSCADAnnotator.BUILTIN_TRANSFORMATION),
        AttributesDescriptor("Built-in Functions//Special Variables (${'$'}fn, ${'$'}fa, etc.)", OpenSCADAnnotator.SPECIAL_VARIABLE)
    )
    
    override fun getColorDescriptors(): Array<ColorDescriptor> = emptyArray()
    
    override fun getDisplayName(): String = "OpenSCAD"
}
