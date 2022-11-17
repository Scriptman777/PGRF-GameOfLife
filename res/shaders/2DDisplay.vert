#version 330

in vec2 inPos;

void main() {
    vec2 fullPos = inPos * 2 - 1;
    gl_Position = vec4(fullPos,0.f,1.f);
}