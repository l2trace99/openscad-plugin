# Changelog

All notable changes to the OpenSCAD IntelliJ Plugin will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.2.1] - 2026-01-02

### Added
- **Custom Temp Directory Setting** - Configure custom directory for render temp files in Settings → Tools → OpenSCAD
- **Automatic Temp Cleanup** - Old preview temp directories are automatically deleted in background when creating new renders

### Fixed
- **Trailing Comma Support** (Issue #12) - Parser now accepts trailing commas in:
  - Vectors/lists: `[1, 2, 3,]`
  - Function/module arguments: `cube(size=10,);`
  - Function/module parameters: `function f(a, b,) = ...`
  - For loop bindings: `for(i=[0:5],) ...`
  - Let assignments: `let(a=1,) ...`
- **Windows Compatibility** - OS-independent library paths now properly support Windows:
  - `%USERPROFILE%\Documents\OpenSCAD\libraries`
  - `%PROGRAMFILES%\OpenSCAD\libraries`
  - `%PROGRAMFILES(X86)%\OpenSCAD\libraries`
- **Windows OpenSCAD Detection** - Uses `where` command instead of `which` on Windows to find OpenSCAD in PATH
- **Linux Flatpak Compatibility** - Temp directory uses `~/.cache/openscad-plugin/` on Linux instead of `/tmp/` to support sandboxed Flatpak OpenSCAD installations

### Technical
- Refactored OS-specific library paths into `OpenSCADPathUtils` utility class
- Added `isWindows()` and `isLinux()` helper functions
- Centralized temp directory creation with `createTempDirectory()` helper

## [1.2.0] - 2026-01-01

### Added
- **Deprecation Warnings** - Highlights deprecated OpenSCAD syntax with warning underlines:
  - `filename=` parameter (use `file=` instead)
  - `layername=` parameter (use `layer=` instead)
  - `triangles=` parameter in polyhedron (use `faces=` instead)
  - `.amf` file imports (use 3MF instead)
  - Identifier names starting with digits (e.g., `2D`, `3d_shape`) - deprecated in future OpenSCAD releases
- **Variable Reassignment Warnings** - Detects when global variables are assigned multiple times, showing original and overwrite locations
- **`each` Keyword Support** - Full parser support for `each` in list comprehensions
- **`assert` Statement Support** - Parse and highlight `assert()` statements
- **`echo` Statement Support** - Parse and highlight `echo()` statements  
- **Bitwise Operators** - Support for `&`, `|`, `~`, `<<`, `>>` operators
- **Hex Number Literals** - Support for `0xFF` style hexadecimal numbers
- **Member Access Operator** - Support for `.` operator (e.g., `vector.x`)
- **Comprehensive Test Suite** - 37 new unit tests including:
  - Deprecation and warning detection tests
  - Lexer tests for new token types
  - Parser regression tests with real-world OpenSCAD files
  - Lightweight parser validation

### Fixed
- **Preview Error Handling** - Preview now clears model on render failure instead of showing stale data
- **String Lexing** - Improved string literal handling with proper state machine
- **Scientific Notation** - Numbers like `1e-9` no longer incorrectly flagged as deprecated identifiers

### Technical
- Scope-aware analysis properly tracks module/function scopes to avoid false positives
- Comment-aware detection ignores patterns inside `//` and `/* */` comments
- String-aware detection ignores patterns inside string literals
- Deduplication logic shows one warning per identifier per line
- Static caching in annotators for reliable single-pass processing

## [1.1.2] - 2025-12-29

### Added
- **Go to Definition** - Cmd/Ctrl+Click on module or function names to jump to their declaration
- **GotoDeclarationHandler** - Direct navigation support for symbols in current file, imported files, and library files
- **Integration Tests** - Unit tests for completion data, identifier patterns, path calculations, and Go to Definition logic

### Fixed
- Import resolver now uses configured library paths from settings (fixes library symbol resolution)
- Reference provider improved to better detect identifier tokens

## [1.1.1] - 2025-12-29

### Added
- **Library Autocomplete** - Autocomplete suggestions for modules and functions from configured library paths
- **Auto-insert Use Statement** - Automatically adds `use <library>` when selecting a library symbol from autocomplete
- **Proactive Library Indexing** - Libraries are indexed on project startup with progress indicator
- **Re-index on Save** - Library index updates automatically when `.scad` files are saved
- **Project Directory Indexing** - Project files are included in autocomplete suggestions
- **Library Path in Autocomplete** - Shows the library file path instead of generic "library function/module"

### Changed
- Re-indexing triggered when library paths are changed in settings

### Fixed
- `ProcessCanceledException` handling during library indexing
- Autocomplete pattern matching for OpenSCAD files

## [1.1.0] - 2025-12-29

### Added
- **Debug Preview** - PNG-based preview that shows OpenSCAD debug modifiers (`#`, `%`, `!`, `*`) with proper colors
- **Orientation Cube** - Interactive 3D orientation cube in upper right corner for quick view switching (Front/Back/Left/Right/Top/Bottom)
- **Export STL Button** - Direct export from preview pane toolbar with file save dialog
- **Camera Orientation Sync** - Debug preview renders from the same camera angle as the 3D view
- **OPENSCADPATH Support** - Run/debug configurations now properly set OPENSCADPATH from library path settings
- **OS-specific Path Separator** - Library paths use correct separator (`:` on Unix, `;` on Windows)

### Changed
- **Improved 3D Rendering** - Backface culling for cleaner surface-only display
- **Camera-relative Shading** - Faces pointing toward viewer are brighter
- **Model Color** - Changed default color from yellow to light gray (`#c1c1c1`)
- **Resizable Preview** - Preview pane can now be resized by dragging the splitter

### Fixed
- Kotlin stdlib conflict warning in Gradle configuration
- Triangle edge visibility in solid render mode

## [1.0.1] - 2025-11-30

### Added
- Comprehensive plugin description with requirements and settings guide
- Buy Me a Coffee link in plugin description
- Detailed usage instructions for all features
- Troubleshooting section in documentation

### Changed
- Enhanced plugin.xml description with complete setup and configuration guide

## [1.0.0] - 2025-11-30

### Added
- GitHub CI/CD workflows for automated testing and releases
- Automated publishing to JetBrains Marketplace on tagged releases
- CHANGELOG.md for version tracking

## [0.1.0] - 2025-11-30

### Added
- **Syntax Highlighting** - Full OpenSCAD language support with color coding
- **Code Intelligence** - Autocomplete, go-to-definition, find usages
- **3D Preview** - Real-time STL preview with wireframe/solid rendering
- **Split Editor** - Code and preview side-by-side
- **Auto-render** - Automatic preview updates on save
- **Run Configurations** - Custom OpenSCAD CLI configurations
- **Animation Support** - Export animation frames using $t variable
- **Batch Export** - Export multiple files at once (STL, PNG, etc.)
- **Library Path Support** - Configure external library paths (OPENSCADPATH)
- **Error Reporting** - OpenSCAD errors shown in IDE notifications
- **File Templates** - Quick start with Empty, 3D Model, or 2D Shape templates
- Parser with robust error recovery
- Structural navigation (outline of modules, functions)
- Support for all OpenSCAD language features:
  - Module and function declarations
  - Control structures (if/else, for, intersection_for, let)
  - Boolean operations (union, difference, intersection)
  - Transformations (translate, rotate, scale, mirror)
  - 2D and 3D primitives
  - List comprehensions
  - Object modifiers (#, !, %, *)

### Fixed
- Parser support for module declarations without braces (single-statement bodies)
- Apple Silicon (ARM64) compatibility with JOGL 2.4.0
- IDE compatibility updated to support 2025.x builds (up to 253.*)
- Proper handling of relative file paths in project context
- Library path configuration for resolving include/use statements

### Technical
- Built on IntelliJ Platform 2023.2.5
- Java 17 compatibility
- Kotlin 2.0.0
- JOGL 2.4.0 for cross-platform 3D rendering
- Grammar-Kit for lexer generation
- Comprehensive test suite

[Unreleased]: https://github.com/l2trace99/openscad-plugin/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/l2trace99/openscad-plugin/releases/tag/v0.1.0
