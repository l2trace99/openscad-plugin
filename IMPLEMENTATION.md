# OpenSCAD IntelliJ Plugin - Implementation Summary

## Overview
This is a full-featured JetBrains IDE plugin for OpenSCAD that provides comprehensive language support including syntax highlighting, code completion, navigation, and more.

## Architecture

### 1. Lexer (Tokenization)
- **File**: `src/main/grammars/OpenSCADLexer.flex`
- **Implementation**: JFlex-based lexer that tokenizes OpenSCAD source code
- **Features**:
  - Recognizes all OpenSCAD keywords (module, function, if, for, let, etc.)
  - Handles special variables starting with `$` (e.g., `$fn`, `$fa`, `$fs`)
  - Supports line comments (`//`) and block comments (`/* */`)
  - Tokenizes numbers, strings, identifiers, and operators
  - Proper operator precedence (multi-char operators before single-char)

### 2. Parser (Syntax Analysis)
- **File**: `src/main/kotlin/org/openscad/parser/OpenSCADParserImpl.kt`
- **Implementation**: Recursive descent parser following the BNF grammar
- **Features**:
  - Complete implementation of OpenSCAD grammar
  - Proper expression precedence (conditional → logical OR → logical AND → equality → relational → additive → multiplicative → unary → postfix → primary)
  - Support for all statement types:
    - Module and function declarations
    - Control flow (if/else, for, intersection_for, let)
    - Module instantiation with object modifiers (#, !, %, *)
    - Include/use statements
  - Vector literals and list comprehensions
  - Range expressions
  - Let expressions (both statement and expression forms)
  - Error recovery for robust parsing

### 3. PSI (Program Structure Interface)
- **Files**: `src/main/kotlin/org/openscad/psi/`
- **Key Classes**:
  - `OpenSCADTypes`: Token and element type definitions
  - `OpenSCADPsiElement`: Base PSI element class
  - `OpenSCADNamedElement`: Base for named elements (modules, functions)
  - `OpenSCADModuleDeclaration`: Module declaration PSI element
  - `OpenSCADFunctionDeclaration`: Function declaration PSI element

### 4. Syntax Highlighting
- **File**: `src/main/kotlin/org/openscad/highlighting/OpenSCADSyntaxHighlighter.kt`
- **Features**:
  - Keywords highlighted
  - Numbers, strings, and literals
  - Comments (line and block)
  - Operators and punctuation
  - Special variables

### 5. Code Completion
- **File**: `src/main/kotlin/org/openscad/completion/OpenSCADCompletionContributor.kt`
- **Features**:
  - All OpenSCAD keywords
  - Built-in 3D primitives (cube, sphere, cylinder, polyhedron, text)
  - Built-in 2D primitives (circle, square, polygon, import)
  - Transformations (translate, rotate, scale, mirror, color, etc.)
  - Boolean operations (union, difference, intersection, hull, minkowski)
  - Special variables ($fn, $fa, $fs, $t, $children, etc.)
  - Built-in functions (trigonometry, math, string, list operations)
  - Auto-insertion of parentheses with parameter hints

### 6. Structure View
- **Files**: `src/main/kotlin/org/openscad/structure/`
- **Features**:
  - Hierarchical view of modules and functions
  - Quick navigation to declarations
  - Alphabetical sorting support

### 7. Reference Resolution & Find Usages
- **Files**: 
  - `src/main/kotlin/org/openscad/references/OpenSCADReference.kt`
  - `src/main/kotlin/org/openscad/findusages/OpenSCADFindUsagesProvider.kt`
- **Features**:
  - Go to definition for modules and functions
  - Find all usages of declarations
  - Reference highlighting

### 8. Documentation Provider
- **File**: `src/main/kotlin/org/openscad/documentation/OpenSCADDocumentationProvider.kt`
- **Features**:
  - Quick documentation (Ctrl+Q / Cmd+J)
  - Built-in documentation for primitives and transformations
  - Parameter information
  - Usage examples

### 9. Editor Features
- **Brace Matching**: `src/main/kotlin/org/openscad/editor/OpenSCADBraceMatcher.kt`
  - Highlights matching (), {}, []
- **Code Folding**: `src/main/kotlin/org/openscad/editor/OpenSCADFoldingBuilder.kt`
  - Fold module/function bodies
  - Fold block comments
- **Commenter**: `src/main/kotlin/org/openscad/commenter/OpenSCADCommenter.kt`
  - Line comment with //
  - Block comment with /* */

## BNF Grammar Coverage

The parser implements the complete BNF grammar specification:

✅ **Lexical Conventions**
- Identifiers (including special variables with $)
- Numbers (integer and floating-point with exponent notation)
- Strings (with escape sequences)
- Boolean literals (true/false)
- Undef literal
- All operators and punctuation
- Object modifiers (#, !, %, *)
- Comments (line and block)

✅ **Top-Level Structure**
- Program as sequence of statements
- All statement types

✅ **Declarations**
- Module declarations with parameters and blocks
- Function declarations with parameters and expressions
- Parameter lists with optional default values

✅ **Assignments and Variables**
- Assignment statements
- Variable references

✅ **Module Instantiation**
- Module calls with arguments
- Object modifiers
- Optional blocks after module calls
- Standalone blocks with modifiers

✅ **Control Statements**
- if/else statements
- for loops with multiple bindings
- intersection_for loops
- let statements (both statement and expression forms)
- Block vs. single statement forms

✅ **Expressions (Full Precedence)**
1. Conditional (ternary ? :)
2. Logical OR (||)
3. Logical AND (&&)
4. Equality (==, !=)
5. Relational (<, <=, >, >=)
6. Additive (+, -)
7. Multiplicative (*, /, %)
8. Unary (+, -, !)
9. Postfix (function calls, indexing, slicing)
10. Primary (literals, identifiers, parenthesized, vectors, ranges, let)

✅ **Vector Literals and Comprehensions**
- Empty vectors []
- Expression lists
- List comprehensions with for and if clauses
- Range literals [start:end] and [start:step:end]

✅ **Include/Use Statements**
- include <"file.scad">
- use <"file.scad">

## Build System

### Gradle Configuration
- **Plugin**: Grammar-Kit for JFlex lexer generation
- **Build Process**:
  1. JFlex generates `OpenSCADLexerImpl` from `OpenSCADLexer.flex`
  2. Generated code placed in `src/main/gen/`
  3. Kotlin compilation includes generated sources
  4. Plugin packaged with IntelliJ Platform SDK

### Build Commands
```bash
# Generate lexer and build plugin
./gradlew build

# Run plugin in test IDE
./gradlew runIde

# Build plugin distribution
./gradlew buildPlugin
```

## Testing

To test the plugin:
1. Run `./gradlew runIde` to launch a test IDE instance
2. Create a new file with `.scad` extension
3. Test features:
   - Syntax highlighting
   - Code completion (Ctrl+Space)
   - Structure view (Alt+7 / Cmd+7)
   - Quick documentation (Ctrl+Q / Cmd+J)
   - Go to definition (Ctrl+B / Cmd+B)
   - Find usages (Alt+F7)
   - Comment/uncomment (Ctrl+/ / Cmd+/)
   - Code folding

## Known Limitations

1. **Semantic Analysis**: Currently limited to file-level scope. Cross-file references (include/use) not fully implemented.
2. **Type Inference**: No type checking or inference (OpenSCAD is dynamically typed).
3. **Built-in Modules**: Treated as regular identifiers; no special validation.
4. **Advanced Features**: No refactoring support, no code inspections/intentions yet.

## Future Enhancements

1. **Cross-file References**: Resolve include/use statements
2. **Code Inspections**: Detect undefined variables, unused declarations
3. **Refactoring**: Rename, extract module/function
4. **Live Preview**: Integration with OpenSCAD renderer
5. **Debugging Support**: If OpenSCAD adds debugging capabilities
6. **Parameter Hints**: Inline parameter name hints
7. **Color Preview**: Show color swatches for color() calls
8. **Import Validation**: Check that imported files exist

## File Structure

```
openscad-plugin/
├── build.gradle.kts                    # Build configuration
├── settings.gradle.kts                 # Gradle settings
├── src/
│   ├── main/
│   │   ├── grammars/
│   │   │   └── OpenSCADLexer.flex     # JFlex lexer specification
│   │   ├── kotlin/org/openscad/
│   │   │   ├── OpenSCADLanguage.kt    # Language definition
│   │   │   ├── commenter/             # Comment support
│   │   │   ├── completion/            # Code completion
│   │   │   ├── documentation/         # Quick docs
│   │   │   ├── editor/                # Editor features
│   │   │   ├── file/                  # File type
│   │   │   ├── findusages/            # Find usages
│   │   │   ├── highlighting/          # Syntax highlighting
│   │   │   ├── lexer/                 # Lexer adapter
│   │   │   ├── parser/                # Parser implementation
│   │   │   ├── psi/                   # PSI elements
│   │   │   ├── references/            # Reference resolution
│   │   │   └── structure/             # Structure view
│   │   └── resources/
│   │       ├── META-INF/
│   │       │   └── plugin.xml         # Plugin descriptor
│   │       └── icons/
│   │           └── openscad.svg       # File icon
│   └── main/gen/                      # Generated sources (JFlex)
└── README.md                           # User documentation
```

## Contributing

When extending this plugin:
1. Follow the existing code structure
2. Update the BNF grammar documentation if adding syntax
3. Add corresponding PSI elements for new constructs
4. Update completion contributors for new keywords/functions
5. Test thoroughly with real OpenSCAD files
