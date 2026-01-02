package org.openscad.highlighting

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import java.util.concurrent.ConcurrentHashMap

/**
 * Annotator that shows warnings for OpenSCAD code issues.
 * 
 * Currently detects:
 * - Variable reassignment warnings (when a variable is assigned multiple times)
 * 
 * Uses static cache to ensure single processing per file per modification.
 */
class OpenSCADWarningAnnotator : Annotator {
    
    companion object {
        // Cache: fileKey -> (warnings, appliedOffsets)
        private data class CacheEntry(
            val warnings: List<OpenSCADWarnings.Warning>,
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
            CacheEntry(OpenSCADWarnings.findWarnings(file.text))
        }
        
        val elementRange = element.textRange
        
        // Apply only warnings that fall within this element's range
        synchronized(entry.applied) {
            for (warning in entry.warnings) {
                val startOffset = file.textRange.startOffset + warning.startOffset
                val endOffset = file.textRange.startOffset + warning.endOffset
                val key = Pair(startOffset, endOffset)
                
                if (entry.applied.contains(key)) continue
                
                // Check if this warning is within the current element
                if (startOffset >= elementRange.startOffset && endOffset <= elementRange.endOffset) {
                    val range = TextRange(startOffset, endOffset)
                    holder.newAnnotation(HighlightSeverity.WARNING, warning.message)
                        .range(range)
                        .create()
                    entry.applied.add(key)
                }
            }
        }
    }
}
