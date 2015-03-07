package com.yangjiao.sensorsdemo;


public interface UIListener extends LocationChangeListener {

	public void runOnUiThread(Runnable action);

	public Object getSystemService(String str);

}
