package org.openscad.refactoring

import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for OpenSCAD rename refactoring and name validation.
 * These tests verify that:
 * 1. Valid OpenSCAD identifiers are accepted
 * 2. Invalid identifiers (keywords, starting with digits, invalid chars) are rejected
 * 3. The NamesValidator correctly identifies keywords vs identifiers
 */
class RenameRefactoringTest {

    // ========== Name Validation Tests ==========
    
    @Test
    fun testValidIdentifier() {
        val validator = OpenSCADNamesValidator()
        assertTrue("Simple identifier should be valid", validator.isIdentifier("myModule", null))
        assertTrue("Identifier with underscore should be valid", validator.isIdentifier("my_module", null))
        assertTrue("Identifier with numbers should be valid", validator.isIdentifier("module123", null))
        assertTrue("Single letter should be valid", validator.isIdentifier("x", null))
        assertTrue("Underscore prefix should be valid", validator.isIdentifier("_private", null))
    }
    
    @Test
    fun testInvalidIdentifier() {
        val validator = OpenSCADNamesValidator()
        assertFalse("Identifier starting with digit should be invalid", validator.isIdentifier("2Dshape", null))
        assertFalse("Empty string should be invalid", validator.isIdentifier("", null))
        assertFalse("Identifier with dash should be invalid", validator.isIdentifier("my-module", null))
        assertFalse("Identifier with space should be invalid", validator.isIdentifier("my module", null))
        assertFalse("Identifier with special char should be invalid", validator.isIdentifier("my@module", null))
    }
    
    @Test
    fun testKeywordsAreNotIdentifiers() {
        val validator = OpenSCADNamesValidator()
        assertTrue("'module' is a keyword", validator.isKeyword("module", null))
        assertTrue("'function' is a keyword", validator.isKeyword("function", null))
        assertTrue("'if' is a keyword", validator.isKeyword("if", null))
        assertTrue("'else' is a keyword", validator.isKeyword("else", null))
        assertTrue("'for' is a keyword", validator.isKeyword("for", null))
        assertTrue("'let' is a keyword", validator.isKeyword("let", null))
        assertTrue("'include' is a keyword", validator.isKeyword("include", null))
        assertTrue("'use' is a keyword", validator.isKeyword("use", null))
        assertTrue("'true' is a keyword", validator.isKeyword("true", null))
        assertTrue("'false' is a keyword", validator.isKeyword("false", null))
        assertTrue("'undef' is a keyword", validator.isKeyword("undef", null))
    }
    
    @Test
    fun testNonKeywords() {
        val validator = OpenSCADNamesValidator()
        assertFalse("'cube' is not a keyword (it's a builtin module)", validator.isKeyword("cube", null))
        assertFalse("'myModule' is not a keyword", validator.isKeyword("myModule", null))
        assertFalse("'cos' is not a keyword (it's a builtin function)", validator.isKeyword("cos", null))
    }
    
    @Test
    fun testKeywordCannotBeUsedAsIdentifier() {
        val validator = OpenSCADNamesValidator()
        // Keywords should not be valid identifiers for renaming purposes
        assertFalse("Keyword 'module' should not be valid as identifier", validator.isIdentifier("module", null))
        assertFalse("Keyword 'function' should not be valid as identifier", validator.isIdentifier("function", null))
        assertFalse("Keyword 'if' should not be valid as identifier", validator.isIdentifier("if", null))
    }
    
    // ========== Rename Input Validation Tests ==========
    
    @Test
    fun testRenameInputValidatorAcceptsValidNames() {
        val validator = OpenSCADRenameInputValidator()
        assertTrue("Valid module name should be accepted", validator.isInputValid("newModuleName"))
        assertTrue("Valid function name should be accepted", validator.isInputValid("calculate_value"))
        assertTrue("Valid variable name should be accepted", validator.isInputValid("x"))
    }
    
    @Test
    fun testRenameInputValidatorRejectsInvalidNames() {
        val validator = OpenSCADRenameInputValidator()
        assertFalse("Empty name should be rejected", validator.isInputValid(""))
        assertFalse("Name starting with digit should be rejected", validator.isInputValid("123abc"))
        assertFalse("Name with invalid chars should be rejected", validator.isInputValid("my-name"))
    }
    
    @Test
    fun testRenameInputValidatorRejectsKeywords() {
        val validator = OpenSCADRenameInputValidator()
        assertFalse("Keyword 'module' should be rejected", validator.isInputValid("module"))
        assertFalse("Keyword 'function' should be rejected", validator.isInputValid("function"))
        assertFalse("Keyword 'if' should be rejected", validator.isInputValid("if"))
    }
    
    @Test
    fun testRenameInputValidatorErrorMessages() {
        val validator = OpenSCADRenameInputValidator()
        
        assertEquals(
            "Should provide error for empty name",
            "Name cannot be empty",
            validator.getErrorMessage("")
        )
        
        assertEquals(
            "Should provide error for keyword",
            "'module' is a reserved keyword in OpenSCAD",
            validator.getErrorMessage("module")
        )
        
        assertEquals(
            "Should provide error for invalid start char",
            "Identifier must start with a letter or underscore",
            validator.getErrorMessage("123abc")
        )
        
        assertEquals(
            "Should provide error for invalid chars",
            "Identifier can only contain letters, digits, and underscores",
            validator.getErrorMessage("my-name")
        )
        
        assertNull(
            "Should return null for valid name",
            validator.getErrorMessage("validName")
        )
    }
}
