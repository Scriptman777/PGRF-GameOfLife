#version 330

/*
2DDisplayHelper is a helper shader program that only serves to hold previous step of the
simulation, or to draw the 2D texture to screen in edit mode

The fragment shader only reads the texture
*/

uniform sampler2D fromDisplayTexture;

in vec2 origPos;

out vec4 outColor;

void main() {
    outColor = texture(fromDisplayTexture, origPos);
}