package org.openscad.preview

import java.awt.*
import java.awt.event.*
import java.awt.image.BufferedImage
import java.nio.file.Path
import javax.imageio.ImageIO
import javax.swing.JPanel
import kotlin.math.cos
import kotlin.math.sin

/**
 * Simple 3D wireframe viewer for STL models
 * Supports mouse rotation, zoom, and pan
 * Also supports displaying PNG preview images (for debug modifier visualization)
 */
class STLViewerPanel : JPanel() {
    
    private var model: STLParser.STLModel? = null
    private var coloredModel: ThreeMFParser.ColoredModel? = null
    private var previewImage: BufferedImage? = null
    private var showImagePreview = false // When true, shows PNG instead of 3D model
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
    
    // Orientation cube settings
    private val cubeSize = 60
    private val cubeMargin = 10
    
    init {
        background = Color(37, 37, 37) // OpenSCAD default background
        minimumSize = Dimension(200, 200)
        preferredSize = Dimension(400, 400)
        
        addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                // Check if click is on orientation cube
                if (e.button == MouseEvent.BUTTON1 && handleOrientationCubeClick(e.x, e.y)) {
                    return
                }
                
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
    
    fun setModel(model: STLParser.STLModel?, preserveView: Boolean = true) {
        val isFirstModel = this.model == null && this.coloredModel == null
        this.model = model
        this.coloredModel = null
        showImagePreview = false // Switch back to 3D view
        // Only reset view for first model load, preserve orientation for re-renders
        if (!preserveView || isFirstModel) {
            resetView()
        }
        repaint()
    }
    
    fun setColoredModel(model: ThreeMFParser.ColoredModel?, preserveView: Boolean = true) {
        val isFirstModel = this.model == null && this.coloredModel == null
        this.coloredModel = model
        this.model = null
        showImagePreview = false // Switch back to 3D view
        if (!preserveView || isFirstModel) {
            resetView()
        }
        repaint()
    }
    
    /**
     * Set a PNG preview image (for debug modifier visualization)
     */
    fun setPreviewImage(imagePath: Path?) {
        if (imagePath != null) {
            try {
                previewImage = ImageIO.read(imagePath.toFile())
                showImagePreview = true
            } catch (e: Exception) {
                previewImage = null
                showImagePreview = false
            }
        } else {
            previewImage = null
            showImagePreview = false
        }
        repaint()
    }
    
    /**
     * Toggle between 3D model view and PNG preview image
     */
    fun toggleImagePreview(): Boolean {
        if (previewImage != null) {
            showImagePreview = !showImagePreview
            repaint()
        }
        return showImagePreview
    }
    
    /**
     * Check if currently showing image preview
     */
    fun isShowingImagePreview(): Boolean = showImagePreview
    
    /**
     * Get current view parameters for camera synchronization with OpenSCAD
     * OpenSCAD camera format: --camera=tx,ty,tz,rot_x,rot_y,rot_z,distance
     * 
     * Our viewer coordinate system:
     * - Front view (rotationX=0, rotationY=0): looking at model from +Z axis, Y points up
     * - rotationX: pitch (tilt up/down around X axis)
     * - rotationY: yaw (rotate left/right around Y axis)
     * 
     * OpenSCAD camera coordinate system:
     * - rot_x=0, rot_z=0: looking from above (top-down view)
     * - rot_x=90: front view (looking from +Y toward origin)
     * - rot_z: azimuth rotation (around Z axis)
     * 
     * Mapping:
     * - To get our "front" view in OpenSCAD: need rot_x=90, rot_z=180 (OpenSCAD's Y is our -Y)
     * - Our rotationX adds to the pitch
     * - Our rotationY maps to OpenSCAD's azimuth (rot_z)
     */
    fun getViewParameters(): ViewParameters {
        // OpenSCAD Euler angles: rot_x, rot_y, rot_z applied in order
        // Our viewer: Y-up with rotationX (pitch) and rotationY (yaw)
        // 
        // After testing, -rotationX maps to rot_x (correct top/bottom)
        // rotationY maps to rot_z (correct left/right with this combination)
        // rot_y is used to compensate for the roll/tilt difference
        val rotXDeg = Math.toDegrees(rotationX)
        val rotYDeg = Math.toDegrees(rotationY)
        
        // The roll compensation depends on the viewing angles
        // When looking from an angle, there's an induced roll due to coordinate system difference
        val openscadRotX = -rotXDeg
        val openscadRotY = rotYDeg  // Use rot_y to compensate for tilt
        val openscadRotZ = 0.0
        
        return ViewParameters(
            rotX = openscadRotX,
            rotY = openscadRotY,
            rotZ = openscadRotZ,
            distance = 140.0 / zoom,
            translateX = panX,
            translateY = panY
        )
    }
    
    data class ViewParameters(
        val rotX: Double,
        val rotY: Double,
        val rotZ: Double,
        val distance: Double,
        val translateX: Double,
        val translateY: Double
    )
    
    fun resetView() {
        // Match OpenSCAD's default view: rot_x=55, rot_z=25
        rotationX = Math.toRadians(55.0)
        rotationY = Math.toRadians(25.0)
        rotationZ = 0.0
        zoom = 1.0
        panX = 0.0
        panY = 0.0
        repaint()
    }
    
    // Preset view orientations matching OpenSCAD's View menu
    // OpenSCAD: rot_x is elevation (0=top, 90=front), rot_z is azimuth
    fun setViewFront() { rotationX = Math.toRadians(90.0); rotationY = 0.0; rotationZ = 0.0; repaint() }
    fun setViewBack() { rotationX = Math.toRadians(90.0); rotationY = Math.toRadians(180.0); rotationZ = 0.0; repaint() }
    fun setViewLeft() { rotationX = Math.toRadians(90.0); rotationY = Math.toRadians(90.0); rotationZ = 0.0; repaint() }
    fun setViewRight() { rotationX = Math.toRadians(90.0); rotationY = Math.toRadians(-90.0); rotationZ = 0.0; repaint() }
    fun setViewTop() { rotationX = 0.0; rotationY = 0.0; rotationZ = 0.0; repaint() }
    fun setViewBottom() { rotationX = Math.toRadians(180.0); rotationY = 0.0; rotationZ = 0.0; repaint() }
    
    /**
     * Handle click on orientation cube, returns true if click was on cube
     */
    private fun handleOrientationCubeClick(mouseX: Int, mouseY: Int): Boolean {
        val cubeX = width - cubeSize - cubeMargin
        val cubeY = cubeMargin
        
        // Check if click is within cube bounds
        if (mouseX < cubeX || mouseX > cubeX + cubeSize || 
            mouseY < cubeY || mouseY > cubeY + cubeSize) {
            return false
        }
        
        // Determine which face was clicked based on position within cube
        val relX = mouseX - cubeX
        val relY = mouseY - cubeY
        val centerX = cubeSize / 2
        val centerY = cubeSize / 2
        
        // Simple region detection - divide cube into regions
        val dx = relX - centerX
        val dy = relY - centerY
        val threshold = cubeSize / 4
        
        when {
            dy < -threshold -> setViewTop()      // Top region
            dy > threshold -> setViewBottom()    // Bottom region
            dx < -threshold -> setViewLeft()     // Left region
            dx > threshold -> setViewRight()     // Right region
            else -> setViewFront()               // Center = front
        }
        
        return true
    }
    
    fun toggleWireframe() {
        showWireframe = !showWireframe
        repaint()
    }
    
    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        
        val g2d = g as Graphics2D
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        
        // Show PNG preview if available and enabled
        if (showImagePreview && previewImage != null) {
            drawPreviewImage(g2d, previewImage!!)
            return
        }
        
        val currentColoredModel = coloredModel
        val currentModel = model
        
        if (currentColoredModel != null) {
            drawColoredModel(g2d, currentColoredModel)
        } else if (currentModel != null) {
            drawModel(g2d, currentModel)
        } else {
            drawNoModelMessage(g2d)
            return
        }
        
        // Draw orientation cube overlay
        drawOrientationCube(g2d)
    }
    
    /**
     * Draw orientation cube in upper right corner
     */
    private fun drawOrientationCube(g2d: Graphics2D) {
        val cubeX = width - cubeSize - cubeMargin
        val cubeY = cubeMargin
        val centerX = cubeX + cubeSize / 2
        val centerY = cubeY + cubeSize / 2
        val halfSize = cubeSize / 2.5
        
        // Define cube vertices (centered at origin)
        val vertices = arrayOf(
            doubleArrayOf(-1.0, -1.0, -1.0), // 0: back-bottom-left
            doubleArrayOf( 1.0, -1.0, -1.0), // 1: back-bottom-right
            doubleArrayOf( 1.0,  1.0, -1.0), // 2: back-top-right
            doubleArrayOf(-1.0,  1.0, -1.0), // 3: back-top-left
            doubleArrayOf(-1.0, -1.0,  1.0), // 4: front-bottom-left
            doubleArrayOf( 1.0, -1.0,  1.0), // 5: front-bottom-right
            doubleArrayOf( 1.0,  1.0,  1.0), // 6: front-top-right
            doubleArrayOf(-1.0,  1.0,  1.0)  // 7: front-top-left
        )
        
        // Transform and project vertices
        val projected = vertices.map { v ->
            var x = v[0]
            var y = v[1]
            var z = v[2]
            
            // Rotate around X
            val cosX = cos(rotationX)
            val sinX = sin(rotationX)
            val y1 = y * cosX - z * sinX
            val z1 = y * sinX + z * cosX
            y = y1; z = z1
            
            // Rotate around Y
            val cosY = cos(rotationY)
            val sinY = sin(rotationY)
            val x1 = x * cosY + z * sinY
            val z2 = -x * sinY + z * cosY
            x = x1; z = z2
            
            Pair((centerX + x * halfSize).toInt(), (centerY - y * halfSize).toInt())
        }
        
        // Define faces with vertices, color, and label
        data class CubeFace(val v: IntArray, val color: Color, val label: String, val normalZ: Double)
        
        // Calculate face depths for sorting
        val faces = listOf(
            CubeFace(intArrayOf(4, 5, 6, 7), Color(100, 150, 200), "Front", cos(rotationX) * cos(rotationY)),
            CubeFace(intArrayOf(0, 3, 2, 1), Color(100, 150, 200), "Back", -cos(rotationX) * cos(rotationY)),
            CubeFace(intArrayOf(0, 4, 7, 3), Color(150, 100, 100), "Right", -cos(rotationX) * sin(rotationY)),
            CubeFace(intArrayOf(1, 2, 6, 5), Color(150, 100, 100), "Left", cos(rotationX) * sin(rotationY)),
            CubeFace(intArrayOf(3, 7, 6, 2), Color(100, 180, 100), "Top", sin(rotationX)),
            CubeFace(intArrayOf(0, 1, 5, 4), Color(100, 180, 100), "Bottom", -sin(rotationX))
        ).sortedBy { it.normalZ } // Draw back-to-front
        
        // Draw background
        g2d.color = Color(60, 60, 60, 200)
        g2d.fillRoundRect(cubeX - 5, cubeY - 5, cubeSize + 10, cubeSize + 10, 8, 8)
        
        // Draw faces
        faces.forEach { face ->
            val xPoints = face.v.map { projected[it].first }.toIntArray()
            val yPoints = face.v.map { projected[it].second }.toIntArray()
            
            // Only draw if facing camera (normalZ > 0)
            if (face.normalZ > -0.1) {
                // Adjust brightness based on facing direction
                val brightness = (0.5 + 0.5 * face.normalZ).coerceIn(0.3, 1.0)
                g2d.color = Color(
                    (face.color.red * brightness).toInt(),
                    (face.color.green * brightness).toInt(),
                    (face.color.blue * brightness).toInt()
                )
                g2d.fillPolygon(xPoints, yPoints, 4)
                
                // Draw label if face is mostly visible
                if (face.normalZ > 0.3) {
                    val labelX = xPoints.average().toInt()
                    val labelY = yPoints.average().toInt()
                    g2d.color = Color.WHITE
                    g2d.font = Font("SansSerif", Font.BOLD, 9)
                    val fm = g2d.fontMetrics
                    val labelWidth = fm.stringWidth(face.label)
                    g2d.drawString(face.label, labelX - labelWidth / 2, labelY + fm.ascent / 2 - 2)
                }
            }
            
            // Draw edges
            g2d.color = Color(80, 80, 80)
            g2d.drawPolygon(xPoints, yPoints, 4)
        }
    }
    
    private fun drawPreviewImage(g2d: Graphics2D, image: BufferedImage) {
        // Scale image to fit panel while maintaining aspect ratio
        val panelRatio = width.toDouble() / height.toDouble()
        val imageRatio = image.width.toDouble() / image.height.toDouble()
        
        val (drawWidth, drawHeight) = if (imageRatio > panelRatio) {
            // Image is wider - fit to width
            Pair(width, (width / imageRatio).toInt())
        } else {
            // Image is taller - fit to height
            Pair((height * imageRatio).toInt(), height)
        }
        
        val x = (width - drawWidth) / 2
        val y = (height - drawHeight) / 2
        
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        g2d.drawImage(image, x, y, drawWidth, drawHeight, null)
        
        // Draw info overlay
        g2d.color = Color.LIGHT_GRAY
        g2d.font = Font("SansSerif", Font.PLAIN, 11)
        g2d.drawString("Debug Preview (PNG) - Shows # % ! * modifiers", 10, 15)
        g2d.drawString("Click '3D View' to return to interactive 3D model", 10, 30)
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
    
    private fun drawColoredModel(g2d: Graphics2D, model: ThreeMFParser.ColoredModel) {
        val centerX = width / 2.0 + panX
        val centerY = height / 2.0 + panY
        
        // Calculate scale to fit model in view
        val modelSize = maxOf(model.bounds.size.x, model.bounds.size.y, model.bounds.size.z)
        val viewSize = minOf(width, height) * 0.8
        val scale = if (modelSize > 0) (viewSize / modelSize) * zoom else zoom
        
        // Draw axes
        drawAxesColored(g2d, centerX, centerY, scale, model.bounds.center)
        
        // Project and sort triangles by depth (painter's algorithm)
        data class ProjectedColoredTriangle(
            val p1: Pair<Int, Int>,
            val p2: Pair<Int, Int>,
            val p3: Pair<Int, Int>,
            val depth: Double,
            val normal: ThreeMFParser.Vector3,
            val color: Color
        )
        
        val projectedTriangles = model.triangles.map { triangle ->
            val p1 = projectColored(triangle.v1, model.bounds.center, centerX, centerY, scale)
            val p2 = projectColored(triangle.v2, model.bounds.center, centerX, centerY, scale)
            val p3 = projectColored(triangle.v3, model.bounds.center, centerX, centerY, scale)
            
            // Calculate average depth for sorting
            val depth = (
                getDepthColored(triangle.v1, model.bounds.center) +
                getDepthColored(triangle.v2, model.bounds.center) +
                getDepthColored(triangle.v3, model.bounds.center)
            ) / 3.0
            
            ProjectedColoredTriangle(p1, p2, p3, depth, triangle.normal, triangle.color)
        }.sortedBy { it.depth } // Draw far to near
        
        // Calculate view direction
        val cosX = cos(rotationX)
        val sinX = sin(rotationX)
        val cosY = cos(rotationY)
        val sinY = sin(rotationY)
        
        // Draw triangles with per-triangle colors
        projectedTriangles.forEach { tri ->
            // Transform normal by view rotation
            var nx = tri.normal.x.toDouble()
            var ny = tri.normal.y.toDouble()
            var nz = tri.normal.z.toDouble()
            
            val ny1 = ny * cosX - nz * sinX
            val nz1 = ny * sinX + nz * cosX
            ny = ny1
            nz = nz1
            
            val nx1 = nx * cosY + nz * sinY
            val nz2 = -nx * sinY + nz * cosY
            nz = nz2
            
            // Backface culling
            if (nz <= 0) {
                return@forEach
            }
            
            // Brightness based on facing direction
            val brightness = (0.6f + 0.4f * nz.toFloat()).coerceIn(0.6f, 1.0f)
            
            // Apply brightness to triangle's color
            val r = (tri.color.red * brightness).toInt().coerceIn(0, 255)
            val g = (tri.color.green * brightness).toInt().coerceIn(0, 255)
            val b = (tri.color.blue * brightness).toInt().coerceIn(0, 255)
            val faceColor = Color(r, g, b)
            
            if (showWireframe) {
                g2d.color = faceColor
                g2d.drawLine(tri.p1.first, tri.p1.second, tri.p2.first, tri.p2.second)
                g2d.drawLine(tri.p2.first, tri.p2.second, tri.p3.first, tri.p3.second)
                g2d.drawLine(tri.p3.first, tri.p3.second, tri.p1.first, tri.p1.second)
            } else {
                g2d.color = faceColor
                val xPoints = intArrayOf(tri.p1.first, tri.p2.first, tri.p3.first)
                val yPoints = intArrayOf(tri.p1.second, tri.p2.second, tri.p3.second)
                g2d.fillPolygon(xPoints, yPoints, 3)
            }
        }
        
        // Draw info
        drawColoredInfo(g2d, model)
        
        // Draw orientation cube
        drawOrientationCube(g2d)
    }
    
    private fun projectColored(point: ThreeMFParser.Vector3, modelCenter: ThreeMFParser.Vector3, 
                               centerX: Double, centerY: Double, scale: Double): Pair<Int, Int> {
        var x = (point.x - modelCenter.x).toDouble()
        var y = (point.y - modelCenter.y).toDouble()
        var z = (point.z - modelCenter.z).toDouble()
        
        // Rotate around X
        val cosX = cos(rotationX)
        val sinX = sin(rotationX)
        val y1 = y * cosX - z * sinX
        val z1 = y * sinX + z * cosX
        y = y1
        z = z1
        
        // Rotate around Y
        val cosY = cos(rotationY)
        val sinY = sin(rotationY)
        val x1 = x * cosY + z * sinY
        x = x1
        
        return Pair((centerX + x * scale).toInt(), (centerY - y * scale).toInt())
    }
    
    private fun getDepthColored(point: ThreeMFParser.Vector3, modelCenter: ThreeMFParser.Vector3): Double {
        var x = (point.x - modelCenter.x).toDouble()
        var y = (point.y - modelCenter.y).toDouble()
        var z = (point.z - modelCenter.z).toDouble()
        
        val cosX = cos(rotationX)
        val sinX = sin(rotationX)
        val y1 = y * cosX - z * sinX
        val z1 = y * sinX + z * cosX
        y = y1
        z = z1
        
        val cosY = cos(rotationY)
        val sinY = sin(rotationY)
        val z2 = -x * sinY + z * cosY
        
        return z2
    }
    
    private fun drawAxesColored(g2d: Graphics2D, centerX: Double, centerY: Double, scale: Double, 
                                modelCenter: ThreeMFParser.Vector3) {
        val axisLength = 50.0
        val origin = ThreeMFParser.Vector3(modelCenter.x, modelCenter.y, modelCenter.z)
        
        // X axis (red)
        g2d.color = Color.RED
        val xEnd = projectColored(ThreeMFParser.Vector3(modelCenter.x + axisLength.toFloat(), modelCenter.y, modelCenter.z), 
                                  origin, centerX, centerY, scale)
        g2d.drawLine(centerX.toInt(), centerY.toInt(), xEnd.first, xEnd.second)
        
        // Y axis (green)
        g2d.color = Color.GREEN
        val yEnd = projectColored(ThreeMFParser.Vector3(modelCenter.x, modelCenter.y + axisLength.toFloat(), modelCenter.z), 
                                  origin, centerX, centerY, scale)
        g2d.drawLine(centerX.toInt(), centerY.toInt(), yEnd.first, yEnd.second)
        
        // Z axis (blue)
        g2d.color = Color.BLUE
        val zEnd = projectColored(ThreeMFParser.Vector3(modelCenter.x, modelCenter.y, modelCenter.z + axisLength.toFloat()), 
                                  origin, centerX, centerY, scale)
        g2d.drawLine(centerX.toInt(), centerY.toInt(), zEnd.first, zEnd.second)
    }
    
    private fun drawColoredInfo(g2d: Graphics2D, model: ThreeMFParser.ColoredModel) {
        g2d.color = Color.WHITE
        g2d.font = Font("SansSerif", Font.PLAIN, 11)
        g2d.drawString("Triangles: ${model.triangles.size} (with colors)", 10, height - 10)
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
        
        // Calculate view direction (camera looks down -Z axis after rotations)
        // We need to transform the normal by the same rotation to get view-relative lighting
        val cosX = cos(rotationX)
        val sinX = sin(rotationX)
        val cosY = cos(rotationY)
        val sinY = sin(rotationY)
        
        // Draw triangles
        projectedTriangles.forEach { tri ->
            // Transform normal by view rotation to get camera-relative orientation
            var nx = tri.normal.x.toDouble()
            var ny = tri.normal.y.toDouble()
            var nz = tri.normal.z.toDouble()
            
            // Rotate normal around X axis
            val ny1 = ny * cosX - nz * sinX
            val nz1 = ny * sinX + nz * cosX
            ny = ny1
            nz = nz1
            
            // Rotate normal around Y axis
            val nx1 = nx * cosY + nz * sinY
            val nz2 = -nx * sinY + nz * cosY
            nx = nx1
            nz = nz2
            
            // nz now represents how much the face points toward the camera
            // Positive nz = facing camera, negative = facing away
            
            // Backface culling - skip triangles facing away from camera
            if (nz <= 0) {
                return@forEach
            }
            
            // Brightness based on how directly the face points at camera
            val brightness = (0.6f + 0.4f * nz.toFloat()).coerceIn(0.6f, 1.0f)
            
            // Base color: #c1c1c1 (light gray)
            val r = (193 * brightness).toInt()
            val g = (193 * brightness).toInt()
            val b = (193 * brightness).toInt()
            val faceColor = Color(r, g, b)
            
            if (showWireframe) {
                // Wireframe mode
                g2d.color = faceColor
                g2d.drawLine(tri.p1.first, tri.p1.second, tri.p2.first, tri.p2.second)
                g2d.drawLine(tri.p2.first, tri.p2.second, tri.p3.first, tri.p3.second)
                g2d.drawLine(tri.p3.first, tri.p3.second, tri.p1.first, tri.p1.second)
            } else {
                // Solid mode - only front-facing surfaces
                g2d.color = faceColor
                val xPoints = intArrayOf(tri.p1.first, tri.p2.first, tri.p3.first)
                val yPoints = intArrayOf(tri.p1.second, tri.p2.second, tri.p3.second)
                g2d.fillPolygon(xPoints, yPoints, 3)
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
