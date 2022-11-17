#version 330

in vec2 origPos;

uniform int time;
uniform int u_width;
uniform int u_height;
uniform int u_firstPass;
uniform sampler2D initTexture;
uniform sampler2D inTexture;

out vec4 outColor;

int hasCell(float x, float y) {
    vec2 texPos = vec2(x,y);

    vec4 texColor = texture(initTexture,texPos);

    if (texColor.r < 0.5f) {
        return 0;
    }
    else {
        return 1;
    }

}

void main() {

    float stepW = 1.f/u_width;
    float stepH = 1.f/u_height;

    int currentCell = hasCell(origPos.x,origPos.y);


    int livingCells =
    hasCell(origPos.x - stepW,origPos.y + stepH) + hasCell(origPos.x,origPos.y + stepH) + hasCell(origPos.x + stepW,origPos.y + stepH) +
    hasCell(origPos.x - stepW,origPos.y) + hasCell(origPos.x + stepW,origPos.y) +
    hasCell(origPos.x - stepW,origPos.y - stepH) + hasCell(origPos.x,origPos.y - stepH) + hasCell(origPos.x + stepW,origPos.y - stepH);


    if (livingCells > 4) {
        // Overpopulated and dies
        outColor = vec4(0,0,0,1);
    }
    else if (livingCells == 2) {
        // Cell survives, empty does nothing
        outColor = texture(initTexture,origPos);
    }
    else if (livingCells == 3) {
        // Cell survives, empty is born
        outColor = vec4(1,0,0,1);
    }
    else {
        // Dies of lonelyness :(
        outColor = vec4(0,0,0,1);
    }


}