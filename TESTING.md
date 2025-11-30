# OpenSCAD Plugin Testing Guide

## Manual Testing with example.scad

The `test-examples/example.scad` file serves as a comprehensive test suite for the parser. It contains examples of all major OpenSCAD language features.

### How to Test

1. **Build the plugin:**
   ```bash
   ./gradlew build
   ```

2. **Run the test IDE:**
   ```bash
   ./gradlew runIde
   ```

3. **Open the test file:**
   - In the test IDE, open `test-examples/example.scad`
   - The file should be recognized as OpenSCAD (check the file icon)

4. **Verify parsing:**
   - Check that there are no red error markers
   - All syntax should be highlighted correctly
   - No "unexpected token" or "missing semicolon" errors

### Features to Validate

#### ✅ Module Declarations
```openscad
module box(width=10, height=10, depth=10, center=false) {
    cube([width, height, depth]);
}
```
- Should parse without errors
- Parameters with default values should work
- Module body should be recognized

#### ✅ Function Declarations
```openscad
function add(a, b) = a + b;
```
- Should parse without errors
- Return expression should be recognized

#### ✅ Chained Transformations
```openscad
translate([10, 0, 0])
    rotate([0, 0, 45])
        cube(5);
```
- **No semicolon required** between chained statements
- Should parse without "missing semicolon" errors

#### ✅ Conditionals in Modules
```openscad
module box(center=false) {
    if (center) {
        translate([-width/2, -height/2, -depth/2])
            cube([width, height, depth]);
    } else {
        cube([width, height, depth]);
    }
}
```
- Chained statement after `translate` should work
- No semicolon needed before the cube call

#### ✅ List Comprehensions
```openscad
points = [for (i = [0:10]) [i*2, i*i, 0]];
```
- Should parse without "unexpected token" errors
- Nested brackets should be handled correctly
- `for` keyword inside brackets should be recognized

#### ✅ Nested List Comprehensions
```openscad
grid = [for (x = [0:5]) for (y = [0:5]) [x, y]];
```
- Multiple `for` clauses should work
- Should parse without errors

#### ✅ List Comprehensions with Conditions
```openscad
evens = [for (i = [0:20]) if (i % 2 == 0) i];
```
- `if` clause in comprehension should work
- Should parse without errors

#### ✅ For Loops
```openscad
for (i = [0:10]) {
    translate([i*10, 0, 0])
        cube(5);
}
```
- Chained statement in loop body should work
- No semicolon needed between `translate` and `cube`

#### ✅ Boolean Operations
```openscad
difference() {
    cube(20);
    cylinder(h=30, r=5, center=true);
}
```
- Should parse without errors
- Block syntax should work

#### ✅ Special Variables
```openscad
$fn = 50;
$fa = 12;
$fs = 2;
```
- Variables starting with `$` should be recognized
- Should be highlighted in special color

#### ✅ Built-in Functions
```openscad
sphere(12);
cube([10, 20, 30]);
translate([5, 0, 0]) cylinder(h=10, r=3);
```
- Built-in functions should be color-coded
- Should parse without errors

### Color Coding Verification

Open `example.scad` and verify these colors:

1. **Keywords** (module, function, if, else, for, let):
   - Should be in keyword color (typically bold)

2. **Built-in Primitives** (cube, sphere, cylinder):
   - Should be in static method color (distinct from keywords)

3. **Transformations** (translate, rotate, scale):
   - Should be in instance method color (different from primitives)

4. **Special Variables** ($fn, $fa, $fs):
   - Should be in global variable color (typically purple/distinct)

5. **Numbers, Strings, Comments**:
   - Should have appropriate syntax colors

### Common Issues to Check

#### ❌ Missing Semicolon Errors
If you see "Expected ';'" errors on chained statements like:
```openscad
translate([10, 0, 0])
    cube(5);  // ← Error here means parser issue
```
This indicates the chained statement parser needs fixing.

#### ❌ List Comprehension Errors
If you see "unexpected token" or "Expected ']'" on:
```openscad
points = [for (i = [0:10]) [i*2, i*i, 0]];
```
This indicates the list comprehension parser needs fixing.

#### ❌ No Syntax Highlighting
If built-in functions like `cube`, `sphere` appear in default text color:
- Check that the annotator is registered in `plugin.xml`
- Verify the file is recognized as OpenSCAD type

### Quick Documentation Test

1. Place cursor on a built-in function (e.g., `cube`)
2. Press **Ctrl+Q** (or **Cmd+J** on Mac)
3. Should see documentation popup with:
   - Function description
   - Parameters
   - Examples
   - **Clickable link** to official documentation

### Code Completion Test

1. Type `cu` in the editor
2. Press **Ctrl+Space**
3. Should see completion suggestions including:
   - `cube`
   - `cylinder`
   - Other built-ins starting with 'cu'

### Structure View Test

1. Open `example.scad`
2. Open Structure view (**Alt+7** or **Cmd+7**)
3. Should see:
   - All module declarations
   - All function declarations
   - Organized hierarchically

## Automated Testing

### Running Tests

```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests "OpenSCADParserSimpleTest"

# Run with detailed output
./gradlew test --info
```

### Test Files

- `src/test/kotlin/org/openscad/parser/OpenSCADParserTest.kt` - Parser unit tests
- `src/test/kotlin/org/openscad/parser/OpenSCADExampleFileTest.kt` - Integration tests
- `src/test/kotlin/org/openscad/parser/OpenSCADParserSimpleTest.kt` - Simple syntax tests

### Adding New Tests

To add a test for a new language feature:

1. Add example code to `test-examples/example.scad`
2. Add a test method in `OpenSCADParserTest.kt`:
   ```kotlin
   fun testMyNewFeature() {
       doCodeTest("""
           // Your OpenSCAD code here
       """.trimIndent())
   }
   ```
3. Run the test to verify it passes

## Regression Testing

Before releasing a new version:

1. ✅ Build succeeds: `./gradlew build`
2. ✅ All tests pass: `./gradlew test`
3. ✅ Test IDE runs: `./gradlew runIde`
4. ✅ `example.scad` opens without errors
5. ✅ No parse errors in example file
6. ✅ Syntax highlighting works
7. ✅ Code completion works
8. ✅ Quick documentation works (Ctrl+Q)
9. ✅ Structure view shows modules/functions
10. ✅ Go to definition works
11. ✅ Find usages works

## Performance Testing

For large files:

1. Create a large OpenSCAD file (1000+ lines)
2. Open in test IDE
3. Verify:
   - File opens quickly (< 2 seconds)
   - Syntax highlighting is responsive
   - No lag when typing
   - Code completion is fast

## Reporting Issues

If you find a parsing issue:

1. Create a minimal example that reproduces the issue
2. Add it to `test-examples/example.scad`
3. Document the expected vs. actual behavior
4. File an issue with the example code

## Test Coverage

Current test coverage includes:

- ✅ Module declarations with parameters
- ✅ Function declarations
- ✅ Chained transformations
- ✅ Conditional statements (if/else)
- ✅ For loops
- ✅ List comprehensions (simple and nested)
- ✅ List comprehensions with conditions
- ✅ Boolean operations
- ✅ Special variables ($fn, $fa, etc.)
- ✅ Vector operations
- ✅ Range expressions
- ✅ Let expressions
- ✅ Conditional expressions (ternary)
- ✅ Object modifiers (#, %, *, !)
- ✅ Include/use statements
- ✅ Echo and assert

## Future Test Additions

Features that need more testing:

- [ ] Complex nested modules
- [ ] Recursive functions
- [ ] Advanced list operations
- [ ] String manipulation
- [ ] File I/O operations
- [ ] Advanced transformations (hull, minkowski)
- [ ] 2D to 3D extrusions
- [ ] Surface operations
- [ ] Import statements with paths
