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

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.DoubleBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33.*;

public class Renderer {
    private final int shaderProgram3D;
    private final int shaderProgram2DDisplayHelper;
    private final int shaderProgramGoL;
    private final int shaderProgram2DPostProcess;
    private Camera camera;
    private Mat4 projection;
    private OGLTexture2D texture;
    private OGLRenderTarget renderTargetGoLWorker;
    private OGLRenderTarget renderTargetGoLDisplay;
    private OGLRenderTarget renderTargetGoLPostProcess;
    private OGLTexture2D.Viewer viewer;
    private AbstractRenderable fullScreenGrid = new GridTriangles(200,200);
    private JFrame helpFrame;
    private long window;
    private int width, height;
    private int GoLsize;
    private int brushSize = 2;
    private int ruleSet = 0;
    private int colorMode = 0;
    private int bodyID = 0;
    private double ox, oy;
    private boolean mouseButton1 = false;
    private float camSpeed = 0.05f;
    private float time = 0.f;
    private boolean pause = true;
    private boolean use3D = false;
    private boolean clearAll = false;
    private boolean loadingPass = true;
    private boolean displayViewer = false;
    private boolean useRepeat = true;

    //Uniforms
    int loc_uView;
    int loc_uProj;
    int loc_uHeightWorker;
    int loc_uWidthWorker;
    int loc_uDrawX;
    int loc_uDrawY;
    int loc_uAddCells;
    int loc_uPause;
    int loc_uBrushSize;
    int loc_uRuleSet;
    int loc_uClearAll;
    int loc_uBodyID;
    int loc_uColorMode;
    int loc_uWidthPost;
    int loc_uHeightPost;
    int loc_uTime;

    public Renderer(long window, int width, int height) {
        this.window = window;
        this.width = width;
        this.height = height;

        // Size - should be the same as texture
        GoLsize = 500;
        String textureToUse = "GoLInits/500BlinkerArray.png";



        renderTargetGoLWorker = new OGLRenderTarget(GoLsize,GoLsize);
        renderTargetGoLDisplay = new OGLRenderTarget(GoLsize,GoLsize);
        renderTargetGoLPostProcess = new OGLRenderTarget(GoLsize,GoLsize);


        // MVP init
        camera = new Camera()
                .withPosition(new Vec3D(0.5f, -2f, 1.5f))
                .withAzimuth(Math.toRadians(90))
                .withZenith(Math.toRadians(-45));
        projection = new Mat4PerspRH(Math.PI / 3, 600 / (float)800, 0.1f, 50.f);

        // Shader init
        shaderProgram3D = ShaderUtils.loadProgram("/shaders/3DScene");
        shaderProgram2DDisplayHelper = ShaderUtils.loadProgram("/shaders/2DDisplayHelper");
        shaderProgram2DPostProcess = ShaderUtils.loadProgram("/shaders/2DDisplayPostProcessor");
        shaderProgramGoL = ShaderUtils.loadProgram("/shaders/gameOfLife");

        // Uniform loc get
        loc_uView = glGetUniformLocation(shaderProgram3D, "u_View");
        loc_uProj = glGetUniformLocation(shaderProgram3D, "u_Proj");
        loc_uBodyID = glGetUniformLocation(shaderProgram3D, "u_bodyID");

        // Needs to get locations twice, since different shader programs have different places in memory
        loc_uWidthWorker = glGetUniformLocation(shaderProgramGoL, "u_width");
        loc_uHeightWorker = glGetUniformLocation(shaderProgramGoL, "u_height");
        loc_uWidthPost = glGetUniformLocation(shaderProgram2DPostProcess, "u_width");
        loc_uHeightPost = glGetUniformLocation(shaderProgram2DPostProcess, "u_height");

        loc_uDrawX = glGetUniformLocation(shaderProgramGoL, "u_drawX");
        loc_uDrawY = glGetUniformLocation(shaderProgramGoL, "u_drawY");
        loc_uAddCells = glGetUniformLocation(shaderProgramGoL, "u_addCells");
        loc_uPause = glGetUniformLocation(shaderProgramGoL, "u_pause");
        loc_uBrushSize = glGetUniformLocation(shaderProgramGoL, "u_brushSize");
        loc_uRuleSet = glGetUniformLocation(shaderProgramGoL, "u_ruleSet");
        loc_uClearAll = glGetUniformLocation(shaderProgramGoL, "u_clearAll");
        loc_uColorMode = glGetUniformLocation(shaderProgram2DPostProcess, "u_colorMode");
        loc_uTime = glGetUniformLocation(shaderProgram2DPostProcess, "u_time");


        loadInitTexture(textureToUse);

        texture.bind(shaderProgramGoL,"initTexture",0);

        viewer = new OGLTexture2D.Viewer();

        glPolygonMode(GL_FRONT_AND_BACK, GL_TRIANGLES);

        initControls();
        initHelp();
    }

    private void initHelp() {
        helpFrame = new JFrame("PGRF3 - Game of Life controls");
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout());
        JLabel titleLabel = new JLabel("Game of Life controls:");
        titleLabel.setFont(new Font("Sans-serif", Font.PLAIN, 23));

        JTextArea controlsArea = new JTextArea(
                "Welcome to the Game of Life PGRF3 project. The program has two main modes, edit and display, each with different controls. You can display this help screen at any time using H \n" +
                        "\n \n GLOBAL CONTROLS: \n \n" +
                        "SHIFT - Change from edit mode to observe mode \n" +
                        "CTRL - Clear the Game area \n" +
                        "SPACE - Pause the simulation \n" +
                        "ALT - Display the buffer viewer \n" +
                        "LEFT/RIGHT - Change Game of Life ruleset \n" +
                        "KEYPAD PLUS/MINUS - Change color mode \n" +
                        "KEYPAD 1-5 - Select from 5 life presets \n" +
                        "KEYPAD 0 - Toggle edge wrap \n" +
                        "H - Display help \n" +

                        "\n \n EDIT MODE: \n \n" +
                        "LEFT CLICK - draw new cells on the screen \n" +
                        "UP/DOWN - Change the size of the brush \n \n" +

                        "\n \n OBSERVE MODE: \n \n" +
                        "WASD - Move camera \n" +
                        "LEFT CLICK - Turn camera \n" +
                        "KEYPAD MUL/DIV - Change 3D object \n"


                ,6,50);

        controlsArea.setFont(new Font("Sans-serif", Font.PLAIN, 14));
        controlsArea.setLineWrap(true);
        controlsArea.setWrapStyleWord(true);
        controlsArea.setOpaque(false);
        controlsArea.setEditable(false);


        panel.add(titleLabel);
        panel.add(controlsArea);
        helpFrame.add(panel);
        helpFrame.setSize(600, 700);
        helpFrame.setLocationRelativeTo(null);
        helpFrame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        helpFrame.setVisible(true);
    }


    /**
     * Main draw loop with optional buffer viewer
     */
    public void draw() {


        // If the size has changed, new Render targets need to be created
        updateRTSize();
        // Simulate one step
        drawStepWorker();
        // Save step for next step and for display
        drawFrontBuffer();
        // Do some post processing
        doPostProcess();
        // Display step as 2D texture or on a 3D object
        drawToScreen();

        // If GoL was cleared, stop clearing
        clearAll = false;
        loadingPass = false;

        time += 0.1;

        // Viewer to see the buffers
        if (displayViewer) {
            viewer.view(renderTargetGoLWorker.getColorTexture(),-1,-1,0.5);
            viewer.view(renderTargetGoLDisplay.getColorTexture(),-1,-0.5,0.5);
            viewer.view(renderTargetGoLPostProcess.getColorTexture(),-1,0,0.5);
        }

    }

    /**
     * An unfortunate solution to some part of lwjgl utils turning interpolation back on
     */
    public void doNotInterpolate() {
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        if (useRepeat) {
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        }
        else {
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP);
        }

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

        glUniform1i(loc_uHeightWorker, GoLsize);
        glUniform1i(loc_uWidthWorker, GoLsize);
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

        renderTargetGoLWorker.getColorTexture().bind(shaderProgram2DDisplayHelper,"toDisplayTexture",0);

        renderTargetGoLDisplay.bind();

        glUseProgram(shaderProgram2DDisplayHelper);

        fullScreenGrid.draw(shaderProgram2DDisplayHelper);

    }

    public void doPostProcess() {
        doNotInterpolate();

        renderTargetGoLDisplay.getColorTexture().bind(shaderProgram2DPostProcess,"toPostProcTexture",0);

        renderTargetGoLPostProcess.bind();

        glUseProgram(shaderProgram2DPostProcess);

        glUniform1i(loc_uHeightPost, GoLsize);
        glUniform1i(loc_uWidthPost, GoLsize);
        glUniform1i(loc_uColorMode, colorMode);
        glUniform1f(loc_uTime,time);

        fullScreenGrid.draw(shaderProgram2DPostProcess);
    }

    /**
     * Draw result to screen or on a 3D object
     */
    public void drawToScreen() {
        doNotInterpolate();

        // Reset viewport that was adjusted in RenderTarget.bind
        glViewport(0, 0, width, height);

        renderTargetGoLPostProcess.getColorTexture().bind(shaderProgram2DDisplayHelper, "fromPostProcTexture",0);

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

            glUseProgram(shaderProgram2DDisplayHelper);

            fullScreenGrid.draw(shaderProgram2DDisplayHelper);
        }
    }

    /**
     * Checks if user changed the buffer size and creates new ones
     */
    private void updateRTSize() {
        if (renderTargetGoLWorker.getHeight() != GoLsize) {
            renderTargetGoLWorker = new OGLRenderTarget(GoLsize,GoLsize);
            renderTargetGoLDisplay = new OGLRenderTarget(GoLsize,GoLsize);
            renderTargetGoLPostProcess = new OGLRenderTarget(GoLsize,GoLsize);
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
                    case GLFW_KEY_UP:
                        brushSize++;
                        break;
                    case GLFW_KEY_DOWN:
                        if (brushSize > 1) {
                            brushSize--;
                        }
                        break;
                    case GLFW_KEY_RIGHT:
                        if (ruleSet < 5){
                            ruleSet++;
                        }
                        break;
                    case GLFW_KEY_LEFT:
                        if (ruleSet > 0){
                            ruleSet--;
                        }
                        break;
                    case GLFW_KEY_KP_ADD:
                        if (colorMode < 3) {
                            colorMode++;
                        }
                        break;
                    case GLFW_KEY_KP_SUBTRACT:
                        if (colorMode > 0) {
                            colorMode--;
                        }
                        break;
                    case GLFW_KEY_KP_DIVIDE:
                        if (bodyID < 2) {
                            bodyID++;
                        }
                        break;
                    case GLFW_KEY_KP_MULTIPLY:
                        if (bodyID > 0) {
                            bodyID--;
                        }
                        break;
                    case GLFW_KEY_KP_0:
                        useRepeat = !useRepeat;
                        break;
                    case GLFW_KEY_KP_1:
                        GoLsize = 100;
                        updateRTSize();
                        loadInitTexture("GoLInits/100GliderGun.png");
                        loadingPass = true;
                        useRepeat = false;
                        ruleSet = 0;
                        break;
                    case GLFW_KEY_KP_2:
                        GoLsize = 500;
                        updateRTSize();
                        loadInitTexture("GoLInits/500Snails.png");
                        loadingPass = true;
                        useRepeat = true;
                        ruleSet = 0;
                        break;
                    case GLFW_KEY_KP_3:
                        GoLsize = 800;
                        updateRTSize();
                        loadInitTexture("GoLInits/800SpaceshipGun.png");
                        loadingPass = true;
                        useRepeat = true;
                        ruleSet = 0;
                        break;
                    case GLFW_KEY_KP_4:
                        GoLsize = 1000;
                        updateRTSize();
                        loadInitTexture("GoLInits/1000LargeSpaceshipGun.png");
                        loadingPass = true;
                        useRepeat = false;
                        ruleSet = 0;
                        break;
                    case GLFW_KEY_KP_5:
                        GoLsize = 6154;
                        updateRTSize();
                        loadInitTexture("GoLInits/6154spinSwitch.png");
                        loadingPass = true;
                        useRepeat = true;
                        ruleSet = 0;
                        break;
                    case GLFW_KEY_H:
                        helpFrame.setVisible(true);
                        break;
                }

            }
        });
    }

    public void loadInitTexture(String textureToUse) {

        try {
            texture = new OGLTexture2D(textureToUse);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        texture.bind(shaderProgramGoL,"initTexture",0);
    }

    public void updateSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public void closeHelp() {
        helpFrame.dispose();
    }
}

