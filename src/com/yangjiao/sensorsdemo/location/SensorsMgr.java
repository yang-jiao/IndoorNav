package com.yangjiao.sensorsdemo.location;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import com.yangjiao.sensorsdemo.IndoorNavigationActivity;
import com.yangjiao.sensorsdemo.UIListener;

public class SensorsMgr implements SensorEventListener{
	private BaseEventListener mEventListener;
    private SensorManager mSmgr; 
    private Sensor mAccSensor; 
    private Sensor mMagSensor; 
    private Sensor mGyroscopeSensor;
    private Sensor mLinerAcceSensor;
    private UIListener mUIListener;	
	
    public void onPause(){
    	if(mEventListener != null){
    		mEventListener.pause();    
    	}
    	unregisterListener();

    }
 
    public void changeListenerRate(final int rate){
    	new Timer().schedule(new TimerTask() {
			@Override
			public void run() {
				unregisterListener();
				initListener(rate);
			}
		}, 300);
    }

	private void unregisterListener() {
		Log.i(IndoorNavigationActivity.TAG,"[unregisterListener]unregisterListener");
		if(mSmgr != null)
			mSmgr.unregisterListener(SensorsMgr.this);
		mEventListener = null;
	}

    public void onResume(){
    	initListener(SensorManager.SENSOR_DELAY_NORMAL);
    	mEventListener.resume();
    }
    
    public void finish(){
    	if(mEventListener != null){
    		mEventListener.finish();
			mEventListener = null;
    	}
    }
    
	public SensorsMgr(UIListener activity) {
		this.mUIListener = activity;
	}
	
	public void initListener(int rate) {
		mSmgr = (SensorManager) mUIListener.getSystemService(Context.SENSOR_SERVICE);
		
		mAccSensor = mSmgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		mMagSensor = mSmgr.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		
		mLinerAcceSensor = mSmgr.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
		
		mSmgr.registerListener(this, mLinerAcceSensor,rate);
		mSmgr.registerListener(this, mAccSensor,rate);
		mSmgr.registerListener(this, mMagSensor,rate);
		
		List <Sensor> gyroscopeSensors = mSmgr.getSensorList(Sensor.TYPE_GYROSCOPE);
		if(gyroscopeSensors.size()>=1){
			mGyroscopeSensor = mSmgr.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
			mSmgr.registerListener(this, mGyroscopeSensor,rate);
			GyroscopeHandler gyroscopeHandler = new GyroscopeHandler(mUIListener);
			registerEventListener(gyroscopeHandler);
		}else{
			LocationHandler locationHandler = new LocationHandler(mUIListener);
			registerEventListener(locationHandler);
		}		
	}
	
	private boolean registerEventListener(BaseEventListener listener){
		mEventListener = listener;
		return true;		
	}
	
	@Override
	public void onSensorChanged(final SensorEvent sensorEvent) {
		switch (sensorEvent.sensor.getType()) {
		case Sensor.TYPE_MAGNETIC_FIELD:
			calculateMagnetic(sensorEvent);
			break;
		case Sensor.TYPE_ACCELEROMETER:
			calculateAccelerometer(sensorEvent);
			break;
		case Sensor.TYPE_GRAVITY:
			calculateGravity(sensorEvent);
			break;
		case Sensor.TYPE_GYROSCOPE:
			calculateGyroscope(sensorEvent);
			break;
		case Sensor.TYPE_LINEAR_ACCELERATION:
			calculateLinearAccelerometer(sensorEvent);
			break;
		}
	}
	
	private void calculateLinearAccelerometer(SensorEvent sensorEvent) {
		if(mEventListener != null){
			float[] linearAccelerometers  = new float[3];
			System.arraycopy(sensorEvent.values, 0, linearAccelerometers, 0, 3);
			mEventListener.onLinearAccelerometerChanged(linearAccelerometers);
		}
	}
	
	private void calculateGravity(SensorEvent sensorEvent) {
		if(mEventListener != null)
			mEventListener.onGravityChanged(sensorEvent);
	}
	
	private void calculateAccelerometer(SensorEvent sensorEvent) {
		if(mEventListener != null)
			mEventListener.onAccelerometerChanged(sensorEvent);
	}
	
	private void calculateMagnetic(SensorEvent sensorEvent) {
		if(mEventListener != null)
			mEventListener.onMagneticChanged(sensorEvent);		
	}
	
	private void calculateGyroscope(SensorEvent sensorEvent) {
		if(mEventListener != null)
			mEventListener.onGyroscopeChanged(sensorEvent);
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}
	
}
