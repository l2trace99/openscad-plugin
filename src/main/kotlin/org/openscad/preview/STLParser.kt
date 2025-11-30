package org.openscad.preview

import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Path
import kotlin.math.sqrt

/**
 * Simple STL file parser supporting both ASCII and binary formats
 */
class STLParser {
    
    data class Triangle(
        val normal: Vector3,
        val v1: Vector3,
        val v2: Vector3,
        val v3: Vector3
    )
    
    data class Vector3(val x: Float, val y: Float, val z: Float) {
        fun length(): Float = sqrt(x * x + y * y + z * z)
        operator fun plus(other: Vector3) = Vector3(x + other.x, y + other.y, z + other.z)
        operator fun minus(other: Vector3) = Vector3(x - other.x, y - other.y, z - other.z)
        operator fun times(scalar: Float) = Vector3(x * scalar, y * scalar, z * scalar)
        operator fun div(scalar: Float) = Vector3(x / scalar, y / scalar, z / scalar)
    }
    
    data class STLModel(
        val triangles: List<Triangle>,
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
    
    /**
     * Parse an STL file (auto-detects ASCII vs binary)
     */
    fun parse(path: Path): STLModel? {
        return try {
            if (isBinarySTL(path)) {
                parseBinarySTL(path)
            } else {
                parseASCIISTL(path)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Check if file is binary STL (vs ASCII)
     */
    private fun isBinarySTL(path: Path): Boolean {
        FileInputStream(path.toFile()).use { fis ->
            val header = ByteArray(80)
            fis.read(header)
            val headerStr = String(header).lowercase()
            return !headerStr.startsWith("solid")
        }
    }
    
    /**
     * Parse binary STL format
     */
    private fun parseBinarySTL(path: Path): STLModel {
        val triangles = mutableListOf<Triangle>()
        
        DataInputStream(BufferedInputStream(FileInputStream(path.toFile()))).use { dis ->
            // Skip 80-byte header
            dis.skipBytes(80)
            
            // Read number of triangles
            val numTriangles = readLittleEndianInt(dis)
            
            // Read each triangle
            repeat(numTriangles) {
                val normal = Vector3(
                    readLittleEndianFloat(dis),
                    readLittleEndianFloat(dis),
                    readLittleEndianFloat(dis)
                )
                
                val v1 = Vector3(
                    readLittleEndianFloat(dis),
                    readLittleEndianFloat(dis),
                    readLittleEndianFloat(dis)
                )
                
                val v2 = Vector3(
                    readLittleEndianFloat(dis),
                    readLittleEndianFloat(dis),
                    readLittleEndianFloat(dis)
                )
                
                val v3 = Vector3(
                    readLittleEndianFloat(dis),
                    readLittleEndianFloat(dis),
                    readLittleEndianFloat(dis)
                )
                
                // Skip attribute byte count
                dis.skipBytes(2)
                
                triangles.add(Triangle(normal, v1, v2, v3))
            }
        }
        
        return STLModel(triangles, calculateBounds(triangles))
    }
    
    /**
     * Parse ASCII STL format
     */
    private fun parseASCIISTL(path: Path): STLModel {
        val triangles = mutableListOf<Triangle>()
        val lines = path.toFile().readLines()
        
        var i = 0
        while (i < lines.size) {
            val line = lines[i].trim()
            
            if (line.startsWith("facet normal")) {
                val normalParts = line.substringAfter("facet normal").trim().split("\\s+".toRegex())
                val normal = Vector3(
                    normalParts[0].toFloat(),
                    normalParts[1].toFloat(),
                    normalParts[2].toFloat()
                )
                
                // Skip "outer loop"
                i++
                
                // Read 3 vertices
                val vertices = mutableListOf<Vector3>()
                repeat(3) {
                    i++
                    val vertexLine = lines[i].trim()
                    if (vertexLine.startsWith("vertex")) {
                        val parts = vertexLine.substringAfter("vertex").trim().split("\\s+".toRegex())
                        vertices.add(Vector3(
                            parts[0].toFloat(),
                            parts[1].toFloat(),
                            parts[2].toFloat()
                        ))
                    }
                }
                
                if (vertices.size == 3) {
                    triangles.add(Triangle(normal, vertices[0], vertices[1], vertices[2]))
                }
                
                // Skip "endloop" and "endfacet"
                i += 2
            }
            
            i++
        }
        
        return STLModel(triangles, calculateBounds(triangles))
    }
    
    private fun readLittleEndianInt(dis: DataInputStream): Int {
        val bytes = ByteArray(4)
        dis.readFully(bytes)
        return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).int
    }
    
    private fun readLittleEndianFloat(dis: DataInputStream): Float {
        val bytes = ByteArray(4)
        dis.readFully(bytes)
        return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).float
    }
    
    private fun calculateBounds(triangles: List<Triangle>): STLModel.Bounds {
        if (triangles.isEmpty()) {
            return STLModel.Bounds(Vector3(0f, 0f, 0f), Vector3(0f, 0f, 0f))
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
        
        return STLModel.Bounds(
            Vector3(minX, minY, minZ),
            Vector3(maxX, maxY, maxZ)
        )
    }
}
