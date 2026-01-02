package org.openscad.highlighting

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import java.util.concurrent.ConcurrentHashMap

/**
 * Annotator that shows deprecation warnings for deprecated OpenSCAD syntax.
 * Detects:
 * - filename=, layername=, triangles= parameters
 * - .amf file imports
 * - Identifier names starting with digits
 * 
 * Uses static cache to ensure single processing per file per modification.
 */
class OpenSCADDeprecatedAnnotator : Annotator {
    
    companion object {
        // Cache: fileKey -> (deprecations, appliedOffsets)
        private data class CacheEntry(
            val deprecations: List<OpenSCADDeprecations.Deprecation>,
            val applied: MutableSet<Pair<Int, Int>> = mutableSetOf()
        )
        private val cache = ConcurrentHashMap<String, CacheEntry>()
        
        private fun getFileKey(file: com.intellij.psi.PsiFile): String {
            return "${file.virtualFile?.path}:${file.modificationStamp}"
        }
        
        // Clear old entries to prevent memory leaks
        private fun cleanOldEntries(currentKey: String) {
            val prefix = currentKey.substringBeforeLast(":")
            cache.keys.filter { it.startsWith(prefix) && it != currentKey }.forEach { cache.remove(it) }
        }
    }
    
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        val file = element.containingFile ?: return
        val fileKey = getFileKey(file)
        
        // Get or compute cache entry for this file version
        val entry = cache.getOrPut(fileKey) {
            cleanOldEntries(fileKey)
            CacheEntry(OpenSCADDeprecations.findDeprecations(file.text))
        }
        
        val elementRange = element.textRange
        
        // Apply only deprecations that fall within this element's range
        synchronized(entry.applied) {
            for (deprecation in entry.deprecations) {
                val startOffset = file.textRange.startOffset + deprecation.startOffset
                val endOffset = file.textRange.startOffset + deprecation.endOffset
                val key = Pair(startOffset, endOffset)
                
                if (entry.applied.contains(key)) continue
                
                // Check if this deprecation is within the current element
                if (startOffset >= elementRange.startOffset && endOffset <= elementRange.endOffset) {
                    val range = TextRange(startOffset, endOffset)
                    holder.newAnnotation(HighlightSeverity.WARNING, deprecation.message)
                        .range(range)
                        .create()
                    entry.applied.add(key)
                }
            }
        }
    }
}
