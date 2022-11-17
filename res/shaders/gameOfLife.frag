#version 330

in vec2 origPos;

uniform int time;
uniform int u_width;
uniform int u_height;
uniform int u_drawX;
uniform int u_drawY;
uniform int u_addCells;
uniform int u_pause;


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


// Cell coloring, ready for alternate rulesets
vec4 getCellColor(int cells) {
    vec3 plasmaColor1 = vec3(1.9,0.55,0);
    vec3 plasmaColor2 = vec3(0.226,0.000,0.615);
    float mixNum = cells/8.f;
    vec3 mixed = mix(plasmaColor1,plasmaColor2,mixNum);
    return vec4(mixed, 1.f);
}

vec4 simulateStep(int livingCells) {

    int currentCell = hasCell(origPos.x,origPos.y);
    vec4 stepResult;


    // More = Overpopulated and dies
    if (livingCells == 3) {
        // Cell survives, empty is born
        stepResult = getCellColor(livingCells);
    }
    else if (livingCells == 2) {
        // Cell survives, empty does nothing
        if (currentCell == 1) {
            stepResult = getCellColor(livingCells);
        }
        else {
            stepResult = vec4(0,0,0,1);
        }
    }
    else {
        // Dies of lonelyness :(
        stepResult = vec4(0,0,0,1);
    }

    return stepResult;

}

void main() {


    // Find cells around and count them
    float stepW = 1.f/u_width;
    float stepH = 1.f/u_height;


    int livingCells =
    hasCell(origPos.x - stepW,origPos.y + stepH) + hasCell(origPos.x,origPos.y + stepH) + hasCell(origPos.x + stepW,origPos.y + stepH) +
    hasCell(origPos.x - stepW,origPos.y) + hasCell(origPos.x + stepW,origPos.y) +
    hasCell(origPos.x - stepW,origPos.y - stepH) + hasCell(origPos.x,origPos.y - stepH) + hasCell(origPos.x + stepW,origPos.y - stepH);


    // Simulate one step
    if (u_pause == 0) {
        outColor = simulateStep(livingCells);
    }
    else {
        outColor = texture(initTexture,origPos);
    }


    // Draw new cells when clicked
    if (u_addCells == 1) {
        if (gl_FragCoord.x == u_drawX + 0.5f && gl_FragCoord.y == u_drawY + 0.5f) {
            outColor = getCellColor(livingCells);
        }

    }


}