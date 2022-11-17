package render;

import lwjglutils.OGLRenderTarget;
import lwjglutils.OGLTexture2D;
import lwjglutils.ShaderUtils;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import solids.AbstractRenderable;
import solids.GridTriangles;
import transforms.Camera;
import transforms.Mat4;
import transforms.Mat4PerspRH;
import transforms.Vec3D;

import java.io.IOException;
import java.nio.DoubleBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33.*;

public class Renderer {
    private int shaderProgram3D;
    private int shaderProgram2DDisplay;
    private int shaderProgramGoL;
    private Camera camera;
    private Mat4 projection;
    private OGLTexture2D texture;
    private OGLRenderTarget renderTargetGoL;
    private AbstractRenderable fullScreenGrid = new GridTriangles(2,2);
    private AbstractRenderable testGrid = new GridTriangles(3,3);
    private long window;
    int width, height;
    double ox, oy;
    private boolean mouseButton1 = false;
    float camSpeed = 0.05f;
    boolean firstPass = true;

    //Uniforms
    int loc_uView;
    int loc_uProj;
    int loc_uHeight;
    int loc_uWidth;
    int loc_firstPass;

    public Renderer(long window, int width, int height) {
        this.window = window;
        this.width = width;
        this.height = height;

        renderTargetGoL = new OGLRenderTarget(width,height);

        // MVP init
        camera = new Camera()
                .withPosition(new Vec3D(0.5f, -2f, 1.5f))
                .withAzimuth(Math.toRadians(90))
                .withZenith(Math.toRadians(-45));
        projection = new Mat4PerspRH(Math.PI / 3, 600 / (float)800, 0.1f, 50.f);

        // Shader init
        shaderProgram3D = ShaderUtils.loadProgram("/shaders/3DScene");
        shaderProgram2DDisplay = ShaderUtils.loadProgram("/shaders/2DDisplay");
        shaderProgramGoL = ShaderUtils.loadProgram("/shaders/gameOfLife");

        // Uniform loc get
        loc_uView = glGetUniformLocation(shaderProgram3D, "u_View");
        loc_uProj = glGetUniformLocation(shaderProgram3D, "u_Proj");
        loc_uWidth = glGetUniformLocation(shaderProgramGoL, "u_width");
        loc_uHeight = glGetUniformLocation(shaderProgramGoL, "u_height");
        loc_firstPass = glGetUniformLocation(shaderProgramGoL, "u_firstPass");



        try {
            texture = new OGLTexture2D("GoLInits/GolTest.png");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        texture.bind(shaderProgramGoL,"initTexture",0);





        initControls();
    }


    public void draw() {
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP);
        drawToTexture();
        drawToScreen();
        firstPass = false;
    }


    public void drawToTexture() {
        // Draw into renderTarget
        glPolygonMode(GL_FRONT_AND_BACK, GL_TRIANGLES);
        renderTargetGoL.bind();
        glUseProgram(shaderProgramGoL);

        glUniform1i(loc_uHeight, height);
        glUniform1i(loc_uWidth, width);
        glUniform1i(loc_firstPass, firstPass ? 1 : 0);

        fullScreenGrid.draw(shaderProgramGoL);


    }

    public void drawToScreen() {
        glPolygonMode(GL_FRONT_AND_BACK, GL_TRIANGLES);
        // Draw texture to screen
        glBindFramebuffer(GL_FRAMEBUFFER,0);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        renderTargetGoL.getColorTexture().bind(shaderProgramGoL,"inTexture",0);
        glUseProgram(shaderProgram2DDisplay);

        // Useless for now
        /*
        glUniformMatrix4fv(loc_uView, false, camera.getViewMatrix().floatArray());
        glUniformMatrix4fv(loc_uProj, false, projection.floatArray());
        */


        fullScreenGrid.draw(shaderProgram2DDisplay);
    }

    private void initControls() {


        // Mose move (from samples)
        glfwSetCursorPosCallback(window, new GLFWCursorPosCallback() {
            @Override
            public void invoke(long window, double x, double y) {
                if (mouseButton1) {
                    camera = camera.addAzimuth((double) Math.PI * (ox - x) / width)
                            .addZenith((double) Math.PI * (oy - y) / width);
                    ox = x;
                    oy = y;
                }
            }
        });

        // Mose click (from samples)
        glfwSetMouseButtonCallback(window, new GLFWMouseButtonCallback() {

            @Override
            public void invoke(long window, int button, int action, int mods) {
                mouseButton1 = glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_1) == GLFW_PRESS;

                if (button==GLFW_MOUSE_BUTTON_1 && action == GLFW_PRESS){
                    mouseButton1 = true;
                    DoubleBuffer xBuffer = BufferUtils.createDoubleBuffer(1);
                    DoubleBuffer yBuffer = BufferUtils.createDoubleBuffer(1);
                    glfwGetCursorPos(window, xBuffer, yBuffer);
                    ox = xBuffer.get(0);
                    oy = yBuffer.get(0);
                }

                if (button==GLFW_MOUSE_BUTTON_1 && action == GLFW_RELEASE){
                    mouseButton1 = false;
                    DoubleBuffer xBuffer = BufferUtils.createDoubleBuffer(1);
                    DoubleBuffer yBuffer = BufferUtils.createDoubleBuffer(1);
                    glfwGetCursorPos(window, xBuffer, yBuffer);
                    double x = xBuffer.get(0);
                    double y = yBuffer.get(0);
                    camera = camera.addAzimuth((double) Math.PI * (ox - x) / width)
                            .addZenith((double) Math.PI * (oy - y) / width);
                    ox = x;
                    oy = y;
                }
            }

        });

        // Movement keys (based on samples)
        glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
            if ( key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE )
                glfwSetWindowShouldClose(window, true); // We will detect this in the rendering loop
            if (action == GLFW_PRESS || action == GLFW_REPEAT){
                switch (key) {
                    case GLFW_KEY_W:
                        camera = camera.forward(camSpeed);
                        break;
                    case GLFW_KEY_D:
                        camera = camera.right(camSpeed);
                        break;
                    case GLFW_KEY_S:
                        camera = camera.backward(camSpeed);
                        break;
                    case GLFW_KEY_A:
                        camera = camera.left(camSpeed);
                        break;
                    case GLFW_KEY_LEFT_CONTROL:
                        camera = camera.down(camSpeed);
                        break;
                    case GLFW_KEY_LEFT_SHIFT:
                        camera = camera.up(camSpeed);
                        break;
                    case GLFW_KEY_SPACE:
                        camera = camera.withFirstPerson(!camera.getFirstPerson());
                        break;
                    case GLFW_KEY_R:
                        camera = camera.mulRadius(0.9f);
                        break;
                    case GLFW_KEY_F:
                        camera = camera.mulRadius(1.1f);
                        break;
                }
            }
        });
    }
}

