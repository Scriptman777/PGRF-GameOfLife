#version 330

uniform int time;

uniform sampler2D toDisplayTexture;

in vec2 origPos;

out vec4 outColor;

void main() {
    outColor = texture(toDisplayTexture, origPos);
}