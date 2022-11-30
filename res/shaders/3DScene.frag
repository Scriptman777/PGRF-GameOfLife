#version 330

/*
3DScene shader program is used render a scene with an object that will then have the Game of Life mapped onto it

The fragment shader only takes color from the texture
*/

uniform int time;

uniform sampler2D toDisplayTexture;

in vec2 origPos;

out vec4 outColor;

void main() {
    outColor = texture(toDisplayTexture, origPos);
}