#version 330

/*
2DDisplayHelper is a helper shader program that only serves to hold previous step of the
simulation, or to draw the 2D texture to screen in edit mode

The vertex shader only takes grid and makes it fullscreen while also passing on the texture coords
*/

in vec2 inPos;

out vec2 origPos;

void main() {
    origPos = inPos;
    vec2 fullPos = inPos * 2 - 1;
    gl_Position = vec4(fullPos,0.f,1.f);
}