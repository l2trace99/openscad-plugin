package org.openscad.preview

import com.intellij.openapi.project.Project
import com.jogamp.opengl.*
import com.jogamp.opengl.awt.GLCanvas
import com.jogamp.opengl.util.FPSAnimator
import org.openscad.settings.OpenSCADSettings
import java.awt.event.*
import javax.swing.JPanel
import java.awt.BorderLayout
import kotlin.math.cos
import kotlin.math.sin

/**
 * Advanced 3D STL viewer using JOGL (Java OpenGL)
 * Provides hardware-accelerated rendering with lighting and smooth shading
 */
class STLViewer3D(private val project: Project) : JPanel(BorderLayout()), GLEventListener {

    private var model: STLParser.STLModel? = null
    private var coloredModel: ThreeMFParser.ColoredModel? = null
    private val canvas: GLCanvas
    private val animator: FPSAnimator

    // Camera/view parameters
    private var rotationX = 30.0f
    private var rotationY = 45.0f
    private var zoom = 1.0f
    private var panX = 0.0f
    private var panY = 0.0f

    // Mouse interaction
    private var lastMouseX = 0
    private var lastMouseY = 0
    private var isDragging = false
    private var isPanning = false

    init {
        val caps = GLCapabilities(GLProfile.get(GLProfile.GL2))
        caps.doubleBuffered = true
        caps.hardwareAccelerated = true

        canvas = GLCanvas(caps)
        canvas.addGLEventListener(this)

        // Setup mouse controls
        setupMouseControls()

        add(canvas, BorderLayout.CENTER)

        // Start animation loop
        animator = FPSAnimator(canvas, 60)
        animator.start()
    }

    private fun setupMouseControls() {
        canvas.addMouseListener(object : MouseAdapter() {
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

        canvas.addMouseMotionListener(object : MouseMotionAdapter() {
            override fun mouseDragged(e: MouseEvent) {
                val dx = e.x - lastMouseX
                val dy = e.y - lastMouseY

                if (isDragging) {
                    rotationY += dx * 0.5f
                    rotationX += dy * 0.5f
                } else if (isPanning) {
                    panX += dx * 0.01f
                    panY -= dy * 0.01f
                }

                lastMouseX = e.x
                lastMouseY = e.y
            }
        })

        canvas.addMouseWheelListener { e ->
            val zoomFactor = if (e.wheelRotation < 0) 1.1f else 0.9f
            zoom *= zoomFactor
            zoom = zoom.coerceIn(0.1f, 10.0f)
        }
    }

    fun setModel(model: STLParser.STLModel?) {
        this.model = model
        this.coloredModel = null
        resetView()
        repaint()
    }

    fun setColoredModel(model: ThreeMFParser.ColoredModel?) {
        this.coloredModel = model
        this.model = null
        resetView()
        repaint()
    }

    fun resetView() {
        rotationX = 30.0f
        rotationY = 45.0f
        zoom = 1.0f
        panX = 0.0f
        panY = 0.0f
        repaint()
    }

    override fun init(drawable: GLAutoDrawable) {
        val gl = drawable.gl.gL2

        // Enable depth testing
        gl.glEnable(GL.GL_DEPTH_TEST)
        gl.glDepthFunc(GL.GL_LEQUAL)

        // Enable back-face culling (STL models are closed solids)
        gl.glEnable(GL.GL_CULL_FACE)
        gl.glCullFace(GL.GL_BACK)

        // Enable lighting
        gl.glEnable(GL2.GL_LIGHTING)
        gl.glEnable(GL2.GL_LIGHT0)

        // Setup light
        val lightPos = floatArrayOf(1.0f, 1.0f, 1.0f, 0.0f)
        val lightAmbient = floatArrayOf(0.2f, 0.2f, 0.2f, 1.0f)
        val lightDiffuse = floatArrayOf(0.7f, 0.7f, 0.7f, 1.0f)
        val lightSpecular = floatArrayOf(0.4f, 0.4f, 0.4f, 1.0f)

        // Disable global ambient (defaults to 0.2, stacks on top of per-light ambient)
        gl.glLightModelfv(GL2.GL_LIGHT_MODEL_AMBIENT, floatArrayOf(0.0f, 0.0f, 0.0f, 1.0f), 0)

        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_POSITION, lightPos, 0)
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_AMBIENT, lightAmbient, 0)
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_DIFFUSE, lightDiffuse, 0)
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_SPECULAR, lightSpecular, 0)

        // Renormalize normals after model scaling (glScalef stretches normals,
        // causing blown-out lighting without this)
        gl.glEnable(GL2.GL_NORMALIZE)

        // Enable smooth shading
        gl.glShadeModel(GL2.GL_SMOOTH)

        // Background color - OpenSCAD default (light gray)
        gl.glClearColor(0.898f, 0.898f, 0.898f, 1.0f)
    }

    override fun display(drawable: GLAutoDrawable) {
        val gl = drawable.gl.gL2

        gl.glClear(GL.GL_COLOR_BUFFER_BIT or GL.GL_DEPTH_BUFFER_BIT)
        gl.glLoadIdentity()

        // Setup camera
        gl.glTranslatef(panX, panY, -5.0f * zoom)
        gl.glRotatef(rotationX, 1.0f, 0.0f, 0.0f)
        gl.glRotatef(rotationY, 0.0f, 1.0f, 0.0f)

        // Draw grid first (under the model)
        val settings = OpenSCADSettings.getInstance(project)
        if (settings.showGrid) {
            drawGrid(gl, settings.gridSize, settings.gridSpacing)
        }

        val currentColoredModel = coloredModel
        val currentModel = model

        if (currentColoredModel != null) {
            drawColoredModel(gl, currentColoredModel)
        } else if (currentModel != null) {
            drawModel(gl, currentModel)
        }
    }

    private fun drawModel(gl: GL2, model: STLParser.STLModel) {
        // Center and scale model
        val center = model.bounds.center
        val size = model.bounds.size
        val maxSize = maxOf(size.x, size.y, size.z)
        val scale = 2.0f / maxSize

        gl.glPushMatrix()
        gl.glScalef(scale, scale, scale)
        gl.glTranslatef(-center.x, -center.y, -center.z)

        // Set material properties - OpenSCAD default (coral/orange)
        val matAmbient = floatArrayOf(0.95f, 0.55f, 0.35f, 1.0f)
        val matDiffuse = floatArrayOf(0.95f, 0.55f, 0.35f, 1.0f)
        val matSpecular = floatArrayOf(0.3f, 0.3f, 0.3f, 1.0f)
        val matShininess = floatArrayOf(64.0f)

        gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL2.GL_AMBIENT, matAmbient, 0)
        gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL2.GL_DIFFUSE, matDiffuse, 0)
        gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL2.GL_SPECULAR, matSpecular, 0)
        gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL2.GL_SHININESS, matShininess, 0)

        // Draw triangles
        gl.glBegin(GL2.GL_TRIANGLES)
        model.triangles.forEach { triangle ->
            // Set normal for lighting
            gl.glNormal3f(triangle.normal.x, triangle.normal.y, triangle.normal.z)

            // Draw vertices
            gl.glVertex3f(triangle.v1.x, triangle.v1.y, triangle.v1.z)
            gl.glVertex3f(triangle.v2.x, triangle.v2.y, triangle.v2.z)
            gl.glVertex3f(triangle.v3.x, triangle.v3.y, triangle.v3.z)
        }
        gl.glEnd()

        gl.glPopMatrix()

        // Draw axes
        drawAxes(gl)
    }

    private fun drawGrid(gl: GL2, gridSize: Float, gridSpacing: Float) {
        gl.glDisable(GL2.GL_LIGHTING)
        gl.glDisable(GL.GL_DEPTH_TEST)

        val halfSize = gridSize / 2.0f
        val numLines = (gridSize / gridSpacing).toInt()

        gl.glBegin(GL.GL_LINES)

        // Grid lines parallel to X axis
        for (i in -numLines..numLines) {
            val y = i * gridSpacing
            if (i == 0) {
                // Center line (darker)
                gl.glColor3f(0.5f, 0.5f, 0.5f)
            } else {
                // Regular grid lines (lighter)
                gl.glColor3f(0.7f, 0.7f, 0.7f)
            }
            gl.glVertex3f(-halfSize, y, 0.0f)
            gl.glVertex3f(halfSize, y, 0.0f)
        }

        // Grid lines parallel to Y axis
        for (i in -numLines..numLines) {
            val x = i * gridSpacing
            if (i == 0) {
                // Center line (darker)
                gl.glColor3f(0.5f, 0.5f, 0.5f)
            } else {
                // Regular grid lines (lighter)
                gl.glColor3f(0.7f, 0.7f, 0.7f)
            }
            gl.glVertex3f(x, -halfSize, 0.0f)
            gl.glVertex3f(x, halfSize, 0.0f)
        }

        gl.glEnd()

        gl.glEnable(GL.GL_DEPTH_TEST)
        gl.glEnable(GL2.GL_LIGHTING)
    }

    private fun drawColoredModel(gl: GL2, model: ThreeMFParser.ColoredModel) {
        // Center and scale model
        val center = model.bounds.center
        val size = model.bounds.size
        val maxSize = maxOf(size.x, size.y, size.z)
        val scale = if (maxSize > 0) 2.0f / maxSize else 1.0f

        gl.glPushMatrix()
        gl.glScalef(scale, scale, scale)
        gl.glTranslatef(-center.x, -center.y, -center.z)

        // Enable color material so vertex colors affect lighting
        gl.glEnable(GL2.GL_COLOR_MATERIAL)
        gl.glColorMaterial(GL.GL_FRONT_AND_BACK, GL2.GL_AMBIENT_AND_DIFFUSE)

        // Set specular properties (shared for all triangles)
        val matSpecular = floatArrayOf(0.3f, 0.3f, 0.3f, 1.0f)
        val matShininess = floatArrayOf(64.0f)
        gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL2.GL_SPECULAR, matSpecular, 0)
        gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL2.GL_SHININESS, matShininess, 0)

        // Draw triangles with per-triangle colors
        gl.glBegin(GL2.GL_TRIANGLES)
        model.triangles.forEach { triangle ->
            // Set color for this triangle
            gl.glColor3f(
                triangle.color.red / 255.0f,
                triangle.color.green / 255.0f,
                triangle.color.blue / 255.0f
            )

            // Set normal for lighting
            gl.glNormal3f(triangle.normal.x, triangle.normal.y, triangle.normal.z)

            // Draw vertices
            gl.glVertex3f(triangle.v1.x, triangle.v1.y, triangle.v1.z)
            gl.glVertex3f(triangle.v2.x, triangle.v2.y, triangle.v2.z)
            gl.glVertex3f(triangle.v3.x, triangle.v3.y, triangle.v3.z)
        }
        gl.glEnd()

        // Disable color material to restore state
        gl.glDisable(GL2.GL_COLOR_MATERIAL)

        gl.glPopMatrix()

        // Draw axes
        drawAxes(gl)
    }

    private fun drawAxes(gl: GL2) {
        gl.glDisable(GL2.GL_LIGHTING)
        gl.glBegin(GL.GL_LINES)

        // X axis (red)
        gl.glColor3f(1.0f, 0.0f, 0.0f)
        gl.glVertex3f(0.0f, 0.0f, 0.0f)
        gl.glVertex3f(1.0f, 0.0f, 0.0f)

        // Y axis (green)
        gl.glColor3f(0.0f, 1.0f, 0.0f)
        gl.glVertex3f(0.0f, 0.0f, 0.0f)
        gl.glVertex3f(0.0f, 1.0f, 0.0f)

        // Z axis (blue)
        gl.glColor3f(0.0f, 0.0f, 1.0f)
        gl.glVertex3f(0.0f, 0.0f, 0.0f)
        gl.glVertex3f(0.0f, 0.0f, 1.0f)

        gl.glEnd()
        gl.glEnable(GL2.GL_LIGHTING)
    }

    override fun reshape(drawable: GLAutoDrawable, x: Int, y: Int, width: Int, height: Int) {
        val gl = drawable.gl.gL2

        gl.glViewport(0, 0, width, height)
        gl.glMatrixMode(GL2.GL_PROJECTION)
        gl.glLoadIdentity()

        val aspect = width.toFloat() / height.toFloat()
        val fovy = 45.0
        val zNear = 0.1
        val zFar = 100.0

        val fH = Math.tan(fovy / 360.0 * Math.PI) * zNear
        val fW = fH * aspect
        gl.glFrustum(-fW, fW, -fH, fH, zNear, zFar)

        gl.glMatrixMode(GL2.GL_MODELVIEW)
        gl.glLoadIdentity()
    }

    override fun dispose(drawable: GLAutoDrawable) {
        animator.stop()
    }
}
