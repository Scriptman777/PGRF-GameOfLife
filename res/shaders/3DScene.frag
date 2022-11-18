#version 330

uniform int time;

uniform sampler2D inTexture;

in vec2 origPos;

out vec4 outColor;

void main() {
    //outColor = texture(inTexture, origPos);
    outColor = vec4(1,0,0,1);
}