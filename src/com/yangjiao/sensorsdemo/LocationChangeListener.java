package com.yangjiao.sensorsdemo;

public interface LocationChangeListener {
	
	public void onStep(float degree);
	
	public void onHeading(float degree);

}
