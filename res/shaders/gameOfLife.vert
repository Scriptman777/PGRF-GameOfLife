#version 330

in vec2 inPos;

out vec2 origPos;

void main() {

    origPos = inPos;
    vec2 fullPos = inPos * 2 - 1;
    gl_Position = vec4(fullPos,0.f,1.f);
}