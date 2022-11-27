#version 330

uniform sampler2D toPostProcTexture;
uniform int u_colorMode;
uniform int u_width;
uniform int u_height;

in vec2 origPos;

out vec4 outColor;

int hasCell(float x, float y) {

    vec2 texPos = vec2(x,y);

    vec4 texColor = texture(toPostProcTexture,texPos);

    if (texColor.r < 0.5f) {
        return 0;
    }
    else {
        return 1;
    }
}

int getCellScore() {
    float stepH = 1.f/u_height;
    float stepW = 1.f/u_width;

    int livingCells = hasCell(origPos.x - stepW,origPos.y + stepH) + hasCell(origPos.x,origPos.y + stepH) + hasCell(origPos.x + stepW,origPos.y + stepH) +
    hasCell(origPos.x - stepW,origPos.y) + hasCell(origPos.x + stepW,origPos.y) +
    hasCell(origPos.x - stepW,origPos.y - stepH) + hasCell(origPos.x,origPos.y - stepH) + hasCell(origPos.x + stepW,origPos.y - stepH);

    return livingCells;
}

void main() {
    vec4 origCol = texture(toPostProcTexture, origPos);
    bool isCell = hasCell(origPos.x,origPos.y) == 1;
    int score = getCellScore();
    float mixNum = score/8.f;


    if (isCell){
        switch (u_colorMode) {
            // White only
            case 0:
                outColor = vec4(1,1,1,1);
                break;
            // Plasma
            case 1:
                vec3 plasmaColor1 = vec3(1.9,0.55,0);
                vec3 plasmaColor2 = vec3(0.226,0.000,0.615);
                vec3 mixedPlasma = mix(plasmaColor1,plasmaColor2,mixNum);
                outColor = vec4(mixedPlasma,1.f);
                break;
            // Cold
            case 2:
                vec3 coldColor1 = vec3(0.f,1.f,1.f);
                vec3 coldColor2 = vec3(1.f,0.f,1.f);
                vec3 mixedCold = mix(coldColor1,coldColor2,mixNum);
                outColor = vec4(mixedCold,1.f);
                break;
        }
    }
    else {
        outColor = vec4(0.015,0.015,0.015,1);
    }



}