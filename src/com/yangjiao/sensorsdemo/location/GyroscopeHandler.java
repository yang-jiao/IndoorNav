package com.yangjiao.sensorsdemo.location;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.util.Log;

import com.yangjiao.sensorsdemo.LocationChangeListener;
import com.yangjiao.sensorsdemo.LocationManager;
import com.yangjiao.sensorsdemo.IndoorNavigationActivity;
import com.yangjiao.sensorsdemo.UIListener;
import com.yangjiao.sensorsdemo.util.MathUtil;

public class GyroscopeHandler implements BaseEventListener, LocationChangeListener{

	private UIListener mUIListener;
	private float[] mAccelerometerValues = new float[3]; 

	private float[] mMagneticFieldValues = new float[3]; 
	
	private float[] mLinearAccelerometerValues = new float[3]; 
    
//	private float[] gravityFieldValues = new float[3]; 

	// accelerometer and magnetometer based rotation matrix
	private float[] mRotationMatrix = new float[9];
	
	// rotation matrix from gyro data
	private float[] mGyroMatrix = new float[9];
	
	// final orientation angles from sensor fusion
	private float[] mFusedOrientation = new float[3];
	// orientation angles from accel and magnet
	private float[] mAccMagOrientation = new float[3];
	// orientation angles from gyro matrix
	private float[] mGyroOrientation = new float[3];
	
	private boolean mInitState = true;
	private float mTimestamp;
	private static final float NS2S = 1.0f / 1000000000.0f;
	// angular speeds from gyro
	private float[] mGyro = new float[3];
	
	public static final int TIME_CONSTANT = 300;
	public static final float FILTER_COEFFICIENT = 0.98f;
	private LocationManager mLocationManager;
	
	DecimalFormat d = new DecimalFormat("#.##");
	
	private float mLastHeading = 0;
	
	private Timer mTimer;
	
	private static final Executor exec = Executors.newFixedThreadPool(1);


	
	public GyroscopeHandler(UIListener activity) {
		this.mUIListener = activity;
		mLocationManager = LocationManager.getInstance();
		mLocationManager.setGyroscopeEnable(true);
		init();
		
	}

	private void init(){
		 mGyroOrientation[0] = 0.0f;
	     mGyroOrientation[1] = 0.0f;
	     mGyroOrientation[2] = 0.0f;
	 
	        // initialise gyroMatrix with identity matrix
	     mGyroMatrix[0] = 1.0f; mGyroMatrix[1] = 0.0f; mGyroMatrix[2] = 0.0f;
	     mGyroMatrix[3] = 0.0f; mGyroMatrix[4] = 1.0f; mGyroMatrix[5] = 0.0f;
	     mGyroMatrix[6] = 0.0f; mGyroMatrix[7] = 0.0f; mGyroMatrix[8] = 1.0f;
	     
	     d.setRoundingMode(RoundingMode.HALF_UP);
	     d.setMaximumFractionDigits(3);
	     d.setMinimumFractionDigits(3);
	}
	
	@Override
	public void onAccelerometerChanged(final SensorEvent event) {		
		exec.execute(new Runnable(){

			@Override
			public void run() {
				System.arraycopy(event.values, 0, mAccelerometerValues, 0, 3);
				mAccelerometerValues = MathUtil.kalmanFilter(mAccelerometerValues);		
				calculateAccMagOrientation();
				mLocationManager.onAccelerometerChanged(event);				
			}});
	}

	@Override
	public void onMagneticChanged(final SensorEvent event) {
		exec.execute(new Runnable(){
			@Override
			public void run() {
				System.arraycopy(event.values, 0, mMagneticFieldValues, 0, 3);
				mLocationManager.onMagneticChanged(event.values);				
			}});			
	}
	
	@Override
	public void onGyroscopeChanged(final SensorEvent event) {
		exec.execute(new Runnable(){
			@Override
			public void run() {
				gyroFunction(event);				
			}});	
		// process gyro data			
	}
	
	@Override
	public void onGravityChanged(SensorEvent event) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void onLinearAccelerometerChanged(float[] linearAccelerometers) {
//		System.arraycopy(event.values, 0, linearAccelerometerValues, 0, 3);
		mLinearAccelerometerValues = linearAccelerometers;
		mLocationManager.onLinearAccelerometerChanged(mLinearAccelerometerValues);		
	}
	
	private  boolean  accMagProcessed = false;
	private void calculateAccMagOrientation() {
		if (SensorManager
				.getRotationMatrix(mRotationMatrix, null, mAccelerometerValues, mMagneticFieldValues)) {
			SensorManager.getOrientation(mRotationMatrix, mAccMagOrientation);
			accMagProcessed = true;
		}
	}	
	
    // This function performs the integration of the gyroscope data.
    // It writes the gyroscope based orientation into gyroOrientation.
    private void gyroFunction(SensorEvent event) {
        // don't start until first accelerometer/magnetometer orientation has been acquired
        if (!accMagProcessed)
            return;
     
        // initialisation of the gyroscope based rotation matrix
        if(mInitState) {
            float[] initMatrix = new float[9];
            initMatrix = MathUtil.getRotationMatrixFromOrientation(mAccMagOrientation);
//            float[] test = new float[3];
//            SensorManager.getOrientation(initMatrix, test);
            mGyroMatrix = MathUtil.matrixMultiplication(mGyroMatrix, initMatrix);
            mInitState = false;
        }
     
        // copy the new gyro values into the gyro array
        // convert the raw gyro data into a rotation vector
        float[] deltaVector = new float[4];
        if(mTimestamp != 0) {
            final float dT = (event.timestamp - mTimestamp) * NS2S;
            System.arraycopy(event.values, 0, mGyro, 0, 3);
            MathUtil.getRotationVectorFromGyro(mGyro, deltaVector, dT / 2.0f);
        }
     
        // measurement done, save current time for next interval
        mTimestamp = event.timestamp;
     
        // convert rotation vector into rotation matrix
        float[] deltaMatrix = new float[9];
        SensorManager.getRotationMatrixFromVector(deltaMatrix, deltaVector);
     
        // apply the new rotation interval on the gyroscope based rotation matrix
        mGyroMatrix = MathUtil.matrixMultiplication(mGyroMatrix, deltaMatrix);
     
        // get the gyroscope based orientation from the rotation matrix
        SensorManager.getOrientation(mGyroMatrix, mGyroOrientation);
        gryoProcessed = true;
        fuseSensors();
    }                
    
    private boolean gryoProcessed = false; 
    class calculateFusedOrientationTask extends TimerTask {
        public void run() {
        	if(mLocationManager == null){
        		return;
        	}
        	
        	if(!gryoProcessed)
        		return;
        	
            fuseSensors();
        }
    }
    
	@Override
	public void finish() {
		mLocationManager.clear();
		if(mTimer != null){
			mTimer.cancel();
			mTimer = null;
		}		
	}

	@Override
	public void onStep(float degree) {
		mUIListener.runOnUiThread(new Runnable(){

			@Override
			public void run() {
				// TODO Auto-generated method stub
				 ((LocationChangeListener)mUIListener).onStep(mLastHeading);
			}            	
		}) ;
		//((LocationChangeListener)activity).onStep(lastHeading);
	}

	@Override
	public void onHeading(float degree) {
				
	}
	
	public void resume(){
		mLocationManager.registerStepListener(this);
//		if(timer == null){
//			timer = new Timer(); 
//			timer.scheduleAtFixedRate(new calculateFusedOrientationTask(),
//	              500, TIME_CONSTANT);
//		}
	}
	
	public void pause(){				
		mLocationManager.unregisterStepListener();
	}

	private void fuseSensors() {
		float oneMinusCoeff = 1.0f - FILTER_COEFFICIENT;
		
		/*
		 * Fix for 179�<--> -179�transition problem:
		 * Check whether one of the two orientation angles (gyro or accMag) is negative while the other one is positive.
		 * If so, add 360�(2 * math.PI) to the negative value, perform the sensor fusion, and remove the 360�from the result
		 * if it is greater than 180� This stabilizes the output in positive-to-negative-transition cases.
		 */
		
		// azimuth
		if (mGyroOrientation[0] < -0.5 * Math.PI && mAccMagOrientation[0] > 0.0) {
			mFusedOrientation[0] = (float) (FILTER_COEFFICIENT * (mGyroOrientation[0] + 2.0 * Math.PI) + oneMinusCoeff * mAccMagOrientation[0]);
			mFusedOrientation[0] -= (mFusedOrientation[0] > Math.PI) ? 2.0 * Math.PI : 0;
		}
		else if (mAccMagOrientation[0] < -0.5 * Math.PI && mGyroOrientation[0] > 0.0) {
			mFusedOrientation[0] = (float) (FILTER_COEFFICIENT * mGyroOrientation[0] + oneMinusCoeff * (mAccMagOrientation[0] + 2.0 * Math.PI));
			mFusedOrientation[0] -= (mFusedOrientation[0] > Math.PI)? 2.0 * Math.PI : 0;
		}
		else {
			mFusedOrientation[0] = FILTER_COEFFICIENT * mGyroOrientation[0] + oneMinusCoeff * mAccMagOrientation[0];
		}
		
		Log.i(IndoorNavigationActivity.TAG, "gyro" + mGyroOrientation[0] +  ",accMag" + mAccMagOrientation[0] + ",fused:" + mFusedOrientation[0]);
		
		// pitch
		if (mGyroOrientation[1] < -0.5 * Math.PI && mAccMagOrientation[1] > 0.0) {
			mFusedOrientation[1] = (float) (FILTER_COEFFICIENT * (mGyroOrientation[1] + 2.0 * Math.PI) + oneMinusCoeff * mAccMagOrientation[1]);
			mFusedOrientation[1] -= (mFusedOrientation[1] > Math.PI) ? 2.0 * Math.PI : 0;
		}
		else if (mAccMagOrientation[1] < -0.5 * Math.PI && mGyroOrientation[1] > 0.0) {
			mFusedOrientation[1] = (float) (FILTER_COEFFICIENT * mGyroOrientation[1] + oneMinusCoeff * (mAccMagOrientation[1] + 2.0 * Math.PI));
			mFusedOrientation[1] -= (mFusedOrientation[1] > Math.PI)? 2.0 * Math.PI : 0;
		}
		else {
			mFusedOrientation[1] = FILTER_COEFFICIENT * mGyroOrientation[1] + oneMinusCoeff * mAccMagOrientation[1];
		}
		
		// roll
		if (mGyroOrientation[2] < -0.5 * Math.PI && mAccMagOrientation[2] > 0.0) {
			mFusedOrientation[2] = (float) (FILTER_COEFFICIENT * (mGyroOrientation[2] + 2.0 * Math.PI) + oneMinusCoeff * mAccMagOrientation[2]);
			mFusedOrientation[2] -= (mFusedOrientation[2] > Math.PI) ? 2.0 * Math.PI : 0;
		}
		else if (mAccMagOrientation[2] < -0.5 * Math.PI && mGyroOrientation[2] > 0.0) {
			mFusedOrientation[2] = (float) (FILTER_COEFFICIENT * mGyroOrientation[2] + oneMinusCoeff * (mAccMagOrientation[2] + 2.0 * Math.PI));
			mFusedOrientation[2] -= (mFusedOrientation[2] > Math.PI)? 2.0 * Math.PI : 0;
		}
		else {
			mFusedOrientation[2] = FILTER_COEFFICIENT * mGyroOrientation[2] + oneMinusCoeff * mAccMagOrientation[2];
		}
    
		// overwrite gyro matrix and orientation with fused orientation
		// to comensate gyro drift
		mGyroMatrix = MathUtil.getRotationMatrixFromOrientation(mFusedOrientation);
		System.arraycopy(mFusedOrientation, 0, mGyroOrientation, 0, 3);           
		
		mLastHeading = (float) Math.toDegrees(mFusedOrientation[0]);
		mUIListener.runOnUiThread(new Runnable(){

			@Override
			public void run() {
				// TODO Auto-generated method stub
				 ((LocationChangeListener)mUIListener).onHeading(mLastHeading);
			}            	
		}) ;
	}
}
