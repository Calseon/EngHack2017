package enghack17.myowninstrument;

/**
 * Created by Matthew on 2/4/2017.
 */

public class DataFilter {

    public float[] highpass(float alpha, float[] in)
    {
        float[] out = new float[in.length];
        out[0] = 0;
        for(int i = 1; i < in.length; i++)
        {
            out[i] = alpha * out[i-1] + alpha * (in[i] - in[i-1]);
        }
        return out;
    }

    public float[] lowpass(float alpha, float[] in)
    {

        float[] out = new float[in.length];
        out[0] = 0;
        for(int i = 1; i < in.length; i++)
        {
            out[i] = alpha * in[i] + (1-alpha) * out[i-1];
        }
        return out;
    }
}
