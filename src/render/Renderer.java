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
    private final int shaderProgram3D;
    private final int shaderProgram2DDisplay;
    private final int shaderProgramGoL;
    private Camera camera;
    private Mat4 projection;
    private OGLTexture2D texture;
    private OGLRenderTarget renderTargetGoLWorker;
    private OGLRenderTarget renderTargetGoLDisplay;
    private OGLTexture2D.Viewer viewer;
    private AbstractRenderable fullScreenGrid = new GridTriangles(200,200);
    private long window;
    private int width, height;
    private int GoLsize;
    private int brushSize = 2;
    private int ruleSet = 0;
    private int bodyID = 0;
    private double ox, oy;
    private boolean mouseButton1 = false;
    private float camSpeed = 0.05f;
    private boolean pause = true;
    private boolean use3D = false;
    private boolean clearAll = false;
    private boolean loadingPass = true;
    private boolean displayViewer = false;

    //Uniforms
    int loc_uView;
    int loc_uProj;
    int loc_uHeight;
    int loc_uWidth;
    int loc_uDrawX;
    int loc_uDrawY;
    int loc_uAddCells;
    int loc_uPause;
    int loc_uBrushSize;
    int loc_uRuleSet;
    int loc_uClearAll;
    int loc_uBodyID;

    public Renderer(long window, int width, int height) {
        this.window = window;
        this.width = width;
        this.height = height;

        GoLsize = 6154;

        renderTargetGoLWorker = new OGLRenderTarget(GoLsize,GoLsize);
        renderTargetGoLDisplay = new OGLRenderTarget(GoLsize,GoLsize);

        // Load in the init texture
        try {
            texture = new OGLTexture2D("GoLInits/6154spinSwitch.png");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // MVP init
        camera = new Camera()
                .withPosition(new Vec3D(0.5f, -2f, 1.5f))
                .withAzimuth(Math.toRadians(90))
                .withZenith(Math.toRadians(-45));
        projection = new Mat4PerspRH(Math.PI / 3, 600 / (float)800, 0.1f, 50.f);

        // Shader init
        shaderProgram3D = ShaderUtils.loadProgram("/shaders/3DScene");
        shaderProgram2DDisplay = ShaderUtils.loadProgram("/shaders/2DDisplayHelper");
        shaderProgramGoL = ShaderUtils.loadProgram("/shaders/gameOfLife");

        // Uniform loc get
        loc_uView = glGetUniformLocation(shaderProgram3D, "u_View");
        loc_uProj = glGetUniformLocation(shaderProgram3D, "u_Proj");
        loc_uBodyID = glGetUniformLocation(shaderProgram3D, "u_bodyID");

        loc_uWidth = glGetUniformLocation(shaderProgramGoL, "u_width");
        loc_uHeight = glGetUniformLocation(shaderProgramGoL, "u_height");
        loc_uDrawX = glGetUniformLocation(shaderProgramGoL, "u_drawX");
        loc_uDrawY = glGetUniformLocation(shaderProgramGoL, "u_drawY");
        loc_uAddCells = glGetUniformLocation(shaderProgramGoL, "u_addCells");
        loc_uPause = glGetUniformLocation(shaderProgramGoL, "u_pause");
        loc_uBrushSize = glGetUniformLocation(shaderProgramGoL, "u_brushSize");
        loc_uRuleSet = glGetUniformLocation(shaderProgramGoL, "u_ruleSet");
        loc_uClearAll = glGetUniformLocation(shaderProgramGoL, "u_clearAll");



        texture.bind(shaderProgramGoL,"initTexture",0);

        viewer = new OGLTexture2D.Viewer();

        glPolygonMode(GL_FRONT_AND_BACK, GL_TRIANGLES);

        initControls();
    }


    /**
     * Main draw loop with optional buffer viewer
     */
    public void draw() {

        chechRTSize();
        drawStepWorker();
        drawFrontBuffer();
        drawToScreen();
        // If GoL was cleared, stop clearing
        clearAll = false;
        loadingPass = false;


        if (displayViewer) {
            // Viewer
            viewer.view(renderTargetGoLWorker.getColorTexture(),-1,-1,0.5);
            viewer.view(renderTargetGoLDisplay.getColorTexture(),-1,-0.5,0.5);
        }

    }

    /**
     * An unfortunate solution to some part of lwjgl utils turning interpolation back on
     */
    public void doNotInterpolate() {
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
    }


    /**
     * Simulates one step of Game of Life
     */
    public void drawStepWorker() {


        doNotInterpolate();
        // Draw into renderTarget
        glDisable(GL_DEPTH_TEST);

        if (!loadingPass) {
            renderTargetGoLDisplay.bindColorTexture(shaderProgramGoL,"initTexture",0);
        }


        renderTargetGoLWorker.bind();

        glUseProgram(shaderProgramGoL);

        glUniform1i(loc_uHeight, GoLsize);
        glUniform1i(loc_uWidth, GoLsize);
        glUniform1f(loc_uDrawX, (float) (ox/width));
        glUniform1f(loc_uDrawY, (float) ((height - oy)/height));
        glUniform1i(loc_uAddCells, mouseButton1 && !use3D ? 1 : 0);
        glUniform1i(loc_uPause, pause ? 1 : 0);
        glUniform1i(loc_uClearAll, clearAll ? 1 : 0);
        glUniform1f(loc_uBrushSize, (float) brushSize);
        glUniform1i(loc_uRuleSet, ruleSet);

        fullScreenGrid.draw(shaderProgramGoL);
    }

    /**
     * Buffer that holds a previous state for the worker, also used as display
     */
    public void drawFrontBuffer() {
        doNotInterpolate();

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);

        renderTargetGoLWorker.getColorTexture().bind(shaderProgram2DDisplay,"toDisplayTexture",0);
        renderTargetGoLDisplay.bind();

        glUseProgram(shaderProgram2DDisplay);

        fullScreenGrid.draw(shaderProgram2DDisplay);

    }

    /**
     * Draw result to screen or on a 3D object
     */
    public void drawToScreen() {
        doNotInterpolate();

        // Reset viewport that was adjusted in RenderTarget.bind
        glViewport(0, 0, width, height);

        renderTargetGoLDisplay.getColorTexture().bind(shaderProgram2DDisplay, "fromDisplayTexture",0);

        glBindFramebuffer(GL_FRAMEBUFFER,0);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        if (use3D) {
            // Draw 3D Scene - view mode
            glEnable(GL_DEPTH_TEST);

            glUseProgram(shaderProgram3D);

            glUniformMatrix4fv(loc_uView, false, camera.getViewMatrix().floatArray());
            glUniformMatrix4fv(loc_uProj, false, projection.floatArray());
            glUniform1i(loc_uBodyID, bodyID);

            fullScreenGrid.draw(shaderProgram3D);
        }
        else {
            // Draw texture to screen - edit mode

            glUseProgram(shaderProgram2DDisplay);

            fullScreenGrid.draw(shaderProgram2DDisplay);
        }


    }

    /**
     * Checks if user changed the buffer size and creates new ones
     */
    private void chechRTSize() {
        if (renderTargetGoLWorker.getHeight() != GoLsize) {
            renderTargetGoLWorker = new OGLRenderTarget(GoLsize,GoLsize);
            renderTargetGoLDisplay = new OGLRenderTarget(GoLsize,GoLsize);
        }
    }

    private void initControls() {


        // Mose move (from samples)
        glfwSetCursorPosCallback(window, new GLFWCursorPosCallback() {
            @Override
            public void invoke(long window, double x, double y) {
                if (mouseButton1 && use3D) {
                    camera = camera.addAzimuth((double) Math.PI * (ox - x) / width)
                            .addZenith((double) Math.PI * (oy - y) / width);
                }
                ox = x;
                oy = y;
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
                    case GLFW_KEY_SPACE:
                        pause = !pause;
                        break;
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
                    case GLFW_KEY_LEFT_SHIFT:
                        use3D = !use3D;
                        break;
                    case GLFW_KEY_LEFT_CONTROL:
                        clearAll = !clearAll;
                        break;
                    case GLFW_KEY_LEFT_ALT:
                        displayViewer = !displayViewer;
                        break;
                    case GLFW_KEY_R:
                        camera = camera.mulRadius(0.9f);
                        break;
                    case GLFW_KEY_F:
                        camera = camera.mulRadius(1.1f);
                        break;
                    case GLFW_KEY_UP:
                        brushSize++;
                        break;
                    case GLFW_KEY_DOWN:
                        if (brushSize > 1) {
                            brushSize--;
                        }
                        break;
                    case GLFW_KEY_RIGHT:
                        ruleSet++;
                        break;
                    case GLFW_KEY_LEFT:
                        if (ruleSet > 0){
                            ruleSet--;
                        }
                        break;
                    case GLFW_KEY_KP_ADD:
                        GoLsize += 100;
                        break;
                    case GLFW_KEY_KP_SUBTRACT:
                        if (GoLsize > 200)
                        GoLsize -= 100;
                        break;
                    case GLFW_KEY_KP_DIVIDE:
                        bodyID++;
                        break;
                    case GLFW_KEY_KP_MULTIPLY:
                        if (bodyID > 0) {
                            bodyID--;
                        }
                        break;
                }
            }
        });
    }
}

