package org.openscad.references

import com.intellij.openapi.application.QueryExecutorBase
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceService
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.search.PsiSearchHelper
import com.intellij.psi.search.UsageSearchContext
import com.intellij.util.Processor
import org.openscad.psi.OpenSCADFunctionDeclaration
import org.openscad.psi.OpenSCADModuleDeclaration
import org.openscad.psi.OpenSCADTypes

/**
 * Custom references searcher for OpenSCAD symbols.
 * This is needed because the standard ReferencesSearch doesn't find
 * references contributed via PsiReferenceContributor for rename refactoring.
 */
class OpenSCADReferencesSearcher : QueryExecutorBase<PsiReference, ReferencesSearch.SearchParameters>(true) {
    
    override fun processQuery(
        queryParameters: ReferencesSearch.SearchParameters,
        consumer: Processor<in PsiReference>
    ) {
        val elementToSearch = queryParameters.elementToSearch
        
        // Only handle OpenSCAD module and function declarations
        if (elementToSearch !is OpenSCADModuleDeclaration && elementToSearch !is OpenSCADFunctionDeclaration) {
            return
        }
        
        val name = (elementToSearch as? PsiNamedElement)?.name ?: return
        val project = elementToSearch.project
        val searchScope = queryParameters.effectiveSearchScope
        
        // Use PsiSearchHelper to find all occurrences of the name
        val searchHelper = PsiSearchHelper.getInstance(project)
        
        searchHelper.processElementsWithWord(
            { element, offsetInElement ->
                // Skip if this is the declaration itself
                if (element == elementToSearch || element.parent == elementToSearch) {
                    return@processElementsWithWord true
                }
                
                // Check element type - we're looking for IDENT tokens
                val elementType = element.node?.elementType
                if (elementType != OpenSCADTypes.IDENT && elementType?.toString() != "IDENT") {
                    return@processElementsWithWord true
                }
                
                // Skip if text doesn't match
                if (element.text != name) {
                    return@processElementsWithWord true
                }
                
                // Get references using PsiReferenceService (includes contributed references)
                val references = PsiReferenceService.getService().getReferences(
                    element,
                    PsiReferenceService.Hints(elementToSearch, offsetInElement)
                )
                
                for (ref in references) {
                    if (ref.isReferenceTo(elementToSearch)) {
                        if (!consumer.process(ref)) {
                            return@processElementsWithWord false
                        }
                    }
                }
                
                // If no references found via service, create one directly
                if (references.isEmpty()) {
                    val ref = OpenSCADReference(element, TextRange(0, element.textLength))
                    if (ref.isReferenceTo(elementToSearch)) {
                        if (!consumer.process(ref)) {
                            return@processElementsWithWord false
                        }
                    }
                }
                
                true
            },
            searchScope,
            name,
            UsageSearchContext.IN_CODE,
            true // case sensitive
        )
    }
}
