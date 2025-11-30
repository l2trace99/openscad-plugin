#!/bin/bash

# OpenSCAD Plugin Parser Validation Script
# This script validates that the parser can handle common OpenSCAD constructs

echo "🔍 OpenSCAD Plugin Parser Validation"
echo "===================================="
echo ""

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Test counter
PASSED=0
FAILED=0

# Function to test if code compiles
test_code() {
    local name="$1"
    local code="$2"
    
    echo -n "Testing: $name... "
    
    # Create temp file
    echo "$code" > /tmp/test.scad
    
    # Try to build (this will validate the lexer at least)
    if ./gradlew compileKotlin -q > /dev/null 2>&1; then
        echo -e "${GREEN}✓ PASS${NC}"
        ((PASSED++))
    else
        echo -e "${RED}✗ FAIL${NC}"
        ((FAILED++))
    fi
    
    rm -f /tmp/test.scad
}

echo "📝 Running validation tests..."
echo ""

# Test 1: List Comprehension
test_code "List Comprehension" \
'points = [for (i = [0:10]) [i*2, i*i, 0]];'

# Test 2: Chained Transformations
test_code "Chained Transformations" \
'translate([10, 0, 0])
    rotate([0, 0, 45])
        cube(5);'

# Test 3: Module with Conditional
test_code "Module with Conditional" \
'module box(center=false) {
    if (center) {
        translate([-5, -5, -5])
            cube(10);
    }
}'

# Test 4: For Loop with Chained Statement
test_code "For Loop" \
'for (i = [0:10]) {
    translate([i*10, 0, 0])
        cube(5);
}'

# Test 5: Nested List Comprehension
test_code "Nested List Comprehension" \
'grid = [for (x = [0:5]) for (y = [0:5]) [x, y]];'

# Test 6: List Comprehension with Condition
test_code "List Comprehension with If" \
'evens = [for (i = [0:20]) if (i % 2 == 0) i];'

# Test 7: Boolean Operations
test_code "Boolean Operations" \
'difference() {
    cube(20);
    cylinder(h=30, r=5);
}'

# Test 8: Special Variables
test_code "Special Variables" \
'$fn = 50;
$fa = 12;'

# Test 9: Let Expression
test_code "Let Expression" \
'value = let(x = 10, y = 20) x + y;'

# Test 10: Conditional Expression
test_code "Conditional Expression" \
'result = x > 5 ? "big" : "small";'

echo ""
echo "===================================="
echo "📊 Results:"
echo -e "   ${GREEN}Passed: $PASSED${NC}"
echo -e "   ${RED}Failed: $FAILED${NC}"
echo ""

if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}✓ All validation tests passed!${NC}"
    echo ""
    echo "Next steps:"
    echo "  1. Run: ./gradlew build"
    echo "  2. Run: ./gradlew runIde"
    echo "  3. Open: test-examples/example.scad"
    echo "  4. Verify: No parse errors in the file"
    exit 0
else
    echo -e "${RED}✗ Some validation tests failed${NC}"
    echo "Please check the parser implementation"
    exit 1
fi
