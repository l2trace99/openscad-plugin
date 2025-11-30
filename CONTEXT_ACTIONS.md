# Context Actions for Documentation Display

## Overview

Added context actions that allow users to view OpenSCAD documentation directly from the editor.

## Features Added

### 1. **Intention Action** (Alt+Enter)
**Class**: `ShowDocumentationIntention`

When the cursor is on an OpenSCAD function or module name, press **Alt+Enter** (or **Option+Enter** on Mac) to see the intention action:
- **"Show OpenSCAD documentation"**

This will display documentation for:
- Built-in functions (cube, sphere, translate, etc.)
- Built-in modules
- User-defined modules and functions in the current file

### 2. **Context Menu Action** (Right-Click)
**Class**: `ShowDocumentationAction`

Right-click on any OpenSCAD function or module name to see:
- **"Show OpenSCAD Documentation"** in the context menu

Or use the keyboard shortcut: **Ctrl+Shift+D** (Windows/Linux) or **Cmd+Shift+D** (Mac)

## How It Works

### For Built-in Symbols
The action displays comprehensive documentation including:
- Description of the function/module
- Parameter list with descriptions
- Usage examples
- Code snippets

Example built-in symbols:
- **3D Primitives**: cube, sphere, cylinder, polyhedron, text
- **2D Primitives**: circle, square, polygon
- **Transformations**: translate, rotate, scale, mirror, color
- **Boolean Operations**: union, difference, intersection, hull, minkowski
- **Functions**: cos, sin, tan, abs, sqrt, min, max, len, concat, etc.

### For User-Defined Symbols
The action displays:
- Module/function name
- Parameter list
- Location in file

## Usage Examples

### Example 1: Built-in Function
```openscad
cube(10);  // Place cursor on "cube" and press Alt+Enter
```

Shows documentation:
```
cube - 3D Primitive
Creates a cube in the first octant.

Parameters:
• size - single value or [x,y,z] vector
• center - false (default) or true

Example: cube([10, 20, 30], center=true);
```

### Example 2: User-Defined Module
```openscad
module box(width=10, height=10, depth=10) {
    cube([width, height, depth]);
}

box(20, 30, 40);  // Place cursor on "box" and press Alt+Enter
```

Shows documentation:
```
Module: box
Parameters: (width=10, height=10, depth=10)
```

### Example 3: Transformation
```openscad
translate([10, 0, 0]) sphere(r=5);  // Cursor on "translate"
```

Shows documentation with parameter details and examples.

## Keyboard Shortcuts

| Action | Windows/Linux | Mac |
|--------|---------------|-----|
| Show Intention | Alt+Enter | Option+Enter |
| Show Documentation | Ctrl+Shift+D | Cmd+Shift+D |
| Quick Documentation | Ctrl+Q | Cmd+J |

## Implementation Details

### Files Created
1. **`ShowDocumentationIntention.kt`**
   - Intention action that appears in Alt+Enter menu
   - Checks if cursor is on a valid symbol
   - Retrieves documentation from `OpenSCADDocumentationProvider`

2. **`ShowDocumentationAction.kt`**
   - Context menu action
   - Displays documentation in a popup window
   - HTML-formatted content with scrolling
   - Resizable and movable popup

### Registration in plugin.xml
```xml
<!-- Intentions -->
<intentionAction>
    <className>org.openscad.intentions.ShowDocumentationIntention</className>
    <category>OpenSCAD</category>
</intentionAction>

<!-- Actions -->
<action id="OpenSCAD.ShowDocumentation" 
        class="org.openscad.actions.ShowDocumentationAction" 
        text="Show OpenSCAD Documentation">
    <add-to-group group-id="EditorPopupMenu" anchor="first"/>
    <keyboard-shortcut keymap="$default" first-keystroke="ctrl shift D"/>
</action>
```

## Supported Symbols

### 3D Primitives
- cube, sphere, cylinder, polyhedron, text

### 2D Primitives
- circle, square, polygon, import

### Transformations
- translate, rotate, scale, resize, mirror, multmatrix
- color, offset, hull, minkowski

### Boolean Operations
- union, difference, intersection

### Extrusions
- linear_extrude, rotate_extrude

### Other Modules
- projection, surface, render

### Control Flow
- for, if, let, intersection_for

### Mathematical Functions
- cos, sin, tan, acos, asin, atan, atan2
- abs, ceil, floor, round, sqrt, pow, exp, ln, log
- min, max, norm, cross

### List/String Functions
- len, concat, lookup, str, chr, ord, search

### System Functions
- version, version_num, parent_module, echo, assert

## Benefits

1. **Quick Reference**: No need to leave the IDE to look up documentation
2. **Context-Aware**: Shows relevant documentation based on cursor position
3. **Multiple Access Methods**: Intention, context menu, or keyboard shortcut
4. **Rich Formatting**: HTML-formatted documentation with examples
5. **User-Defined Support**: Works with your own modules and functions

## Testing

To test the new feature:

1. Open `test-examples/example.scad`
2. Place cursor on any function name (e.g., `cube`, `translate`, `sphere`)
3. Press **Alt+Enter** or **Right-click**
4. Select "Show OpenSCAD Documentation"
5. View the documentation popup

## Future Enhancements

Potential improvements:
1. **Parameter hints**: Show parameter names inline as you type
2. **Signature help**: Show function signatures while typing arguments
3. **External links**: Link to official OpenSCAD documentation
4. **Search**: Search all available functions/modules
5. **Favorites**: Quick access to frequently used functions
6. **Code snippets**: Insert common patterns from documentation

## Build Status

✅ **BUILD SUCCESSFUL**
- All features compiled successfully
- Plugin artifact updated: `build/distributions/openscad-plugin-0.1.0.zip`
- Ready for installation and testing

## Summary

The context actions for documentation display provide a seamless way to access OpenSCAD documentation without leaving the IDE. Users can quickly look up both built-in and user-defined symbols using familiar IDE shortcuts and menus.
