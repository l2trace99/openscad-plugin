package org.openscad.lexer

import com.intellij.psi.TokenType
import org.junit.Test

/**
 * Regression tests for parser fixes.
 * Each test verifies that specific OpenSCAD syntax is correctly lexed without BAD_CHARACTER tokens.
 */
class ParserRegressionTest {

    private fun assertNoLexerErrors(code: String, description: String) {
        val lexer = OpenSCADLexerImpl()
        lexer.reset(code, 0, code.length, 0)
        
        val badChars = mutableListOf<String>()
        var tokenType = lexer.advance()
        var position = 0
        
        while (tokenType != null) {
            val tokenText = lexer.yytext().toString()
            if (tokenType == TokenType.BAD_CHARACTER) {
                badChars.add("Position $position: '$tokenText' (code: ${tokenText.firstOrNull()?.code})")
            }
            position += tokenText.length
            tokenType = lexer.advance()
        }
        
        if (badChars.isNotEmpty()) {
            throw AssertionError("$description\nFound ${badChars.size} BAD_CHARACTER tokens:\n${badChars.joinToString("\n")}\nCode: $code")
        }
    }

    // ========== Power Operator ^ ==========
    
    @Test
    fun testPowerOperator() {
        assertNoLexerErrors(
            "x = 2^3;",
            "Basic power operator"
        )
    }
    
    @Test
    fun testPowerOperatorInExpression() {
        assertNoLexerErrors(
            "nozArea = (nozzle/2)^2*PI;",
            "Power operator with parentheses and multiplication"
        )
    }
    
    @Test
    fun testPowerOperatorComplex() {
        assertNoLexerErrors(
            "lineProfile = PI*(layer/2)^2 + (line-layer)*layer;",
            "Complex expression with power operator"
        )
    }

    // ========== Parameter Names Starting with Digits (e.g., 2D=value) ==========
    
    @Test
    fun testParameterNameStartingWithDigit() {
        assertNoLexerErrors(
            "SBogen(2D=thickness);",
            "Parameter name starting with digit (2D)"
        )
    }
    
    @Test
    fun testMultipleParametersWithDigitNames() {
        assertNoLexerErrors(
            """SBogen(
                2D=thickness,
                dist=separation,
                grad=bendAngle
            );""",
            "Multiple parameters including one starting with digit"
        )
    }

    // ========== Echo/Assert as Expression Prefixes ==========
    
    @Test
    fun testEchoAsExpressionPrefix() {
        assertNoLexerErrors(
            "r = echo(\"debug\") 42;",
            "echo() as expression prefix"
        )
    }
    
    @Test
    fun testEchoInTernary() {
        assertNoLexerErrors(
            "function f(name) = is_undef(name) ? r : echo(str(name)) r;",
            "echo() in ternary false branch as expression prefix"
        )
    }
    
    @Test
    fun testAssertAsExpressionPrefix() {
        assertNoLexerErrors(
            "function f(a) = assert(a > 0) a * 2;",
            "assert() as expression prefix in function"
        )
    }
    
    @Test
    fun testChainedAssertEcho() {
        assertNoLexerErrors(
            "function f(a, b) = assert(a < 0) assert(b > 0) let(c = a + b) a * b;",
            "Chained assert expressions"
        )
    }

    // ========== Function Literals (Anonymous Functions) ==========
    
    @Test
    fun testFunctionLiteral() {
        assertNoLexerErrors(
            "f = function(x) x * 2;",
            "Basic function literal"
        )
    }
    
    @Test
    fun testFunctionLiteralWithDefaultParam() {
        assertNoLexerErrors(
            "naca_func = function(x, t=0.12) 5*t*sqrt(x);",
            "Function literal with default parameter"
        )
    }
    
    @Test
    fun testFunctionLiteralInLet() {
        assertNoLexerErrors(
            """function naca(l=10) = let(
                naca_func = function(x, t=t) 5*t*sqrt(x)
            ) naca_func(0.5);""",
            "Function literal inside let expression"
        )
    }
    
    @Test
    fun testFunctionLiteralComplex() {
        assertNoLexerErrors(
            "naca_t = function(t=t, fn=fn/2, l=l, y=dir?1:-1) [for(n=range) n*l];",
            "Complex function literal with multiple params and list comprehension"
        )
    }

    // ========== Generator Expressions in Vectors ==========
    
    @Test
    fun testIfGeneratorInVector() {
        assertNoLexerErrors(
            "[if(x > 0) x, if(y > 0) y]",
            "If generator expressions in vector"
        )
    }
    
    @Test
    fun testEachWithIfGenerators() {
        assertNoLexerErrors(
            """[for(i=[0:10]) each [
                if(i <= 5) i,
                if(i > 5) i * 2
            ]]""",
            "each with if generator expressions"
        )
    }
    
    @Test
    fun testNestedGenerators() {
        assertNoLexerErrors(
            """[for(f=[0:fn]) let(i=f*360/fn) each [
                if(i<=90) [sin(i), cos(i)],
                if(i>90 && i<=180) [sin(i), -cos(i)],
                if(i>180) [-sin(i), -cos(i)]
            ]]""",
            "Complex nested generators with each and if"
        )
    }
    
    @Test
    fun testIfForChainedGenerators() {
        assertNoLexerErrors(
            """[
                if(cond) for(i=[0:n]) [i, i*2],
                if(other) for(j=[0:m]) let(x=j*2) [x, x]
            ]""",
            "Chained if-for-let generator expressions"
        )
    }
    
    @Test
    fun testForWithoutVariable() {
        assertNoLexerErrors(
            "[ for([0:5]) 1 ]",
            "for generator with range only (no variable)"
        )
    }
    
    @Test
    fun testComplexChainedGenerators() {
        assertNoLexerErrors(
            """[
                if(grad==0 && minF) for([0:minF]) [sin(rot)*r, cos(rot)*r, z],
                if(grad) for(i=[0:ifn-1]) let(iw=i%ifn) [sin(iw*step)*r, cos(iw*step)*r, z]
            ]""",
            "Complex chained generators from real OpenSCAD code"
        )
    }

    // ========== Hexadecimal Numbers ==========
    
    @Test
    fun testHexadecimalNumber() {
        assertNoLexerErrors(
            "x = 0xFF;",
            "Hexadecimal number"
        )
    }
    
    @Test
    fun testHexadecimalInExpression() {
        assertNoLexerErrors(
            "color = [0x1F/255, 0xAB/255, 0xCD/255];",
            "Hexadecimal numbers in expression"
        )
    }

    // ========== Each Keyword ==========
    
    @Test
    fun testEachInListComprehension() {
        assertNoLexerErrors(
            "flat = [for(v=nested) each v];",
            "each in list comprehension to flatten"
        )
    }
    
    @Test
    fun testEachWithExpression() {
        assertNoLexerErrors(
            "[each [1,2,3], each [4,5,6]]",
            "each with vector expressions"
        )
    }

    // ========== Assert Statement ==========
    
    @Test
    fun testAssertStatement() {
        assertNoLexerErrors(
            "assert(x > 0);",
            "Basic assert statement"
        )
    }
    
    @Test
    fun testAssertWithMessage() {
        assertNoLexerErrors(
            "assert(x > 0, \"x must be positive\");",
            "Assert statement with message"
        )
    }
    
    @Test
    fun testAssertBeforeStatement() {
        assertNoLexerErrors(
            "assert(len(points) >= 3) polygon(points);",
            "Assert followed by statement"
        )
    }
    
    @Test
    fun testAssertInFunctionExpression() {
        assertNoLexerErrors(
            "function scaleGrad(grad=45,h=1,r=1)=assert(grad!=0)max(0,(r-(h/tan(grad)))/r);",
            "Assert as expression prefix in function body"
        )
    }

    // ========== Dot Notation (Member Access) ==========
    
    @Test
    fun testDotNotation() {
        assertNoLexerErrors(
            "x = point.x; y = point.y; z = point.z;",
            "Dot notation for vector element access"
        )
    }
    
    @Test
    fun testDotNotationInExpression() {
        assertNoLexerErrors(
            "dist = sqrt(p.x^2 + p.y^2 + p.z^2);",
            "Dot notation in complex expression"
        )
    }

    // ========== Bitwise Operators ==========
    
    @Test
    fun testBitwiseOr() {
        assertNoLexerErrors(
            "x = a | b;",
            "Bitwise OR operator"
        )
    }
    
    @Test
    fun testBitwiseAnd() {
        assertNoLexerErrors(
            "x = a & b;",
            "Bitwise AND operator"
        )
    }
    
    @Test
    fun testBitwiseNot() {
        assertNoLexerErrors(
            "x = ~a;",
            "Bitwise NOT operator"
        )
    }
    
    @Test
    fun testShiftLeft() {
        assertNoLexerErrors(
            "x = a << 2;",
            "Left shift operator"
        )
    }
    
    @Test
    fun testShiftRight() {
        assertNoLexerErrors(
            "x = a >> 2;",
            "Right shift operator"
        )
    }
    
    @Test
    fun testBitwiseOperatorsCombined() {
        assertNoLexerErrors(
            "x = (a & 0xFF) | (b << 8);",
            "Combined bitwise operators"
        )
    }
    
    // ========== Echo as Keyword ==========
    
    @Test
    fun testEchoKeyword() {
        assertNoLexerErrors(
            "echo(\"message\");",
            "Echo as keyword"
        )
    }
    
    @Test
    fun testEchoAsExpressionPrefixKeyword() {
        assertNoLexerErrors(
            "r = echo(\"debug\") 42;",
            "Echo keyword as expression prefix"
        )
    }

    // ========== Combined Complex Cases ==========
    
    @Test
    fun testComplexFunctionWithAllFeatures() {
        assertNoLexerErrors(
            """function complex(n=0x10, scale=1.0) = 
                let(
                    f = function(x) x^2,
                    result = [for(i=[0:n]) each [
                        if(i % 2 == 0) f(i) * scale,
                        if(i % 2 != 0) echo(i) f(i)
                    ]]
                )
                assert(len(result) > 0, "empty result")
                result;""",
            "Complex function with hex, function literal, generators, echo prefix, assert"
        )
    }
    
    @Test
    fun testComplexBitwiseExpression() {
        assertNoLexerErrors(
            """function mask(value, bits=8) = 
                let(m = (1 << bits) - 1)
                value & m | (~value & m) >> 1;""",
            "Complex function with bitwise operations"
        )
    }

    // ========== Modifiers Before Statements ==========
    
    @Test
    fun testModifierBeforeIf() {
        assertNoParserErrors(
            "%if(debug) cube(10);",
            "% modifier before if statement"
        )
    }
    
    @Test
    fun testModifierBeforeFor() {
        assertNoParserErrors(
            "#for(i=[0:10]) translate([i,0,0]) cube(1);",
            "# modifier before for statement"
        )
    }
    
    @Test
    fun testModifierBeforeIfWithBlock() {
        assertNoParserErrors(
            """%if(is_num(rot)) {
                rotate(rot) circle(3);
                scale([1,0.5]) circle(4);
            }""",
            "% modifier before if statement with block"
        )
    }

    // ========== Digit-Starting Identifiers ==========
    
    @Test
    fun testDigitStartingAssignment() {
        assertNoParserErrors(
            "2D = true;",
            "Digit-starting variable assignment"
        )
    }
    
    @Test
    fun testDigitStartingModuleCall() {
        assertNoParserErrors(
            "2Dpipe(thickness=5);",
            "Digit-starting module call"
        )
    }
    
    @Test
    fun testDigitStartingInExpression() {
        assertNoParserErrors(
            "x = 2D ? a : b;",
            "Digit-starting identifier in ternary expression"
        )
    }

    // ========== For Loop Patterns ==========
    
    @Test
    fun testForLoopWithoutVariable() {
        assertNoParserErrors(
            "for([0:10]) cube(1);",
            "For loop without variable binding"
        )
    }
    
    @Test
    fun testForLoopWithMultipleBindings() {
        assertNoParserErrors(
            "for(i=[0:5], j=[0:3]) translate([i,j,0]) cube(1);",
            "For loop with multiple bindings"
        )
    }

    // ========== List Comprehension with Additional Elements ==========
    
    @Test
    fun testListComprehensionWithTrailingElements() {
        assertNoParserErrors(
            "x = [for(i=[0:5]) i*2, [1,0], [0,0]];",
            "List comprehension followed by additional vector elements"
        )
    }
    
    @Test
    fun testComplexListComprehensionWithTrailingElements() {
        assertNoParserErrors(
            """function zigZag(e=5,x=50,y=5,mod=2)=
                [for(i=[0:e*mod])[i%mod<1?i*x:i*x+1,i%mod<1?2:y],[x,0],[0,0]];""",
            "Function returning list comprehension with trailing elements"
        )
    }
    
    // ========== Chained Generator Expressions ==========
    
    @Test
    fun testEachForEachChain() {
        assertNoParserErrors(
            "x = [each for(i=[0:5]) each [i, i*2]];",
            "each for each chain in vector"
        )
    }
    
    @Test
    fun testIfEachInVector() {
        assertNoParserErrors(
            "x = [if(cond) each [1, 2, 3]];",
            "if followed by each in vector"
        )
    }
    
    @Test
    fun testEachVector() {
        assertNoParserErrors(
            "x = [each [1,2,3], each [4,5,6]];",
            "each with vector literal"
        )
    }
    
    @Test
    fun testExactLine5241Pattern() {
        // Exact pattern from test-use.scad line 5241
        assertNoParserErrors(
            """points=[
                each for(i=[0:5])each[i, i*2],
                if (rand) each[
                    if(cond)[0,1],
                    each for(j=[0:3])each[j, j*2],
                ],
                if (rand==0) each[ [1,2],[3,4] ],
            ];""",
            "Complex nested each/for/if pattern from line 5241"
        )
    }
    
    @Test
    fun testEachWithParenthesizedExpression() {
        // Pattern from line 1902: each([...]*expr)
        assertNoParserErrors(
            "x = [for(i=[0:5])[each([cos(i),sin(i)]*r),z]];",
            "each with parenthesized expression"
        )
    }
    
    @Test
    fun testEachParenExpressionSimple() {
        assertNoParserErrors(
            "x = [each(v)];",
            "each with simple parenthesized variable"
        )
    }
    
    // ========== If-Else with For in List Comprehension ==========
    
    @Test
    fun testIfElseForInComprehension() {
        // Pattern from line 10993-10994: if(...) for(...) expr else for(...) expr
        assertNoParserErrors(
            """x = [for(z=[0:5])
                if(z%2) for(i=[0:3]) [i, z]
                else for(i=[0:3]) [i*2, z]
            ];""",
            "if-else with for loops in list comprehension"
        )
    }
    
    @Test
    fun testIfElseForWithLet() {
        assertNoParserErrors(
            """x = [for(z=[0:5])
                if(z%2) for(i=[0:3]) let(r=i*2) [r, z]
                else for(i=[0:3]) [i, z]
            ];""",
            "if-else with for and let in comprehension"
        )
    }

    // ========== Trailing Commas (Issue #12) ==========
    
    @Test
    fun testTrailingCommaInVector() {
        assertNoParserErrors(
            "x = [1, 2, 3,];",
            "Trailing comma in vector literal"
        )
    }
    
    @Test
    fun testTrailingCommaInFunctionArguments() {
        assertNoParserErrors(
            "cube(size=10,);",
            "Trailing comma in function arguments"
        )
    }
    
    @Test
    fun testTrailingCommaInFunctionParameters() {
        assertNoParserErrors(
            "function f(a, b,) = a + b;",
            "Trailing comma in function parameters"
        )
    }
    
    @Test
    fun testTrailingCommaInModuleParameters() {
        assertNoParserErrors(
            "module m(x, y,) { cube(x); }",
            "Trailing comma in module parameters"
        )
    }
    
    @Test
    fun testTrailingCommaInModuleCall() {
        assertNoParserErrors(
            "translate([1, 2, 3,]) cube(10);",
            "Trailing comma in module call arguments"
        )
    }
    
    @Test
    fun testTrailingCommaInNestedVector() {
        assertNoParserErrors(
            "x = [[1, 2,], [3, 4,],];",
            "Trailing commas in nested vectors"
        )
    }
    
    @Test
    fun testTrailingCommaInForBinding() {
        assertNoParserErrors(
            "for(i=[0:5], j=[0:3],) cube([i, j, 1]);",
            "Trailing comma in for loop bindings"
        )
    }
    
    @Test
    fun testTrailingCommaInLetAssignments() {
        assertNoParserErrors(
            "x = let(a=1, b=2,) a + b;",
            "Trailing comma in let assignments"
        )
    }

    // ========== Parser Validation Helper ==========
    
    private fun assertNoParserErrors(code: String, description: String) {
        val errors = LightweightParserValidator.validate(code)
        if (errors.isNotEmpty()) {
            throw AssertionError("$description\nFound ${errors.size} parser errors:\n${errors.joinToString("\n") { it.message }}\nCode: $code")
        }
    }
}
