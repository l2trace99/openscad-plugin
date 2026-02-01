package org.openscad.preview

import com.intellij.openapi.diagnostic.Logger
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import java.awt.Color
import java.nio.file.Path
import java.util.zip.ZipFile
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.math.sqrt

/**
 * Parser for 3MF files that extracts mesh data with color information.
 * 3MF format preserves colors from OpenSCAD's color() statements.
 */
class ThreeMFParser {
    
    private val logger = Logger.getInstance(ThreeMFParser::class.java)
    
    /**
     * Parse a 3MF file and return a colored model
     */
    fun parse(path: Path): ColoredModel? {
        return try {
            logger.info("Parsing 3MF file: $path")
            ZipFile(path.toFile()).use { zip ->
                // Log all entries for debugging
                val entries = zip.entries().toList()
                logger.info("3MF contains ${entries.size} entries: ${entries.map { it.name }}")
                
                // 3MF files contain a 3D/3dmodel.model XML file
                val modelEntry = zip.getEntry("3D/3dmodel.model") 
                    ?: zip.getEntry("3d/3dmodel.model")
                    ?: run {
                        logger.warn("No 3dmodel.model found in 3MF file")
                        return null
                    }
                
                val inputStream = zip.getInputStream(modelEntry)
                val factory = DocumentBuilderFactory.newInstance()
                factory.isNamespaceAware = true
                val docBuilder = factory.newDocumentBuilder()
                val doc = docBuilder.parse(inputStream)
                
                val result = parseModelXml(doc.documentElement)
                logger.info("Parsed ${result.triangles.size} triangles from 3MF")
                
                // Log color info
                val uniqueColors = result.triangles.map { it.color }.distinct()
                logger.info("Found ${uniqueColors.size} unique colors: $uniqueColors")
                
                result
            }
        } catch (e: Exception) {
            logger.warn("Failed to parse 3MF file: ${path}", e)
            null
        }
    }
    
    private fun parseModelXml(root: Element): ColoredModel {
        val triangles = mutableListOf<ColoredTriangle>()
        val colors = mutableMapOf<String, Color>()
        
        logger.info("Root element: ${root.tagName}, namespace: ${root.namespaceURI}")
        
        // Parse basematerials for colors - use wildcard namespace since 3MF has default namespace
        val baseMaterialsNodes = root.getElementsByTagNameNS("*", "basematerials")
        logger.info("Found ${baseMaterialsNodes.length} basematerials elements")
        for (i in 0 until baseMaterialsNodes.length) {
            val baseMaterials = baseMaterialsNodes.item(i) as? Element ?: continue
            val baseId = baseMaterials.getAttribute("id")
            val bases = baseMaterials.getElementsByTagNameNS("*", "base")
            logger.info("Basematerials id=$baseId has ${bases.length} base elements")
            for (j in 0 until bases.length) {
                val base = bases.item(j) as? Element ?: continue
                val colorStr = base.getAttribute("displaycolor")
                logger.info("  Base $j: displaycolor=$colorStr")
                if (colorStr.isNotEmpty()) {
                    val color = parseColor(colorStr)
                    if (color != null) {
                        colors["$baseId:$j"] = color
                        logger.info("  Mapped color $baseId:$j -> $color")
                    }
                }
            }
        }
        
        logger.info("Color map after parsing basematerials: $colors")
        
        // Parse mesh objects - use wildcard namespace
        val meshNodes = root.getElementsByTagNameNS("*", "mesh")
        logger.info("Found ${meshNodes.length} mesh elements")
        for (i in 0 until meshNodes.length) {
            val mesh = meshNodes.item(i) as? Element ?: continue
            
            // Get parent object for default material
            val parentObject = findParentObject(mesh)
            val defaultPid = parentObject?.getAttribute("pid") ?: ""
            val defaultPindex = parentObject?.getAttribute("pindex") ?: "0"
            val defaultColor = colors["$defaultPid:$defaultPindex"] ?: DEFAULT_COLOR
            logger.info("Mesh $i: defaultPid=$defaultPid, defaultPindex=$defaultPindex, defaultColor=$defaultColor")
            
            // Parse vertices - use wildcard namespace
            val vertices = mutableListOf<Vector3>()
            val verticesNode = mesh.getElementsByTagNameNS("*", "vertices").item(0) as? Element
            if (verticesNode != null) {
                val vertexNodes = verticesNode.getElementsByTagNameNS("*", "vertex")
                for (j in 0 until vertexNodes.length) {
                    val vertex = vertexNodes.item(j) as? Element ?: continue
                    val x = vertex.getAttribute("x").toFloatOrNull() ?: 0f
                    val y = vertex.getAttribute("y").toFloatOrNull() ?: 0f
                    val z = vertex.getAttribute("z").toFloatOrNull() ?: 0f
                    vertices.add(Vector3(x, y, z))
                }
            }
            logger.info("Parsed ${vertices.size} vertices")
            
            // Parse triangles - use wildcard namespace
            val trianglesNode = mesh.getElementsByTagNameNS("*", "triangles").item(0) as? Element
            if (trianglesNode != null) {
                val triangleNodes = trianglesNode.getElementsByTagNameNS("*", "triangle")
                for (j in 0 until triangleNodes.length) {
                    val triangle = triangleNodes.item(j) as? Element ?: continue
                    
                    val v1Idx = triangle.getAttribute("v1").toIntOrNull() ?: continue
                    val v2Idx = triangle.getAttribute("v2").toIntOrNull() ?: continue
                    val v3Idx = triangle.getAttribute("v3").toIntOrNull() ?: continue
                    
                    if (v1Idx >= vertices.size || v2Idx >= vertices.size || v3Idx >= vertices.size) continue
                    
                    val v1 = vertices[v1Idx]
                    val v2 = vertices[v2Idx]
                    val v3 = vertices[v3Idx]
                    
                    // Calculate normal
                    val normal = calculateNormal(v1, v2, v3)
                    
                    // Get triangle color from pid/p1 attributes
                    val pid = triangle.getAttribute("pid").takeIf { it.isNotEmpty() } ?: defaultPid
                    val p1 = triangle.getAttribute("p1").takeIf { it.isNotEmpty() } ?: defaultPindex
                    val color = colors["$pid:$p1"] ?: defaultColor
                    
                    triangles.add(ColoredTriangle(normal, v1, v2, v3, color))
                }
            }
        }
        
        return ColoredModel(triangles, calculateBounds(triangles))
    }
    
    private fun findParentObject(element: Element): Element? {
        var parent = element.parentNode
        while (parent != null) {
            if (parent is Element && parent.tagName == "object") {
                return parent
            }
            parent = parent.parentNode
        }
        return null
    }
    
    private fun parseColor(colorStr: String): Color? {
        return try {
            // 3MF colors are in #RRGGBB or #RRGGBBAA format
            val hex = colorStr.removePrefix("#")
            when (hex.length) {
                6 -> Color(
                    hex.substring(0, 2).toInt(16),
                    hex.substring(2, 4).toInt(16),
                    hex.substring(4, 6).toInt(16)
                )
                8 -> Color(
                    hex.substring(0, 2).toInt(16),
                    hex.substring(2, 4).toInt(16),
                    hex.substring(4, 6).toInt(16),
                    hex.substring(6, 8).toInt(16)
                )
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    private fun calculateNormal(v1: Vector3, v2: Vector3, v3: Vector3): Vector3 {
        val u = v2 - v1
        val v = v3 - v1
        val nx = u.y * v.z - u.z * v.y
        val ny = u.z * v.x - u.x * v.z
        val nz = u.x * v.y - u.y * v.x
        val len = sqrt(nx * nx + ny * ny + nz * nz)
        return if (len > 0) Vector3(nx / len, ny / len, nz / len) else Vector3(0f, 0f, 1f)
    }
    
    private fun calculateBounds(triangles: List<ColoredTriangle>): ColoredModel.Bounds {
        if (triangles.isEmpty()) {
            return ColoredModel.Bounds(Vector3(0f, 0f, 0f), Vector3(0f, 0f, 0f))
        }
        
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var minZ = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE
        var maxZ = Float.MIN_VALUE
        
        triangles.forEach { triangle ->
            listOf(triangle.v1, triangle.v2, triangle.v3).forEach { v ->
                minX = minOf(minX, v.x)
                minY = minOf(minY, v.y)
                minZ = minOf(minZ, v.z)
                maxX = maxOf(maxX, v.x)
                maxY = maxOf(maxY, v.y)
                maxZ = maxOf(maxZ, v.z)
            }
        }
        
        return ColoredModel.Bounds(Vector3(minX, minY, minZ), Vector3(maxX, maxY, maxZ))
    }
    
    data class Vector3(val x: Float, val y: Float, val z: Float) {
        operator fun minus(other: Vector3) = Vector3(x - other.x, y - other.y, z - other.z)
        operator fun plus(other: Vector3) = Vector3(x + other.x, y + other.y, z + other.z)
        operator fun div(scalar: Float) = Vector3(x / scalar, y / scalar, z / scalar)
    }
    
    data class ColoredTriangle(
        val normal: Vector3,
        val v1: Vector3,
        val v2: Vector3,
        val v3: Vector3,
        val color: Color
    )
    
    data class ColoredModel(
        val triangles: List<ColoredTriangle>,
        val bounds: Bounds
    ) {
        data class Bounds(
            val min: Vector3,
            val max: Vector3
        ) {
            val center: Vector3 get() = (min + max) / 2f
            val size: Vector3 get() = max - min
        }
    }
    
    companion object {
        val DEFAULT_COLOR: Color = Color(242, 140, 89) // OpenSCAD default coral/orange
    }
}
