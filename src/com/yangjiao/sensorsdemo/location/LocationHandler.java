package com.yangjiao.sensorsdemo.location;

import android.hardware.SensorEvent;

import com.yangjiao.sensorsdemo.LocationChangeListener;
import com.yangjiao.sensorsdemo.LocationManager;

public class LocationHandler implements BaseEventListener{

	private LocationManager mLocationManager;
	private LocationChangeListener mListener;
	
	public LocationHandler(LocationChangeListener lListener) {
		mLocationManager = LocationManager.getInstance();
		mListener = lListener;
	}

	public void resume(){
		mLocationManager.registerStepListener(mListener);
	}
	
	public void pause(){
		mLocationManager.unregisterStepListener();
	}
	
	@Override
	public void onAccelerometerChanged(SensorEvent event) {
    	mLocationManager.onAccelerometerChanged(event);
	}

    
	@Override
	public void onMagneticChanged(SensorEvent event) {
		mLocationManager.onMagneticChanged(event.values);
		
	}

	@Override
	public void onGyroscopeChanged(SensorEvent event) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void onGravityChanged(SensorEvent event) {		
	}
	
	@Override
	public void onLinearAccelerometerChanged(float[] linearAccelerometers) {
		mLocationManager.onLinearAccelerometerChanged(linearAccelerometers);
	}

	@Override
	public void finish() {
		mLocationManager.clear();		
	}

}
