package org.openscad.references

import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import org.openscad.psi.OpenSCADFunctionDeclaration
import org.openscad.psi.OpenSCADModuleDeclaration
import org.openscad.psi.OpenSCADPsiElement

class OpenSCADReference(element: PsiElement, textRange: TextRange) :
    PsiReferenceBase<PsiElement>(element, textRange), PsiPolyVariantReference {
    
    private val referenceName: String = element.text.substring(textRange.startOffset, textRange.endOffset)
    
    override fun resolve(): PsiElement? {
        val results = multiResolve(false)
        return if (results.size == 1) results[0].element else null
    }
    
    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        val file = element.containingFile
        val results = mutableListOf<ResolveResult>()
        
        // Find module declarations
        PsiTreeUtil.findChildrenOfType(file, OpenSCADModuleDeclaration::class.java).forEach { module ->
            if (module.name == referenceName) {
                results.add(PsiElementResolveResult(module))
            }
        }
        
        // Find function declarations
        PsiTreeUtil.findChildrenOfType(file, OpenSCADFunctionDeclaration::class.java).forEach { function ->
            if (function.name == referenceName) {
                results.add(PsiElementResolveResult(function))
            }
        }
        
        return results.toTypedArray()
    }
    
    override fun getVariants(): Array<Any> {
        val file = element.containingFile
        val variants = mutableListOf<Any>()
        
        // Add all module and function declarations as completion variants
        PsiTreeUtil.findChildrenOfType(file, OpenSCADModuleDeclaration::class.java).forEach { module ->
            module.name?.let { variants.add(it) }
        }
        
        PsiTreeUtil.findChildrenOfType(file, OpenSCADFunctionDeclaration::class.java).forEach { function ->
            function.name?.let { variants.add(it) }
        }
        
        return variants.toTypedArray()
    }
}

class OpenSCADReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        // Register reference provider for identifiers
        registrar.registerReferenceProvider(
            com.intellij.patterns.PlatformPatterns.psiElement(OpenSCADPsiElement::class.java),
            OpenSCADReferenceProvider()
        )
    }
}

class OpenSCADReferenceProvider : PsiReferenceProvider() {
    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        // Only provide references for identifiers that are not declarations
        if (element is PsiNamedElement) {
            return emptyArray()
        }
        
        val textRange = TextRange(0, element.textLength)
        return arrayOf(OpenSCADReference(element, textRange))
    }
}
