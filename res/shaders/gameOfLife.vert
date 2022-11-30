#version 330

/*
Game of Life shader program is used to simulate the cells on a texture, it also allows for drawing new cells
and changing the rules by which cells behave

The vertex shader does not need to do anything but pass texture coordinates and resize the grid
*/

in vec2 inPos;

out vec2 origPos;

void main() {

    origPos = inPos;
    vec2 fullPos = inPos * 2 - 1;
    gl_Position = vec4(fullPos,0.f,1.f);
}