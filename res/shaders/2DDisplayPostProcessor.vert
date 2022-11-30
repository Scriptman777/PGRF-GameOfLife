#version 330

/*
2DDisplayPostProcessor shader program is used to add effects to the Game of Life simulation, like
coloring based on neighbouring cells or changing color with time

The vertex shader only takes grid and makes it fullscreen while also passing on the texture coords
*/

in vec2 inPos;

out vec2 origPos;

void main() {
    origPos = inPos;
    vec2 fullPos = inPos * 2 - 1;
    gl_Position = vec4(fullPos,0.f,1.f);
}