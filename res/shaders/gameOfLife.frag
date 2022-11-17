#version 330

in vec3 vertColor;

uniform int time;

out vec4 outColor;

void main() {
    outColor = vec4(vertColor, 1.f);
}