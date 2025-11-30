package org.openscad.documentation

import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.psi.PsiElement
import org.openscad.psi.OpenSCADFunctionDeclaration
import org.openscad.psi.OpenSCADModuleDeclaration

class OpenSCADDocumentationProvider : AbstractDocumentationProvider() {
    override fun generateDoc(element: PsiElement?, originalElement: PsiElement?): String? {
        if (element == null) return null
        
        return when (element) {
            is OpenSCADModuleDeclaration -> generateModuleDoc(element)
            is OpenSCADFunctionDeclaration -> generateFunctionDoc(element)
            else -> {
                // Check if it's a built-in module/function
                val text = element.text
                BUILTIN_DOCS[text]
            }
        }
    }
    
    private fun generateModuleDoc(module: OpenSCADModuleDeclaration): String {
        val name = module.name ?: "unknown"
        val params = module.getParameterList()?.text ?: "()"
        
        return buildString {
            append("<html><body>")
            append("<h3>Module: $name</h3>")
            append("<p><b>Parameters:</b> $params</p>")
            append("</body></html>")
        }
    }
    
    private fun generateFunctionDoc(function: OpenSCADFunctionDeclaration): String {
        val name = function.name ?: "unknown"
        val params = function.getParameterList()?.text ?: "()"
        
        return buildString {
            append("<html><body>")
            append("<h3>Function: $name</h3>")
            append("<p><b>Parameters:</b> $params</p>")
            append("</body></html>")
        }
    }
    
    companion object {
        private val BUILTIN_DOCS = mapOf(
            "cube" to """
                <html><body>
                <h3>cube - 3D Primitive</h3>
                <p>Creates a cube in the first octant.</p>
                <p><b>Parameters:</b></p>
                <ul>
                  <li><b>size</b> - single value or [x,y,z] vector</li>
                  <li><b>center</b> - false (default) or true</li>
                </ul>
                <p><b>Example:</b> <code>cube([10, 20, 30], center=true);</code></p>
                <p><a href="https://en.wikibooks.org/wiki/OpenSCAD_User_Manual/Primitive_Solids#cube">Official Documentation</a></p>
                </body></html>
            """.trimIndent(),
            
            "sphere" to """
                <html><body>
                <h3>sphere - 3D Primitive</h3>
                <p>Creates a sphere at the origin.</p>
                <p><b>Parameters:</b></p>
                <ul>
                  <li><b>r</b> - radius of sphere</li>
                  <li><b>d</b> - diameter of sphere</li>
                </ul>
                <p><b>Example:</b> <code>sphere(r=10);</code></p>
                <p><a href="https://en.wikibooks.org/wiki/OpenSCAD_User_Manual/Primitive_Solids#sphere">Official Documentation</a></p>
                </body></html>
            """.trimIndent(),
            
            "cylinder" to """
                <html><body>
                <h3>cylinder - 3D Primitive</h3>
                <p>Creates a cylinder or cone.</p>
                <p><b>Parameters:</b></p>
                <ul>
                  <li><b>h</b> - height of the cylinder</li>
                  <li><b>r</b> - radius of cylinder (r1 = r2)</li>
                  <li><b>r1</b> - radius, bottom of cone</li>
                  <li><b>r2</b> - radius, top of cone</li>
                  <li><b>d</b> - diameter of cylinder (d1 = d2)</li>
                  <li><b>center</b> - false (default) or true</li>
                </ul>
                <p><b>Example:</b> <code>cylinder(h=10, r=5, center=true);</code></p>
                <p><a href="https://en.wikibooks.org/wiki/OpenSCAD_User_Manual/Primitive_Solids#cylinder">Official Documentation</a></p>
                </body></html>
            """.trimIndent(),
            
            "translate" to """
                <html><body>
                <h3>translate - Transformation</h3>
                <p>Translates (moves) its child elements along the specified vector.</p>
                <p><b>Parameters:</b></p>
                <ul>
                  <li><b>v</b> - [x, y, z] translation vector</li>
                </ul>
                <p><b>Example:</b> <code>translate([10, 0, 0]) cube(5);</code></p>
                <p><a href="https://en.wikibooks.org/wiki/OpenSCAD_User_Manual/Transformations#translate">Official Documentation</a></p>
                </body></html>
            """.trimIndent(),
            
            "rotate" to """
                <html><body>
                <h3>rotate - Transformation</h3>
                <p>Rotates its child elements about the origin or around an arbitrary axis.</p>
                <p><b>Parameters:</b></p>
                <ul>
                  <li><b>a</b> - angle in degrees or [x, y, z] rotation angles</li>
                  <li><b>v</b> - [x, y, z] axis of rotation</li>
                </ul>
                <p><b>Example:</b> <code>rotate([0, 0, 45]) cube(5);</code></p>
                <p><a href="https://en.wikibooks.org/wiki/OpenSCAD_User_Manual/Transformations#rotate">Official Documentation</a></p>
                </body></html>
            """.trimIndent(),
            
            "scale" to """
                <html><body>
                <h3>scale - Transformation</h3>
                <p>Scales its child elements using the specified vector.</p>
                <p><b>Parameters:</b></p>
                <ul>
                  <li><b>v</b> - [x, y, z] scale factors or single value</li>
                </ul>
                <p><b>Example:</b> <code>scale([2, 1, 1]) cube(5);</code></p>
                <p><a href="https://en.wikibooks.org/wiki/OpenSCAD_User_Manual/Transformations#scale">Official Documentation</a></p>
                </body></html>
            """.trimIndent(),
            
            "union" to """
                <html><body>
                <h3>union - Boolean Operation</h3>
                <p>Creates a union of all its child nodes. This is the sum of all children.</p>
                <p><b>Example:</b></p>
                <pre>union() {
    cube(5);
    translate([5, 0, 0]) cube(5);
}</pre>
                <p><a href="https://en.wikibooks.org/wiki/OpenSCAD_User_Manual/CSG_Modelling#union">Official Documentation</a></p>
                </body></html>
            """.trimIndent(),
            
            "difference" to """
                <html><body>
                <h3>difference - Boolean Operation</h3>
                <p>Subtracts the 2nd (and all further) child nodes from the first one.</p>
                <p><b>Example:</b></p>
                <pre>difference() {
    cube(10);
    translate([5, 5, 5]) sphere(r=6);
}</pre>
                <p><a href="https://en.wikibooks.org/wiki/OpenSCAD_User_Manual/CSG_Modelling#difference">Official Documentation</a></p>
                </body></html>
            """.trimIndent(),
            
            "intersection" to """
                <html><body>
                <h3>intersection - Boolean Operation</h3>
                <p>Creates the intersection of all child nodes. Only the area which is common to all children is retained.</p>
                <p><b>Example:</b></p>
                <pre>intersection() {
    cube(10);
    translate([5, 5, 5]) sphere(r=6);
}</pre>
                <p><a href="https://en.wikibooks.org/wiki/OpenSCAD_User_Manual/CSG_Modelling#intersection">Official Documentation</a></p>
                </body></html>
            """.trimIndent(),
            
            "for" to """
                <html><body>
                <h3>for - Loop Statement</h3>
                <p>Iterates over a range or list and creates child elements for each iteration.</p>
                <p><b>Syntax:</b> <code>for (variable = range) { ... }</code></p>
                <p><b>Example:</b></p>
                <pre>for (i = [0:5]) {
    translate([i*10, 0, 0]) cube(5);
}</pre>
                <p><a href="https://en.wikibooks.org/wiki/OpenSCAD_User_Manual/Conditional_and_Iterator_Functions#for">Official Documentation</a></p>
                </body></html>
            """.trimIndent(),
            
            "if" to """
                <html><body>
                <h3>if - Conditional Statement</h3>
                <p>Conditionally creates child elements based on a boolean expression.</p>
                <p><b>Syntax:</b> <code>if (condition) { ... } else { ... }</code></p>
                <p><b>Example:</b></p>
                <pre>if (x > 5) {
    cube(10);
} else {
    sphere(5);
}</pre>
                <p><a href="https://en.wikibooks.org/wiki/OpenSCAD_User_Manual/Conditional_and_Iterator_Functions#if">Official Documentation</a></p>
                </body></html>
            """.trimIndent()
        )
    }
}
