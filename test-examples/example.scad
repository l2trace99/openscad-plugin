// OpenSCAD Test File
// This file demonstrates various OpenSCAD features

// Special variables
$fn = 50;  // Number of fragments
$fa = 12;  // Minimum angle
$fs = 2;   // Minimum size

// Module declaration with parameters
module box(width=10, height=10, depth=10, center=false) {
    if (center) {
        translate([-width/2, -height/2, -depth/2])
            cube([width, height, depth]);
    } else {
        cube([width, height, depth]);
    }
}

// Function declaration
function fibonacci(n) = 
    n <= 1 ? n : fibonacci(n-1) + fibonacci(n-2);

// Module with for loop
module grid(rows=5, cols=5, spacing=10) {
    for (i = [0:rows-1], j = [0:cols-1]) {
        translate([i*spacing, j*spacing, 0])
            cube(5);
    }
}

// Boolean operations
module csg_example() {
    difference() {
        // Main body
        union() {
            cube([20, 20, 20], center=true);
            translate([0, 0, 15])
                sphere(r=10);
        }
        
        // Subtract cylinder
        cylinder(h=30, r=5, center=true);
    }
}

// Transformations
module transformed_shapes() {
    // Translate
    translate([30, 0, 0])
        cube(10);
    
    // Rotate
    rotate([0, 0, 45])
        translate([50, 0, 0])
            cube(10);
    
    // Scale
    scale([2, 1, 1])
        translate([70, 0, 0])
            cube(10);
    
    // Mirror
    mirror([1, 0, 0])
        translate([90, 0, 0])
            cube(10);
}

// List comprehension
module comprehension_example() {
    points = [for (i = [0:10]) [i*2, i*i, 0]];
    
    for (p = points) {
        translate(p)
            sphere(r=1);
    }
}

// Let statement
module let_example() {
    let (r = 10, h = 20) {
        cylinder(h=h, r=r);
        translate([0, 0, h])
            sphere(r=r);
    }
}

// Let expression
function circle_area(r) = let(pi = 3.14159) pi * r * r;

// Conditional expression
function abs(x) = x < 0 ? -x : x;

// Vector operations
module vector_example() {
    v1 = [1, 2, 3];
    v2 = [4, 5, 6];
    
    // Vector indexing
    echo("First element:", v1[0]);
    
    // Vector slicing
    echo("Slice:", v1[0:1]);
}

// 2D primitives
module shapes_2d() {
    circle(r=10);
    
    translate([30, 0, 0])
        square([20, 20], center=true);
    
    translate([60, 0, 0])
        polygon(points=[[0,0], [10,0], [10,10], [0,10]]);
}

// Linear extrude
module extruded_shape() {
    linear_extrude(height=10, twist=90, slices=20)
        circle(r=5);
}

// Rotate extrude
module revolved_shape() {
    rotate_extrude(angle=360)
        translate([20, 0, 0])
            circle(r=5);
}

// Object modifiers
module modifiers_example() {
    # cube(10);           // Debug (transparent)
    ! sphere(r=5);        // Root (only this shown)
    % cylinder(h=10, r=3); // Background (transparent gray)
    * translate([20, 0, 0]) cube(5); // Disable (not shown)
}

// Hull operation
module hull_example() {
    hull() {
        translate([0, 0, 0]) sphere(r=5);
        translate([20, 0, 0]) sphere(r=5);
        translate([10, 20, 0]) sphere(r=5);
    }
}

// Minkowski sum
module minkowski_example() {
    minkowski() {
        cube([10, 10, 1]);
        cylinder(r=2, h=1);
    }
}

// Include and use
// include <library.scad>
// use <utilities.scad>

// Intersection for
module intersection_for_example() {
    intersection_for(i = [0:3]) {
        rotate([0, 0, i*90])
            translate([5, 0, 0])
                cube([10, 10, 10]);
    }
}

// Text
module text_example() {
    linear_extrude(height=2)
        text("OpenSCAD", size=10, font="Liberation Sans");
}

// Import
// import("model.stl");

// Surface from heightmap
// surface(file="heightmap.dat", center=true);

// Color
module colored_shapes() {
    color("red") cube(10);
    color([0, 1, 0]) translate([15, 0, 0]) cube(10);
    color([0, 0, 1, 0.5]) translate([30, 0, 0]) cube(10);
}

// Main assembly
union() {
    box(20, 20, 20, center=true);
    translate([30, 0, 0]) csg_example();
    translate([60, 0, 0]) transformed_shapes();
}
