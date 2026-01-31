# OpenSCAD IntelliJ Plugin

[![CI](https://github.com/l2trace99/openscad-plugin/actions/workflows/ci.yml/badge.svg)](https://github.com/l2trace99/openscad-plugin/actions/workflows/ci.yml)
[![Code Quality](https://github.com/l2trace99/openscad-plugin/actions/workflows/code-quality.yml/badge.svg)](https://github.com/l2trace99/openscad-plugin/actions/workflows/code-quality.yml)
[![Release](https://github.com/l2trace99/openscad-plugin/actions/workflows/release.yml/badge.svg)](https://github.com/l2trace99/openscad-plugin/actions/workflows/release.yml)

A full-featured IntelliJ IDEA plugin for OpenSCAD with syntax highlighting, code intelligence, 3D preview, and more.


Support this project by buying me a coffee!

[![Buy Me a Coffee](https://www.buymeacoffee.com/assets/img/custom_images/orange_img.png)](https://www.buymeacoffee.com/l2trace99)

## Features

- ✅ **Syntax Highlighting** - Full OpenSCAD language support with trailing comma support
- ✅ **Code Intelligence** - Autocomplete, go-to-definition, find usages
- ✅ **Rename Refactoring** - Rename modules and functions with automatic reference updates (Shift+F6)
- ✅ **3D Preview** - Real-time STL preview with wireframe/solid rendering
- ✅ **Split Editor** - Code and preview side-by-side
- ✅ **auto-refresh** - Automatic preview updates on save
- ✅ **Run Configurations** - Custom OpenSCAD CLI configurations
- ✅ **Animation Support** - Export animation frames using `$t`
- ✅ **Batch Export** - Export multiple files at once
- ✅ **Library Path Support** - Configure external library paths
- ✅ **Library Indexing** - Autocomplete symbols from library files
- ✅ **Error Reporting** - OpenSCAD errors shown in IDE notifications
- ✅ **Cross-Platform** - Windows, macOS, and Linux support (including Flatpak)

## Installation

1. Download the latest release from [Releases](https://github.com/l2trace99/openscad-plugin/releases)
2. In IntelliJ IDEA: **Settings** → **Plugins** → **⚙️** → **Install Plugin from Disk**
3. Select the downloaded `.zip` file
4. Restart IDE

## Usage

### Configure OpenSCAD Path

**Settings** → **Tools** → **OpenSCAD**
- Set custom OpenSCAD executable path
- Configure library paths (one per line)
- Configure custom temp directory (see below)
- Enable auto-refresh on save
- Set rendering options

### Custom Temp Directory (Flatpak/Sandbox Support)

If you're using OpenSCAD installed via Flatpak or another sandboxed environment, OpenSCAD may not have access to the system's default temp directory (`/tmp`). This causes preview rendering to fail.

**To fix this:**
1. Go to **Settings** → **Tools** → **OpenSCAD**
2. Set **Custom Temp Directory** to a path that OpenSCAD can access, for example:
   - `~/.cache/openscad-plugin/` (recommended for Linux)
   - Any directory within your home folder

The plugin will create this directory automatically if it doesn't exist.

**Note:** On Linux, the plugin automatically uses `~/.cache/openscad-plugin/` as the default temp directory to support Flatpak installations.

### Rename Refactoring

Rename modules and functions with automatic reference updates:

1. Place cursor on a module or function name
2. Press **Shift+F6** (or right-click → **Refactor** → **Rename**)
3. Enter the new name
4. All usages are automatically updated

**Name validation:**
- Names must start with a letter or underscore
- Names can only contain letters, digits, and underscores
- OpenSCAD keywords (`module`, `function`, `if`, `for`, etc.) cannot be used as names

### Create New File

**File** → **New** → **OpenSCAD File**
- Choose from templates: Empty, 3D Model, 2D Shape

### Preview

Open any `.scad` file to see the split editor with live 3D preview.

**Preview Controls:**
- **Render** - Generate STL preview
- **Reset View** - Reset camera
- **Wireframe** - Toggle wireframe/solid view
- **Debug Preview** - Show OpenSCAD debug modifiers (see below)
- **Export STL** - Export directly to STL file
- **auto-refresh** - Enable automatic rendering on save

**3D Navigation:**
- **Left-click + drag** - Rotate model
- **Right-click + drag** - Pan view
- **Mouse wheel** - Zoom in/out
- **Orientation Cube** - Click faces (Front/Back/Left/Right/Top/Bottom) to snap to that view

### Debug Preview

The **Debug Preview** feature renders your model as OpenSCAD sees it, showing debug modifiers with their proper colors:

| Modifier | Name | Effect in Debug Preview |
|----------|------|------------------------|
| `#` | Debug | Highlighted in transparent red/pink |
| `%` | Background | Shown in transparent gray |
| `!` | Root | Only this object is rendered |
| `*` | Disable | Object is hidden |

**Usage:**
1. Add debug modifiers to your code (e.g., `#cube(10);`)
2. Click **Debug Preview** button in the toolbar
3. The preview shows a PNG rendered by OpenSCAD with debug colors
4. Click **3D View** to return to the interactive 3D model

**Note:** Debug preview preserves your current camera orientation, so you can rotate the 3D view first, then click Debug Preview to see the same angle with debug colors

### Library Indexing

The plugin automatically indexes all `.scad` files in your configured library paths, making library modules and functions available in autocomplete.

**How it works:**
1. Configure library paths in **Settings** → **Tools** → **OpenSCAD**
2. The plugin scans all `.scad` files in those directories
3. Modules and functions appear in autocomplete with "library module" or "library function" labels
4. Selecting a library symbol automatically adds the required `use <...>` statement to your file

**Indexed locations:**
- Paths configured in settings
- Project `lib/` directory (if it exists)
- Standard OpenSCAD library locations:
  - `/usr/share/openscad/libraries`
  - `/usr/local/share/openscad/libraries`
  - `~/.local/share/OpenSCAD/libraries`
  - `~/Documents/OpenSCAD/libraries`

**Autocomplete features:**
- Shows parameter hints for modules/functions
- Displays icon indicating module vs function
- Shows source file path
- Private symbols (starting with `_`) are excluded

### Export

**Right-click on `.scad` file:**
- **Export to STL...** - Single file export
- **Export to Multiple Formats...** - Export to STL + PNG

**Tools Menu:**
- **Batch Export to STL...** - Export all project files

### Run Configuration

**Run** → **Edit Configurations** → **+** → **OpenSCAD**

Configure:
- Input/output files
- Rendering options
- Animation frames
- Camera settings
- Custom parameters

## Development

### Build

```bash
./gradlew build
```

### Run in Test IDE

```bash
./gradlew runIde
```

### Run Tests

```bash
./gradlew test
```

## Grammar Reference

Your tasks:
- Implement a parser, syntax highlighter, and code intelligence features for OpenSCAD.
- Use the BNF-style grammar below as the authoritative description of the language syntax.
- You may internally translate this BNF to ANTLR, tree-sitter, or any parsing technology.

The BNF below is a *clean-room* summary derived from public documentation and existing grammars, but written from scratch. It is intended to be practically complete for typical OpenSCAD usage (v2021.01+).

Reference URLs used to derive this spec (for you to cross-check or refine, NOT to copy verbatim):
- OpenSCAD Language Reference (Wikibooks): https://en.wikibooks.org/wiki/OpenSCAD_User_Manual/The_OpenSCAD_Language
- OpenSCAD main documentation index: https://openscad.org/documentation.html
- OpenSCAD CheatSheet (syntax + builtins): https://openscad.org/cheatsheet/
- SCADFormat (OpenSCAD.g4 grammar – GPL, do NOT copy verbatim): https://github.com/hugheaves/scadformat/blob/main/OpenSCAD.g4
- tree-sitter-openscad grammar (MIT, for cross-checking): https://github.com/openscad/tree-sitter-openscad


```
======================================================================
LEXICAL CONVENTIONS (informal)
======================================================================

Terminals (tokens) referenced by the grammar:

IDENT           ::= letter (letter | digit | "_")*
NUMBER          ::= integer | real-number (supports decimal and exponent notation)
STRING          ::= " ... "   (C-style string with escapes; single-line)
BOOL_LITERAL    ::= "true" | "false"
UNDEF_LITERAL   ::= "undef"
PATH_LITERAL    ::= STRING    (used in include/use/import/surface; model as STRING)

Operators / punctuation:
    "+" "-" "*" "/" "%" "^"
    "==" "!=" "<" "<=" ">" ">="
    "&&" "||" "!"
    "?" ":"
    "="
    "[" "]" "{" "}" "(" ")" "<" ">" "," ";"
    ":" ".."            (":" used for ranges and ternary)
    "$"                 (for special variables like $fn)

Object modifiers (prefix before objects or blocks):
    "#" "!" "%" "*"

Keywords:
    module function
    include use
    if else
    for intersection_for
    let
    true false
    undef

Comments:
    Line comment:          "//" until end-of-line
    Block comment:         "/*" ... "*/"
Comments are ignored by the grammar.

Whitespace (spaces, tabs, newlines) separates tokens and is otherwise ignored.


======================================================================
TOP-LEVEL STRUCTURE
======================================================================

<program> ::= { <statement> }

<statement> ::=
      <empty_statement>
    | <assignment_statement> ";"
    | <module_declaration>
    | <function_declaration>
    | <module_instantiation_statement>
    | <control_statement>
    | <include_statement> ";"
    | <use_statement> ";"

<empty_statement> ::= ";"

----------------------------------------------------------------------
Include / use
----------------------------------------------------------------------

<include_statement> ::= "include" "<" PATH_LITERAL ">"
<use_statement>     ::= "use"     "<" PATH_LITERAL ">"

(Note: Model PATH_LITERAL as STRING token in the lexer.)


======================================================================
DECLARATIONS
======================================================================

----------------------------------------------------------------------
Modules
----------------------------------------------------------------------

<module_declaration> ::=
    "module" IDENT "(" [ <parameter_list> ] ")" <block>

<parameter_list> ::= <parameter> { "," <parameter> }

<parameter> ::=
      IDENT                             -- positional parameter
    | IDENT "=" <expression>            -- parameter with default

<block> ::= "{" { <statement> } "}"

----------------------------------------------------------------------
Functions
----------------------------------------------------------------------

<function_declaration> ::=
    "function" IDENT "(" [ <parameter_list> ] ")" "=" <expression> ";"


======================================================================
ASSIGNMENTS AND VARIABLES
======================================================================

<assignment_statement> ::= <lvalue> "=" <expression>

<lvalue> ::= IDENT
    -- (OpenSCAD does not support assigning directly into vector elements)


======================================================================
MODULE INSTANTIATION & OBJECT MODIFIERS
======================================================================

<module_instantiation_statement> ::=
      <object_modifier_opt> <module_call> ";"
    | <object_modifier_opt> <module_call> <block>
    | <object_modifier_opt> <block>              -- e.g. "# { cube(1); }"

<object_modifier_opt> ::= [ <object_modifier> ]

<object_modifier> ::= "#" | "!" | "%" | "*"

<module_call> ::=
    IDENT "(" [ <argument_list> ] ")"

<argument_list> ::= <argument> { "," <argument> }

<argument> ::=
      <expression>                 -- positional argument
    | IDENT "=" <expression>       -- named argument


======================================================================
CONTROL STATEMENTS
======================================================================

<control_statement> ::=
      <if_statement>
    | <for_statement>
    | <intersection_for_statement>
    | <let_statement>

----------------------------------------------------------------------
If / else
----------------------------------------------------------------------

<if_statement> ::=
    "if" "(" <expression> ")" <block_or_single_statement> [ "else" <block_or_single_statement> ]

<block_or_single_statement> ::=
      <block>
    | <single_embedded_statement>

<single_embedded_statement> ::=
      <module_instantiation_statement>
    | <assignment_statement> ";"
    | <control_statement>
    | <include_statement> ";"
    | <use_statement> ";"

----------------------------------------------------------------------
For / intersection_for
----------------------------------------------------------------------

<for_statement> ::=
    "for" "(" <for_binding_list> ")" <block_or_single_statement>

<intersection_for_statement> ::=
    "intersection_for" "(" <for_binding_list> ")" <block_or_single_statement>

<for_binding_list> ::= <for_binding> { "," <for_binding> }

<for_binding> ::= IDENT "=" <for_iterator_expression>

<for_iterator_expression> ::=
      <expression>                      -- simple value or vector
    | "[" <expression> ":" <expression> [ ":" <expression> ] "]"
        -- numeric range [start : step : end] (step optional)

----------------------------------------------------------------------
let(...) as a special expression or statement-like construct
----------------------------------------------------------------------

<let_statement> ::=
    "let" "(" <assignment_list> ")" <block_or_single_statement>

<assignment_list> ::= <assignment_in_let> { "," <assignment_in_let> }

<assignment_in_let> ::= IDENT "=" <expression>


======================================================================
EXPRESSIONS (with precedence)
======================================================================

We define expressions with standard precedence from lowest (top) to highest (bottom):

1. conditional ( ?: )
2. logical OR (||)
3. logical AND (&&)
4. equality / relational (==, !=, <, <=, >, >=)
5. additive (+, -)
6. multiplicative (*, /, %)
7. unary (+, -, !, object-modifier as unary is *not* allowed in expressions)
8. postfix (function calls, indexing)
9. primary (literals, identifiers, vector literals, ranges, let-expressions)

----------------------------------------------------------------------
Expression root
----------------------------------------------------------------------

<expression> ::= <conditional_expression>

<expression_list> ::= <expression> { "," <expression> }

----------------------------------------------------------------------
Conditional operator
----------------------------------------------------------------------

<conditional_expression> ::=
      <logical_or_expression>
    | <logical_or_expression> "?" <expression> ":" <expression>

----------------------------------------------------------------------
Logical OR / AND
----------------------------------------------------------------------

<logical_or_expression> ::=
      <logical_and_expression>
    | <logical_or_expression> "||" <logical_and_expression>

<logical_and_expression> ::=
      <equality_expression>
    | <logical_and_expression> "&&" <equality_expression>

----------------------------------------------------------------------
Equality / relational
----------------------------------------------------------------------

<equality_expression> ::=
      <relational_expression>
    | <equality_expression> "==" <relational_expression>
    | <equality_expression> "!=" <relational_expression>

<relational_expression> ::=
      <additive_expression>
    | <relational_expression> "<"  <additive_expression>
    | <relational_expression> "<=" <additive_expression>
    | <relational_expression> ">"  <additive_expression>
    | <relational_expression> ">=" <additive_expression>

----------------------------------------------------------------------
Additive / multiplicative
----------------------------------------------------------------------

<additive_expression> ::=
      <multiplicative_expression>
    | <additive_expression> "+" <multiplicative_expression>
    | <additive_expression> "-" <multiplicative_expression>

<multiplicative_expression> ::=
      <unary_expression>
    | <multiplicative_expression> "*" <unary_expression>
    | <multiplicative_expression> "/" <unary_expression>
    | <multiplicative_expression> "%" <unary_expression>

----------------------------------------------------------------------
Unary
----------------------------------------------------------------------

<unary_expression> ::=
      <postfix_expression>
    | "+" <unary_expression>
    | "-" <unary_expression>
    | "!" <unary_expression>

----------------------------------------------------------------------
Postfix: function calls and indexing
----------------------------------------------------------------------

<postfix_expression> ::=
      <primary_expression>
    | <postfix_expression> "(" [ <argument_list> ] ")"     -- function call
    | <postfix_expression> "[" <expression> "]"            -- single index
    | <postfix_expression> "[" <expression> ":" <expression> "]"   -- 2-arg slice
    | <postfix_expression> "[" <expression> ":" <expression> ":" <expression> "]"
        -- 3-arg slice (start:step:end)

----------------------------------------------------------------------
Primary expressions
----------------------------------------------------------------------

<primary_expression> ::=
      <literal>
    | IDENT
    | "(" <expression> ")"
    | <vector_literal>
    | <range_literal>
    | <let_expression>

<literal> ::=
      NUMBER
    | STRING
    | BOOL_LITERAL
    | UNDEF_LITERAL

----------------------------------------------------------------------
let(...) as an expression
----------------------------------------------------------------------

<let_expression> ::=
    "let" "(" <assignment_list> ")" <expression>

(Same <assignment_list> rule as above.)

----------------------------------------------------------------------
Vector literals, ranges, list comprehensions
----------------------------------------------------------------------

<vector_literal> ::=
      "[" "]"
    | "[" <expression_list> [ "," ] "]"
    | "[" <comprehension> "]"

<comprehension> ::=
    <expression> <comprehension_clause> { <comprehension_clause> }

<comprehension_clause> ::=
      "for" IDENT "=" <for_iterator_expression>
    | "if"  <expression>

<range_literal> ::=
    "[" <expression> ":" <expression> [ ":" <expression> ] "]"

(Note: In practice, the same concrete syntax is used for numeric ranges and vector
literals; the parser distinguishes them in the semantic phase.)


======================================================================
GEOMETRIC PRIMITIVES & BUILT-IN MODULES/FUNCTIONS (syntactic view)
======================================================================

From the parser's perspective, *all* built-in modules and functions are syntactically
just IDENT tokens followed by "( ... )". Their specific names (cube, sphere, translate,
color, union, difference, intersection, hull, minkowski, linear_extrude, rotate_extrude,
text, import, surface, projection, etc.) are not special in the grammar and should be
handled in a later semantic phase.

Examples (all are just <module_call> or <postfix_expression>):
    cube(size = 10);
    sphere(r = 5);
    translate([0,0,10]) cube(5);
    union() { ... }
    difference() { ... }
    intersection() { ... }
    hull() { ... }
    minkowski() { ... }
    linear_extrude(height=10) polygon(...);
    rotate_extrude(angle=360) ...;
    color("red") cube(1);
    text("Hello", size=10);
    import("model.stl");
    surface("heightmap.dat");


======================================================================
SPECIAL VARIABLES (syntactic view)
======================================================================

Special variables like $fn, $fa, $fs, $t, $vpr, $vpt, $vpd, $children, $preview are
lexically IDENT tokens starting with '$'. They are syntactically identical to regular
IDENT in this grammar:

    <primary_expression> includes IDENT, which covers $fn, $t, etc.

Semantic restrictions (e.g., they are read-only) are enforced later.


======================================================================
NOTES FOR IMPLEMENTATION
======================================================================

- The BNF above is intended to be deterministic and suitable for LL(*) / LR(*) parsing.
- You are allowed to refactor production names and split rules when converting to ANTLR or tree-sitter, as long as the accepted language stays equivalent.
- Pay special attention to:
    * The dual use of "[" ... "]" for both vectors and ranges.
    * let(...) which appears both as a statement-like construct and as an expression.
    * Optional block vs. single-statement forms after if/for/intersection_for/let.
    * Prefix object modifiers (#, !, %, *) which may apply to single objects or whole blocks.
```
