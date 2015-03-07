package com.yangjiao.sensorsdemo.location;

import android.hardware.SensorEvent;

public interface BaseEventListener{

	public void onAccelerometerChanged(SensorEvent event);
	public void onMagneticChanged(SensorEvent event);
	public void onGyroscopeChanged(SensorEvent event);
	public void onGravityChanged(SensorEvent event);
	public void onLinearAccelerometerChanged(float[] linearAccelerometers);
	public void finish();
	public void resume();
	public void pause();
	
}
