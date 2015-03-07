package com.yangjiao.sensorsdemo;

import android.content.Context;
import android.content.SharedPreferences.Editor;

public class SettingConfig {
	
	public static boolean DEBUGMODE = true;
	public static boolean IS_AUTO_SENSITIVITY = true;
	public static boolean IS_INDOOR = false;
	
	private static float SENSOR_THRESHOLD = 10.0f; // sensitivity
	private static double STEP_LENGTH = Double.MIN_VALUE;
	
	
	public static void upSENSOR_THRESHOLD() {
		if (SENSOR_THRESHOLD >= 10) {
			return;
		}
		SENSOR_THRESHOLD = SENSOR_THRESHOLD + 1;
	}

	public static void downSENSOR_THRESHOLD() {
		if (SENSOR_THRESHOLD <= 1) {
			return;
		}
		SENSOR_THRESHOLD = SENSOR_THRESHOLD - 1;
	}
	
	public static float getSENSOR_THRESHOLD() {
		return SENSOR_THRESHOLD;
	}
	public static void setSENSOR_THRESHOLD(float sENSOR_THRESHOLD) {
		SENSOR_THRESHOLD = sENSOR_THRESHOLD;
	}
	public static double getSTEP_LENGTH(Context ctx) {
		if(STEP_LENGTH != Double.MIN_VALUE){
			return STEP_LENGTH;
		}
		int val = ctx.getSharedPreferences("pref", Context.MODE_WORLD_WRITEABLE).getInt("step", 60);
		STEP_LENGTH = ((double)val)/100;
		return STEP_LENGTH;
	}
	
	public static void setSTEP_LENGTH(Context ctx,int val){
		STEP_LENGTH = Double.MIN_VALUE;
		Editor editor = ctx.getSharedPreferences("pref", Context.MODE_WORLD_WRITEABLE).edit();
    	editor.putInt("step", val);
    	editor.commit();
	}

}
