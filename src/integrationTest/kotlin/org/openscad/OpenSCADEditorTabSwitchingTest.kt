package org.openscad

import org.junit.Assert.*
import org.junit.Test
import org.openscad.util.OpenSCADPathUtils
import java.nio.file.Files

/**
 * Integration tests for OpenSCAD editor tab switching freeze bug fixes.
 *
 * These tests verify the specific fixes that resolve the freeze issue when
 * switching between rendered OpenSCAD files:
 *
 * 1. STLViewer3D has pauseAnimator(), resumeAnimator(), and cleanup() methods
 *    (fixes the freeze by pausing inactive animators)
 *
 * 2. OpenSCADPathUtils tracks active directories in a Map<Path, Long>
 *    (fixes the race condition where cleanup deleted active directories)
 *
 * These tests WILL FAIL if the bug fixes are reverted.
 */
class OpenSCADEditorTabSwitchingTest {

    /**
     * CRITICAL TEST: Verifies STLViewer3D has the animator control methods
     * that fix the tab switching freeze issue.
     *
     * EXPECTED TO FAIL if code is reverted: NoSuchMethodException
     *
     * These methods enable pausing the animator when a tab becomes inactive
     * and resuming it when the user switches back, preventing multiple
     * animators from competing for OpenGL resources.
     */
    @Test
    fun testSTLViewer3DHasAnimatorControlMethods() {
        val clazz = Class.forName("org.openscad.preview.STLViewer3D")

        // Verify pauseAnimator() method exists (pauses animator when tab becomes inactive)
        val pauseMethod = clazz.getDeclaredMethod("pauseAnimator")
        assertNotNull("STLViewer3D must have pauseAnimator() method to fix freeze bug", pauseMethod)
        assertEquals("pauseAnimator should return void", Void.TYPE, pauseMethod.returnType)

        // Verify resumeAnimator() method exists (resumes animator when tab becomes active)
        val resumeMethod = clazz.getDeclaredMethod("resumeAnimator")
        assertNotNull("STLViewer3D must have resumeAnimator() method to fix freeze bug", resumeMethod)
        assertEquals("resumeAnimator should return void", Void.TYPE, resumeMethod.returnType)

        // Verify cleanup() method exists (stops animator and disposes resources on editor close)
        val cleanupMethod = clazz.getDeclaredMethod("cleanup")
        assertNotNull("STLViewer3D must have cleanup() method to prevent resource leaks", cleanupMethod)
        assertEquals("cleanup should return void", Void.TYPE, cleanupMethod.returnType)
    }

    /**
     * CRITICAL TEST: Verifies OpenSCADPathUtils uses Map for tracking active directories.
     *
     * EXPECTED TO FAIL if code is reverted: AssertionError (field will be Set, not Map)
     *
     * The bug was that activeTempDirectories was a Set<Path> which couldn't track
     * timestamps. The fix changed it to Map<Path, Long> to enable:
     * - Timestamp-based stale entry cleanup
     * - Prevention of cleanup threads deleting active directories
     * - Detection of externally deleted directories
     */
    @Test
    fun testPathUtilsHasActiveTempDirectoriesTracking() {
        val clazz = Class.forName("org.openscad.util.OpenSCADPathUtils")

        // Verify activeTempDirectories field exists
        val field = clazz.getDeclaredField("activeTempDirectories")
        assertNotNull("OpenSCADPathUtils should have activeTempDirectories field", field)

        // CRITICAL: Verify it's a Map (the fix)
        field.isAccessible = true
        val fieldType = field.type
        assertTrue(
            "CRITICAL FIX: activeTempDirectories MUST be a Map<Path, Long> (was Set<Path> in buggy version). " +
            "Found type: ${fieldType.name}",
            java.util.Map::class.java.isAssignableFrom(fieldType)
        )

        // Additional verification: ensure it's NOT a Set (the bug)
        assertFalse(
            "activeTempDirectories must NOT be a Set (that was the bug causing race condition)",
            java.util.Set::class.java.isAssignableFrom(fieldType)
        )
    }

    /**
     * BEHAVIORAL TEST: Verifies that old untracked directories get cleaned up
     * while new tracked directories are preserved.
     *
     * This tests the actual behavior of the fix: cleanup threads should only
     * delete directories that are NOT in the active tracking map.
     */
    @Test
    fun testOldTempDirectoriesAreCleanedUpButNewOnesPreserved() {
        val testTempDir = Files.createTempDirectory("test-cleanup")
        try {
            // Create an old directory with the same prefix (not via OpenSCADPathUtils, so it's NOT tracked)
            val oldDir = Files.createTempDirectory(testTempDir, "cleanup-test")
            assertTrue("Old dir should exist initially", Files.exists(oldDir))

            // Create a new directory via OpenSCADPathUtils with the SAME prefix
            // (this WILL be tracked as active in the Map)
            val newDir = OpenSCADPathUtils.createTempDirectory("cleanup-test", testTempDir.toString())
            assertTrue("New dir should exist", Files.exists(newDir))

            // Wait for cleanup thread to run
            Thread.sleep(300)

            // Old directory should be deleted (same prefix, but not in tracking map)
            assertFalse("Old untracked directory should be cleaned up", Files.exists(oldDir))

            // New directory should still exist (it's in the active tracking map)
            assertTrue("New tracked directory must NOT be deleted (the fix prevents this)", Files.exists(newDir))
        } finally {
            testTempDir.toFile().deleteRecursively()
        }
    }
}
