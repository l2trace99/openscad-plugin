package org.openscad.preview

import java.awt.*
import java.awt.event.*
import java.awt.image.BufferedImage
import javax.swing.JPanel
import kotlin.math.cos
import kotlin.math.sin

/**
 * Simple 3D wireframe viewer for STL models
 * Supports mouse rotation, zoom, and pan
 */
class STLViewerPanel : JPanel() {
    
    private var model: STLParser.STLModel? = null
    private var rotationX = 0.3
    private var rotationY = 0.3
    private var rotationZ = 0.0
    private var zoom = 1.0
    private var panX = 0.0
    private var panY = 0.0
    private var showWireframe = false // Toggle between solid and wireframe
    
    private var lastMouseX = 0
    private var lastMouseY = 0
    private var isDragging = false
    private var isPanning = false
    
    init {
        background = Color(45, 45, 45)
        preferredSize = Dimension(600, 600)
        
        addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                lastMouseX = e.x
                lastMouseY = e.y
                isDragging = e.button == MouseEvent.BUTTON1
                isPanning = e.button == MouseEvent.BUTTON3
            }
            
            override fun mouseReleased(e: MouseEvent) {
                isDragging = false
                isPanning = false
            }
        })
        
        addMouseMotionListener(object : MouseMotionAdapter() {
            override fun mouseDragged(e: MouseEvent) {
                val dx = e.x - lastMouseX
                val dy = e.y - lastMouseY
                
                if (isDragging) {
                    // Rotate
                    rotationY += dx * 0.01
                    rotationX += dy * 0.01
                    repaint()
                } else if (isPanning) {
                    // Pan
                    panX += dx
                    panY += dy
                    repaint()
                }
                
                lastMouseX = e.x
                lastMouseY = e.y
            }
        })
        
        addMouseWheelListener { e ->
            // Zoom
            val zoomFactor = if (e.wheelRotation < 0) 1.1 else 0.9
            zoom *= zoomFactor
            repaint()
        }
    }
    
    fun setModel(model: STLParser.STLModel?) {
        this.model = model
        resetView()
        repaint()
    }
    
    fun resetView() {
        rotationX = 0.3
        rotationY = 0.3
        rotationZ = 0.0
        zoom = 1.0
        panX = 0.0
        panY = 0.0
        repaint()
    }
    
    fun toggleWireframe() {
        showWireframe = !showWireframe
        repaint()
    }
    
    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        
        val g2d = g as Graphics2D
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        
        val currentModel = model
        if (currentModel == null) {
            drawNoModelMessage(g2d)
            return
        }
        
        drawModel(g2d, currentModel)
    }
    
    private fun drawNoModelMessage(g2d: Graphics2D) {
        g2d.color = Color.LIGHT_GRAY
        g2d.font = Font("SansSerif", Font.PLAIN, 14)
        val message = "No model loaded"
        val fm = g2d.fontMetrics
        val x = (width - fm.stringWidth(message)) / 2
        val y = height / 2
        g2d.drawString(message, x, y)
    }
    
    private fun drawModel(g2d: Graphics2D, model: STLParser.STLModel) {
        val centerX = width / 2.0 + panX
        val centerY = height / 2.0 + panY
        
        // Calculate scale to fit model in view
        val modelSize = maxOf(model.bounds.size.x, model.bounds.size.y, model.bounds.size.z)
        val viewSize = minOf(width, height) * 0.8
        val scale = (viewSize / modelSize) * zoom
        
        // Draw axes
        drawAxes(g2d, centerX, centerY, scale)
        
        // Project and sort triangles by depth (painter's algorithm)
        data class ProjectedTriangle(
            val p1: Pair<Int, Int>,
            val p2: Pair<Int, Int>,
            val p3: Pair<Int, Int>,
            val depth: Double,
            val normal: STLParser.Vector3
        )
        
        val projectedTriangles = model.triangles.map { triangle ->
            val p1 = project(triangle.v1, model.bounds.center, centerX, centerY, scale)
            val p2 = project(triangle.v2, model.bounds.center, centerX, centerY, scale)
            val p3 = project(triangle.v3, model.bounds.center, centerX, centerY, scale)
            
            // Calculate average depth for sorting
            val depth = (
                getDepth(triangle.v1, model.bounds.center) +
                getDepth(triangle.v2, model.bounds.center) +
                getDepth(triangle.v3, model.bounds.center)
            ) / 3.0
            
            ProjectedTriangle(p1, p2, p3, depth, triangle.normal)
        }.sortedBy { it.depth } // Draw far to near
        
        // Draw triangles
        projectedTriangles.forEach { tri ->
            // Calculate lighting based on normal (simple directional light)
            val lightDir = STLParser.Vector3(0.5f, 0.5f, 1.0f)
            val lightDot = maxOf(0f, 
                tri.normal.x * lightDir.x + 
                tri.normal.y * lightDir.y + 
                tri.normal.z * lightDir.z
            )
            
            // Base color with lighting
            val brightness = (0.3f + 0.7f * lightDot).coerceIn(0f, 1f)
            val r = (100 * brightness).toInt()
            val g = (150 * brightness).toInt()
            val b = (255 * brightness).toInt()
            
            if (showWireframe) {
                // Wireframe mode
                g2d.color = Color(r, g, b)
                g2d.drawLine(tri.p1.first, tri.p1.second, tri.p2.first, tri.p2.second)
                g2d.drawLine(tri.p2.first, tri.p2.second, tri.p3.first, tri.p3.second)
                g2d.drawLine(tri.p3.first, tri.p3.second, tri.p1.first, tri.p1.second)
            } else {
                // Solid mode with filled triangles
                g2d.color = Color(r, g, b)
                val xPoints = intArrayOf(tri.p1.first, tri.p2.first, tri.p3.first)
                val yPoints = intArrayOf(tri.p1.second, tri.p2.second, tri.p3.second)
                g2d.fillPolygon(xPoints, yPoints, 3)
                
                // Draw edges in darker color
                g2d.color = Color(r / 2, g / 2, b / 2)
                g2d.drawPolygon(xPoints, yPoints, 3)
            }
        }
        
        // Draw info
        drawInfo(g2d, model)
    }
    
    private fun getDepth(point: STLParser.Vector3, modelCenter: STLParser.Vector3): Double {
        // Translate to origin
        var x = (point.x - modelCenter.x).toDouble()
        var y = (point.y - modelCenter.y).toDouble()
        var z = (point.z - modelCenter.z).toDouble()
        
        // Apply rotations
        val cosX = cos(rotationX)
        val sinX = sin(rotationX)
        val y1 = y * cosX - z * sinX
        val z1 = y * sinX + z * cosX
        y = y1
        z = z1
        
        val cosY = cos(rotationY)
        val sinY = sin(rotationY)
        val x1 = x * cosY + z * sinY
        val z2 = -x * sinY + z * cosY
        x = x1
        z = z2
        
        return z // Return depth (z-coordinate after rotation)
    }
    
    private fun drawAxes(g2d: Graphics2D, centerX: Double, centerY: Double, scale: Double) {
        val axisLength = 50.0
        
        // X axis (red)
        g2d.color = Color.RED
        val xEnd = project(STLParser.Vector3(axisLength.toFloat(), 0f, 0f), STLParser.Vector3(0f, 0f, 0f), centerX, centerY, scale)
        g2d.drawLine(centerX.toInt(), centerY.toInt(), xEnd.first, xEnd.second)
        
        // Y axis (green)
        g2d.color = Color.GREEN
        val yEnd = project(STLParser.Vector3(0f, axisLength.toFloat(), 0f), STLParser.Vector3(0f, 0f, 0f), centerX, centerY, scale)
        g2d.drawLine(centerX.toInt(), centerY.toInt(), yEnd.first, yEnd.second)
        
        // Z axis (blue)
        g2d.color = Color.BLUE
        val zEnd = project(STLParser.Vector3(0f, 0f, axisLength.toFloat()), STLParser.Vector3(0f, 0f, 0f), centerX, centerY, scale)
        g2d.drawLine(centerX.toInt(), centerY.toInt(), zEnd.first, zEnd.second)
    }
    
    private fun project(
        point: STLParser.Vector3,
        modelCenter: STLParser.Vector3,
        centerX: Double,
        centerY: Double,
        scale: Double
    ): Pair<Int, Int> {
        // Translate to origin
        var x = (point.x - modelCenter.x).toDouble()
        var y = (point.y - modelCenter.y).toDouble()
        var z = (point.z - modelCenter.z).toDouble()
        
        // Rotate around X axis
        val cosX = cos(rotationX)
        val sinX = sin(rotationX)
        val y1 = y * cosX - z * sinX
        val z1 = y * sinX + z * cosX
        y = y1
        z = z1
        
        // Rotate around Y axis
        val cosY = cos(rotationY)
        val sinY = sin(rotationY)
        val x1 = x * cosY + z * sinY
        val z2 = -x * sinY + z * cosY
        x = x1
        z = z2
        
        // Rotate around Z axis
        val cosZ = cos(rotationZ)
        val sinZ = sin(rotationZ)
        val x2 = x * cosZ - y * sinZ
        val y2 = x * sinZ + y * cosZ
        x = x2
        y = y2
        
        // Apply scale and translate to screen coordinates
        val screenX = (centerX + x * scale).toInt()
        val screenY = (centerY - y * scale).toInt() // Invert Y for screen coordinates
        
        return Pair(screenX, screenY)
    }
    
    private fun drawInfo(g2d: Graphics2D, model: STLParser.STLModel) {
        g2d.color = Color.LIGHT_GRAY
        g2d.font = Font("SansSerif", Font.PLAIN, 11)
        
        val renderMode = if (showWireframe) "Wireframe" else "Solid"
        val info = listOf(
            "Triangles: ${model.triangles.size}",
            "Size: %.1f x %.1f x %.1f".format(
                model.bounds.size.x,
                model.bounds.size.y,
                model.bounds.size.z
            ),
            "Mode: $renderMode",
            "Left drag: Rotate | Right drag: Pan | Wheel: Zoom"
        )
        
        var y = 15
        info.forEach { line ->
            g2d.drawString(line, 10, y)
            y += 15
        }
    }
}
