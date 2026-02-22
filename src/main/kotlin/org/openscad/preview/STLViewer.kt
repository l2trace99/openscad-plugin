package org.openscad.preview

/**
 * Interface for STL model viewers.
 * Provides a common contract for both hardware-accelerated (JOGL) and software renderers.
 */
interface STLViewer {
    /**
     * Set an STL model to display.
     * @param model The model to display, or null to clear the view
     */
    fun setModel(model: STLParser.STLModel?)
    
    /**
     * Set a colored model (from 3MF) to display.
     * @param model The colored model to display, or null to clear the view
     */
    fun setColoredModel(model: ThreeMFParser.ColoredModel?)
    
    /**
     * Reset the view to default camera position.
     */
    fun resetView()
}
