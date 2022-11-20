#version 330

in vec2 origPos;

uniform int time;
uniform int u_width;
uniform int u_height;
uniform float u_drawX;
uniform float u_drawY;
uniform int u_addCells;
uniform int u_pause;
uniform float u_brushSize;
uniform int u_ruleSet;
uniform int u_clearAll;


uniform sampler2D initTexture;

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


// Cell coloring, ready for alternate rulesets with more neighbours
vec4 getCellColor(int cells) {
    vec3 plasmaColor1 = vec3(1.9,0.55,0);
    vec3 plasmaColor2 = vec3(0.226,0.000,0.615);
    float mixNum = cells/8.f;
    vec3 mixed = mix(plasmaColor1,plasmaColor2,mixNum);
    return vec4(mixed, 1.f);
}

// Get color of background
vec4 getSpaceColor() {
    return vec4(0.1,0.1,0.1,1);
}

// The original Conway ruleset
vec4 stepConway(int livingCells, int currentCell) {
    if (livingCells == 3) {
        // Cell survives, empty is born
        return getCellColor(livingCells);
    }
    else if (livingCells == 2) {
        // Cell survives, empty does nothing
        if (currentCell == 1) {
            return getCellColor(livingCells);
        }
        else {
            return getSpaceColor();
        }
    }
    else {
        // Dies
        return getSpaceColor();
    }

}

// Maze creating ruleset
vec4 stepMaze(int livingCells, int currentCell){

    // Born
    if (livingCells == 3) {
        return getCellColor(livingCells);
    }
    // Survives
    else if (livingCells > 0 && livingCells < 6) {
        if (currentCell == 1) {
            return getCellColor(livingCells);
        }
        else {
            return getSpaceColor();
        }
    }
    // Dies
    else {
        return getSpaceColor();
    }
}

// "Walled cities" ruleset
vec4 stepCities(int livingCells, int currentCell){
    // Born
    if (livingCells > 3) {
        return getCellColor(livingCells);
    }
    // Survives
    else if (livingCells > 1 && livingCells < 6) {
        if (currentCell == 1) {
            return getCellColor(livingCells);
        }
        else {
            return getSpaceColor();
        }
    }
    // Dies
    else {
        return getSpaceColor();
    }
}

// Amoeba ruleset
vec4 stepAmoeba(int livingCells, int currentCell){
    // Born
    if (livingCells == 3 || livingCells == 5 || livingCells == 7) {
        return getCellColor(livingCells);
    }
    // Survives
    else if (livingCells == 1 || livingCells == 3 || livingCells == 5 || livingCells == 8) {
        if (currentCell == 1) {
            return getCellColor(livingCells);
        }
        else {
            return getSpaceColor();
        }
    }
    // Dies
    else {
        return getSpaceColor();
    }
}

vec4 stepDisapear(int livingCells, int currentCell) {
    // Born
    if (livingCells == 3 || livingCells > 4) {
        return getCellColor(livingCells);
    }
    // Survives
    else if (livingCells > 4) {
        if (currentCell == 1) {
            return getCellColor(livingCells);
        }
        else {
            return getSpaceColor();
        }
    }
    // Dies
    else {
        return getSpaceColor();
    }
}

vec4 stepStains(int livingCells, int currentCell){
    // Born
    if (livingCells == 3 || livingCells == 4) {
        return getCellColor(livingCells);
    }
    // Survives
    else if (livingCells < 3) {
        if (currentCell == 1) {
            return getCellColor(livingCells);
        }
        else {
            return getSpaceColor();
        }
    }
    // Dies
    else {
        return getSpaceColor();
    }
}



// Simulate one step with a specific ruleset
vec4 simulateStep(int livingCells) {

    // See if the current pixel has a cell
    int currentCell = hasCell(origPos.x,origPos.y);

    if (u_clearAll == 1) {
        return getSpaceColor();
    }

    switch (u_ruleSet) {
        case 0:
            return stepConway(livingCells, currentCell);
        case 1:
            return stepMaze(livingCells, currentCell);
        case 2:
            return stepAmoeba(livingCells, currentCell);
        case 3:
            return stepCities(livingCells, currentCell);
        case 4:
            return stepDisapear(livingCells, currentCell);
        case 5:
            return stepStains(livingCells, currentCell);
    }

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
        if (abs(origPos.x - u_drawX) < u_brushSize/u_width && abs(origPos.y - u_drawY) < u_brushSize/u_height) {
            outColor = getCellColor(livingCells);
        }

    }


}