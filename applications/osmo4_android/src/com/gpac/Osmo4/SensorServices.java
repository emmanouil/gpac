package com.gpac.Osmo4;

import java.lang.Math;

import android.content.Context;
import android.util.Log;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * Class for 360video sensors management
 *
 * @author Emmanouil Potetsianakis <emmanouil.potetsianakis@telecom-paristech.fr>
 * @version $Revision$
 * 
 */
public class SensorServices implements SensorEventListener, GPACInstanceInterface {

	private static SensorManager sensorManager;

	private static Sensor accelerometer;
	private static Sensor magnetometer;

    protected  Osmo4Renderer rend;

	 float[] lastAcc = {0.0f, 0.0f, 0.0f}, prevAcc;
	 float[] lastMagn = {0.0f, 0.0f, 0.0f}, prevMagn;
	 float[] lastOr = {0.0f, 0.0f, 0.0f}, prevOr;

    private float rotation[] = new float[9];
    private float identity[] = new float[9];


    private static final String LOG_TAG = "GPAC SensorServices";
    private static final String LOG_TAG_C4 = "GPAC SensorServices C4";

    //the lower the value, the more smoothing is applied (lower response) - set to 1.0 for no filter
    private static final float filterLevel = 0.05f;

    /**
     * Constructor (initialize sensors)
     * 
     * @param context The parent Context
     * @return SensorServices object
     *
     */
    public SensorServices(Context context){
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    public void setRenderer(Osmo4Renderer renderer){
            rend = renderer;
}
    /**
     * Register sensors to start receiving data
     * 
     * @return SensorServices object
     *
     */
    public void registerSensors(){
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
    }

    public void unregisterSensors(){
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        switch(event.sensor.getType()){
            case Sensor.TYPE_ACCELEROMETER:
                prevAcc = event.values;

                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                prevMagn = event.values;

                break;
            default:
                return;
        }

        boolean gotRotation = false;

        try {
            gotRotation = SensorManager.getRotationMatrix(rotation, identity, prevAcc, prevMagn);
        } catch (Exception e) {
            gotRotation = false;
            Log.e(LOG_TAG, "Error getting rotation and identity matrices"+ e.getMessage());
        }

        if(gotRotation){

            float orientation[] = new float[3];
            SensorManager.getOrientation(rotation, orientation);
            Log.v(LOG_TAG, "We have orientation: "+orientation[0]+" ,  "+orientation[1]+" ,  "+orientation[2]);
		lastOr = orientation;
		prevOr = lowPass(lastOr, prevOr);

		if (prevOr != null && lastOr != null){
		Log.d(LOG_TAG_C4, "brut : prevOr[0] = " + prevOr[0] + " _ prevOr[1] = " + prevOr[1] + " _ prevOr[2] = " + prevOr[2]);}
		Log.d(LOG_TAG_C4, "________________________________________________________________________________________________________________");

            //NOTE: we invert yaw and roll (for 360 navigation)
            rend.getInstance().onOrientationChange(- prevOr[0], prevOr[1], - prevOr[2]);

        }

    }

    private static float[] smoothSensorMeasurement(float[] in, float[] out){
        
        if(out==null) return in;

        for(int i=0; i<in.length; i++){
            out[i] = out[i] + filterLevel * (in[i] - out[i]);
        }

        return out;
    }

	private float[] lowPass(float[] input, float[] output) {
		if (output == null){ 
			//Log.d(LOG_TAG_C4, "lowpass : output == null");
			return input;
			}
		float[] out = {0.0f, 0.0f, 0.0f};
		for (int i = 0; i < input.length; i++) {
			if (i==0){
				if (input[i]*output[i]<0 && (input[i]>2.8f || input[i]<-0.5f) )
					//out[i] = (-1) * (output[i] + filterLevel * ((-1) * input[i] - output[i]));
					out[i] = input[i];
				else
					out[i] = output[i] + filterLevel * (input[i] - output[i]);
			}
			else 
				out[i] = output[i] + filterLevel * (input[i] - output[i]);
			}
		//Log.d(LOG_TAG_C4, "input.length = " + input.length);
		//Log.d(LOG_TAG_C4, "lowPass : out[0] = " + out[0] + " _ out[1] = " + out[1] + " _ out[2] = " + out[2] );

		return out;
	}


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    	//required - but not used
    }

    @Override
    public native void setGpacLogs(String tools_at_levels);

    @Override
    public native void setGpacPreference(String category, String name, String value);

    @Override
    public void destroy(){}

    @Override
    public void connect(String pop){}

    @Override
    public void disconnect(){}
}
