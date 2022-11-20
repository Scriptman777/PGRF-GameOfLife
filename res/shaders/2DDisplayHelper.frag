#version 330

uniform sampler2D fromDisplayTexture;

in vec2 origPos;

out vec4 outColor;

void main() {
    outColor = texture(fromDisplayTexture, origPos);
}