#version 330


/*
3DScene shader program is used render a scene with an object that will then have the Game of Life mapped onto it

The vertex shader bends the grid into one of 3 different shapes, Flat, Donut and Sphere
*/

in vec2 inPos;

uniform mat4 u_View;
uniform mat4 u_Proj;
uniform int u_bodyID;

out vec2 origPos;

void main() {

    float ratio = 6.3;
    origPos = inPos;
    vec2 pos = inPos * ratio - (ratio/2);

    float x,y,z;

    switch (u_bodyID) {
            case 0:
                x = pos.x;
                y = pos.y;
                z = 1;
                break;
            case 1:
                float a = 2.f;
                float b = 0.5f;

                x = cos(pos.x)*(a + b*cos(pos.y));
                y = sin(pos.x)*(a + b*cos(pos.y));
                z = b*sin(pos.y);

                break;
            case 2:
                float phi, theta, r;
                phi = inPos.x * radians(360.f);
                theta = inPos.y * radians(180.f);

                r = 1.f;

                x = r * sin(theta) * cos(phi);
                y = r * sin(theta) * sin(phi);
                z = r * cos(theta);
                break;

    }

    vec4 posMVP = u_Proj * u_View * vec4(x,y,z, 1.f);
    gl_Position = posMVP;
}

