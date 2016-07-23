package xyz.peast.beep.objects;

import xyz.peast.beep.Constants;
import xyz.peast.beep.VertexArray;

/**
 * Created by duvernea on 7/22/16.
 */
public class Table {
    private static final int POSITION_COMPONENT_COUNT = 2;
    private static final int TEXTURE_COORDINATES_COMPONENT_COUNT = 2;
    private static final int STRIDE = (POSITION_COMPONENT_COUNT + TEXTURE_COORDINATES_COMPONENT_COUNT) * Constants.BYTES_PER_FLOAT;

    private static final float[] VERTEX_DATA = {
            // Order of coordinates: X, Y, S, T

            // Triangle Fan
            0f, 0f, .5f, .5f,
            -.5f, -.8f, 0f, .9f,
            .5f, -.8f, 1f, .9f,
            .5f, .8f, 1f, .1f,
            -.5f, .8f, 0f, .1f,
            -.5f, -.8f, 0f, 0.9f
    };
    private final VertexArray vertexArray;

    public Table() {
        vertexArray = new VertexArray(VERTEX_DATA);
    }
}
