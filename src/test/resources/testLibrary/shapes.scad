// Test library file for unit tests

// Public module for creating a rounded cube
module rounded_cube(size = 10, radius = 1) {
    minkowski() {
        cube(size - 2 * radius, center = true);
        sphere(r = radius);
    }
}

// Public module for creating a hollow cylinder
module hollow_cylinder(outer_r = 10, inner_r = 8, height = 20) {
    difference() {
        cylinder(r = outer_r, h = height);
        translate([0, 0, -1])
            cylinder(r = inner_r, h = height + 2);
    }
}

// Public function to calculate volume
function cube_volume(size) = size * size * size;

// Public function to calculate cylinder volume
function cylinder_volume(r, h) = PI * r * r * h;

// Private module (should not be indexed)
module _internal_helper() {
    cube(1);
}

// Private function (should not be indexed)
function _private_calc(x) = x * 2;
