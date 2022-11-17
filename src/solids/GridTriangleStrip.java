package solids;

import static org.lwjgl.opengl.GL11.GL_TRIANGLE_STRIP;

public class GridTriangleStrip extends AbstractRenderable {


    public GridTriangleStrip(int totalRows, int totalCols) {
        super();
        vertexBuffer = new float[totalRows*totalCols*2];
        indexBuffer = new int[2*totalRows*totalCols];
        preferredRenderMode = GL_TRIANGLE_STRIP;
        java.util.Arrays.fill(indexBuffer,totalRows*totalCols-1);

        //Verts
        int index = 0;
        for (int row = 0; row < totalRows; row++) {
            for (int col = 0; col < totalCols; col++) {

                float newX = col / (float) (totalCols-1);
                vertexBuffer[index] = newX;

                float newY = row / (float) (totalRows-1);
                vertexBuffer[index+1] = newY;

                index +=2;
            }
        }

        //Indices
        index = 0;

        //Init
        int newIn = 0;
        indexBuffer[index++] = newIn;

        newIn = totalCols;
        indexBuffer[index++] = newIn;

        boolean forward = true;
        for (int row = 0; row < totalRows-1; row++) {

            int wholeRows = row*totalCols;

            if (forward) {
                for (int col = 0; col < totalCols-1; col++) {

                    newIn = col + 1 + wholeRows;
                    indexBuffer[index++] = newIn;

                    newIn = col + totalCols + 1 + wholeRows;
                    indexBuffer[index++] = newIn;

                }
            }
            else {
                for (int col = totalCols-1; col > 0; col--) {

                    newIn = col + totalCols - 1 + wholeRows;
                    indexBuffer[index++] = newIn;

                    newIn = col - 1 + wholeRows;
                    indexBuffer[index++] = newIn;

                }
            }

            // Magic row operation
            if (row < totalRows-2) {
                if (forward) {
                    newIn = totalCols*2 + wholeRows - 1;
                    indexBuffer[index++] = newIn;

                    newIn = totalCols*2 + wholeRows - 1;
                    indexBuffer[index++] = newIn;

                    newIn = totalCols*3 + wholeRows - 1;
                    indexBuffer[index++] = newIn;

                    newIn = totalCols*2 + wholeRows - 1;
                    indexBuffer[index++] = newIn;

                }

                else {

                    newIn = wholeRows+totalCols;
                    indexBuffer[index++] = newIn;

                    newIn = wholeRows+totalCols*2;
                    indexBuffer[index++] = newIn;

                }
            }


            forward = !forward;
        }
        initBuffers();

    }

}


