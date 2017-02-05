package enghack17.myowninstrument;

/**
 * Created by Matthew on 2/4/2017.
 */

public class DataFilter {

    static final float ALPHA = 0.25f;

    public float[] lowPass( float[] input, float[] output ) {
        if (output == null) return input;

        for (int i=0; i<input.length; i++) {
            output[i] = output[i] + ALPHA * (input[i] - output[i]);
        }
        return output;
    }
}
