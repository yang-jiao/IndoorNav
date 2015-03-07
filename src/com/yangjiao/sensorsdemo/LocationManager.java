package com.yangjiao.sensorsdemo;


import java.util.Collections;
import java.util.LinkedList;

import android.hardware.SensorEvent;
import android.hardware.SensorManager;

import com.yangjiao.sensorsdemo.util.MathUtil;


public class LocationManager {
	private static LocationManager instance = null;

	private float[] mAccelerometerValues = new float[3];
	private float[] mLinearAccelerometerValues = new float[3];
	private float[] mMagneticFieldValues = new float[3];
	float[] mAccOrientation = new float[3];
	private LinkedList<Long> mDiffStampList = new  LinkedList <Long>();
	private LinkedList<Long> mStepDiffStampList = new  LinkedList <Long>();
	private static int mDiffQueueSize = 10;
	private static long mDiffStampThresholdMin = 230*mDiffQueueSize;
	private static long mDiffStampThresholdMax = 500*mDiffQueueSize;
	boolean mIsGyroscopeEnable = false;

	// parameters for step analyse
	private float mLastValues;
	private float mLastDirections;
	private float mLastExtremes[][] = { new float[2], new float[1] };
	private float mLastDiff;
	private int mLastMatch = -1;
	private final static int OFFSET = 480;
	private LocationChangeListener mLocationChangeListener;
	
	private LinkedList<Float> mHeadingList = new LinkedList<Float>();
	
	private LocationManager() {
	}

	public static LocationManager getInstance() {
		if (instance == null) {
			instance = new LocationManager();
		}
		return instance;
	}

	public void registerStepListener(LocationChangeListener listener) {
		mLocationChangeListener = listener;
	}

	public synchronized void unregisterStepListener() {
		mLocationChangeListener = null;
	}

	private synchronized boolean doStepsAnalyse(float[] values, float degree, long timestamp) {
		boolean val = false;
		float scale = -(OFFSET * 0.5f * (1.0f / (SensorManager.MAGNETIC_FIELD_EARTH_MAX)));
		float vSum = 0;
		for (int i = 0; i < 3; i++) {
			final float v = OFFSET * 0.5f + values[i] * scale;
			vSum += v;
		}
		float v = vSum / 3;

		float direction = (v > mLastValues ? 1 : (v < mLastValues ? -1 : 0));
		addHeadingToList(degree);
		if (direction == -mLastDirections) {
			// Direction changed
			int extType = (direction > 0 ? 0 : 1); // minumum or maximum?
			mLastExtremes[extType][0] = mLastValues;
			float diff = Math.abs(mLastExtremes[extType][0]
					- mLastExtremes[1 - extType][0]);
			
			boolean isValidStep = updateDiff(diff,timestamp);
//			Log.i("doStepsAnalyse","SettingConfig.getSENSOR_THRESHOLD():"+SettingConfig.getSENSOR_THRESHOLD());
			if (diff > SettingConfig.getSENSOR_THRESHOLD() && isValidStep) {

				boolean isAlmostAsLargeAsPrevious = diff > (mLastDiff * 2 / 3);
				boolean isPreviousLargeEnough = mLastDiff > (diff / 3);
				boolean isNotContra = (mLastMatch != 1 - extType);

				if (isAlmostAsLargeAsPrevious && isPreviousLargeEnough
						&& isNotContra) {
					if (mLocationChangeListener != null) {
						mLocationChangeListener.onStep(calculateDegree());
						val = true;
					}
					mLastMatch = extType;
				} else {
					mLastMatch = -1;
				}
			}
			mLastDiff = diff;
		}
		mLastDirections = direction;
		mLastValues = v;
		return val;
	}


	private boolean updateDiff(float diff, long timestamp) {
		if(!SettingConfig.IS_AUTO_SENSITIVITY){
			return true;
		}
		if(diff > SettingConfig.getSENSOR_THRESHOLD()){
			if(mStepDiffStampList.size()<mDiffQueueSize){
		    	  mStepDiffStampList.add(timestamp);
			}else{
				mStepDiffStampList.removeFirst();
				mStepDiffStampList.add(timestamp);
			}
		}

		if(mDiffStampList.size()<mDiffQueueSize){
			mDiffStampList.add(timestamp);
			return true;
		}
		mDiffStampList.removeFirst();
		mDiffStampList.add(timestamp);
		
		
		if(mStepDiffStampList.size()<mDiffQueueSize){
			SettingConfig.downSENSOR_THRESHOLD();
			return true;
		}
		
		long timeDiff = (mDiffStampList.getLast() - mStepDiffStampList.getLast()) / 1000000;
		if (timeDiff*mDiffQueueSize > mDiffStampThresholdMax) {
			SettingConfig.downSENSOR_THRESHOLD();
			return false;
		}
	
		long stepTimeDiff = (mStepDiffStampList.getLast()-mStepDiffStampList.getFirst())/1000000;
        
		if (stepTimeDiff < mDiffStampThresholdMin) {
			SettingConfig.upSENSOR_THRESHOLD();
			return false;
		}
		
		if (stepTimeDiff > mDiffStampThresholdMax) {
			SettingConfig.downSENSOR_THRESHOLD();
			return false;
		}
		return true;
	}

	public void onAccelerometerChanged(SensorEvent event) {
		System.arraycopy(event.values, 0, mAccelerometerValues, 0, 3);
		float[] values = new float[3];
		float[] RR = new float[9];
		// float[] remapR = new float[9];
		float[] magnetics = getMagneticFieldValues();
		float[] accelerometers = getAccelerometerValues();
		float[] kalman_accelerometers = MathUtil.kalmanFilter(accelerometers);
		long timestamp = event.timestamp;
		SensorManager.getRotationMatrix(RR, null, kalman_accelerometers, magnetics);

		// SensorManager.remapCoordinateSystem(RR, SensorManager.AXIS_X,
		// SensorManager.AXIS_Z, remapR);
		SensorManager.getOrientation(RR, values);

		values[0] = (float) Math.toDegrees(values[0]);

		if (!doStepsAnalyse(accelerometers, values[0],timestamp)
				&& mLocationChangeListener != null) {
			mLocationChangeListener.onHeading(values[0]);
		}
	}

	public void onMagneticChanged(float[] magneticValues) {
		System.arraycopy(magneticValues, 0, this.mMagneticFieldValues, 0, 3);
	}

	public void onLinearAccelerometerChanged(float[] linearAccelerometers) {
		System.arraycopy(linearAccelerometers, 0, mLinearAccelerometerValues, 0,
				3);

	}

	public void clear() {
		instance = null;
	}

	public void setGyroscopeEnable(boolean isGyroscopeEnable) {
		this.mIsGyroscopeEnable = isGyroscopeEnable;

	}

	public float[] getAccelerometerValues() {
		return mAccelerometerValues;
	}

	public float[] getLinearAccelerometerValues() {
		return mLinearAccelerometerValues;
	}

	public float[] getMagneticFieldValues() {
		float[] magnetics = new float[3];
		System.arraycopy(mMagneticFieldValues, 0, magnetics, 0, 3);
		return magnetics;
	}
	
	private void addHeadingToList(float heading){
		//TODO is '10' make sense
		if(mHeadingList.size() == 10){
		   mHeadingList.removeFirst();
		}
		mHeadingList.addLast(heading);
	}
	
	private float calculateDegree(){
		float sum = 0f;
		float finalDegree = mHeadingList.getLast();
		float avg = 0f;
		Collections.sort(mHeadingList);
		float delta = Math.abs(mHeadingList.getLast() - mHeadingList.getFirst());
		if(delta < 45f){
			//Normal case
			for(float f:mHeadingList){
				sum = sum +f;
			}
			avg = sum/mHeadingList.size();
		}else if(delta < 180f){
			//use final degree
			avg = finalDegree;
		}else if(delta < 360){
			for(float f:mHeadingList){
				if(Math.signum(f) == -1.0f){
					sum = sum +f + 360;
				}else{
					sum = sum +f;
				}
			}
			avg = sum/mHeadingList.size();
			if(avg > 180f)
				avg = avg - 360;
		}
		
		mHeadingList.clear();
		return avg;
	}
}
