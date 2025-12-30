// Test OpenSCAD file for integration tests

// Simple cube
cube(10);

// Translated sphere
translate([20, 0, 0])
    sphere(r = 5);

// Custom module
module my_shape(size = 10) {
    difference() {
        cube(size, center = true);
        sphere(r = size / 2);
    }
}

// Use custom module
my_shape(15);

// Function example
function double(x) = x * 2;

// Variable with function
size = double(5);

// Use variable
cylinder(h = size, r = 3);
