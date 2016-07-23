package xyz.peast.beep.gles.objects;

import android.opengl.GLES20;

import xyz.peast.beep.gles.Constants;
import xyz.peast.beep.gles.VertexArray;
import xyz.peast.beep.gles.programs.ColorShaderProgram;

/**
 * Created by duvernea on 7/23/16.
 */
public class Mallet {
    private static final int POSITION_COMPONENT_COUNT = 2;
    private static final int COLOR_COMPONENT_COUNT = 3;
    private static final int STRIDE =
            (POSITION_COMPONENT_COUNT + COLOR_COMPONENT_COUNT) * Constants.BYTES_PER_FLOAT;
    private static final float[] VERTEX_DATA = {
            // Order of coordinates: Xy, Y, R, G, B
            0f, -.4f, 0f, 0f, 1f,
            0f, .4f, 1f, 0f, 0f
    };
    private final VertexArray vertexArray;

    public Mallet() {
        vertexArray = new VertexArray(VERTEX_DATA);
    }
    public void bindData(ColorShaderProgram colorProgram) {
        vertexArray.setVertexAttribPointer(
                0,
                colorProgram.getPositionAttributeLocation(),
                POSITION_COMPONENT_COUNT,
                STRIDE);
        vertexArray.setVertexAttribPointer(
                POSITION_COMPONENT_COUNT,
                colorProgram.getColorAttributeLocation(),
                COLOR_COMPONENT_COUNT,
                STRIDE);
    }
    public void draw() {
        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, 2);
    }

}
