# Changelog

All notable changes to the OpenSCAD IntelliJ Plugin will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
