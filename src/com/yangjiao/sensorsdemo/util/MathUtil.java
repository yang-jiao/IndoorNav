package com.yangjiao.sensorsdemo.util;

import jama.Matrix;

import java.math.BigDecimal;

import jkalman.JKalman;

public class MathUtil {
	public static final float EPSILON = 0.000000001f;
	 public static double divide(double v1,double v2, int scale)

	    {

	        return divide(v1, v2, scale, BigDecimal.ROUND_HALF_EVEN);

	    }
	    public static double divide(double v1,double v2,int scale, int round_mode){

	        if(scale < 0)

	        {

	            throw new IllegalArgumentException("The scale must be a positive integer or zero");

	        }

	        BigDecimal b1 = new BigDecimal(Double.toString(v1));

	        BigDecimal b2 = new BigDecimal(Double.toString(v2));

	        return b1.divide(b2, scale, round_mode).doubleValue();
	        
	    }
	    
	    public static  float[] generateNewMatrix(float[] A) {
	    	float[] result = new float[9];
	    	result[0] = A[0];
	    	result[1] = 0.0f;
	    	result[2] = 0.0f;
	    	result[3] = A[1];
	    	result[4] = 0.0f;
	    	result[5] = 0.0f;
	    	result[6] = A[2];
	    	result[7] = 0.0f;
	    	result[8] = 0.0f;
	    	
	    	return result; 
	    	
	    }
	    public static  float[] matrixMultiplication(float[] A, float[] B) {
			float[] result = new float[9];

			result[0] = A[0] * B[0] + A[1] * B[3] + A[2] * B[6];
			result[1] = A[0] * B[1] + A[1] * B[4] + A[2] * B[7];
			result[2] = A[0] * B[2] + A[1] * B[5] + A[2] * B[8];

			result[3] = A[3] * B[0] + A[4] * B[3] + A[5] * B[6];
			result[4] = A[3] * B[1] + A[4] * B[4] + A[5] * B[7];
			result[5] = A[3] * B[2] + A[4] * B[5] + A[5] * B[8];

			result[6] = A[6] * B[0] + A[7] * B[3] + A[8] * B[6];
			result[7] = A[6] * B[1] + A[7] * B[4] + A[8] * B[7];
			result[8] = A[6] * B[2] + A[7] * B[5] + A[8] * B[8];

			return result;
		}
	    

	    public static  float[] getRotationMatrixFromOrientation(float[] o) {
	        float[] xM = new float[9];
	        float[] yM = new float[9];
	        float[] zM = new float[9];
	     
	        float sinX = (float)Math.sin(o[1]);
	        float cosX = (float)Math.cos(o[1]);
	        float sinY = (float)Math.sin(o[2]);
	        float cosY = (float)Math.cos(o[2]);
	        float sinZ = (float)Math.sin(o[0]);
	        float cosZ = (float)Math.cos(o[0]);
	     
	        // rotation about x-axis (pitch)
	        xM[0] = 1.0f; xM[1] = 0.0f; xM[2] = 0.0f;
	        xM[3] = 0.0f; xM[4] = cosX; xM[5] = sinX;
	        xM[6] = 0.0f; xM[7] = -sinX; xM[8] = cosX;
	     
	        // rotation about y-axis (roll)
	        yM[0] = cosY; yM[1] = 0.0f; yM[2] = sinY;
	        yM[3] = 0.0f; yM[4] = 1.0f; yM[5] = 0.0f;
	        yM[6] = -sinY; yM[7] = 0.0f; yM[8] = cosY;
	     
	        // rotation about z-axis (azimuth)
	        zM[0] = cosZ; zM[1] = sinZ; zM[2] = 0.0f;
	        zM[3] = -sinZ; zM[4] = cosZ; zM[5] = 0.0f;
	        zM[6] = 0.0f; zM[7] = 0.0f; zM[8] = 1.0f;
	     
	        // rotation order is y, x, z (roll, pitch, azimuth)
	        float[] resultMatrix = MathUtil.matrixMultiplication(xM, yM);
	        resultMatrix = MathUtil.matrixMultiplication(zM, resultMatrix);
	        return resultMatrix;
	    }
	    
	public static float[] matrixVectorMultiplication(float[] m, float[] v) {
		float[] result = new float[3];
		result[0] = m[0] * v[0] + m[1] * v[1] + m[2] * v[2];
		result[1] = m[3] * v[0] + m[4] * v[1] + m[5] * v[2];
		result[2] = m[6] * v[0] + m[7] * v[1] + m[8] * v[2];

		return result;
	}
		
	// This function is borrowed from the Android reference
	// at http://developer.android.com/reference/android/hardware/SensorEvent.html#values
	// It calculates a rotation vector from the gyroscope angular speed values.
	public static void getRotationVectorFromGyro(float[] gyroValues,
            float[] deltaRotationVector,
            float timeFactor)
	{
		float[] normValues = new float[3];
		
		// Calculate the angular speed of the sample
		float omegaMagnitude =
		(float)Math.sqrt(gyroValues[0] * gyroValues[0] +
		gyroValues[1] * gyroValues[1] +
		gyroValues[2] * gyroValues[2]);
		
		// Normalize the rotation vector if it's big enough to get the axis
		if(omegaMagnitude > EPSILON) {
		normValues[0] = gyroValues[0] / omegaMagnitude;
		normValues[1] = gyroValues[1] / omegaMagnitude;
		normValues[2] = gyroValues[2] / omegaMagnitude;
		}
		
		// Integrate around this axis with the angular speed by the timestep
		// in order to get a delta rotation from this sample over the timestep
		// We will convert this axis-angle representation of the delta rotation
		// into a quaternion before turning it into the rotation matrix.
		float thetaOverTwo = omegaMagnitude * timeFactor;
		float sinThetaOverTwo = (float)Math.sin(thetaOverTwo);
		float cosThetaOverTwo = (float)Math.cos(thetaOverTwo);
		deltaRotationVector[0] = sinThetaOverTwo * normValues[0];
		deltaRotationVector[1] = sinThetaOverTwo * normValues[1];
		deltaRotationVector[2] = sinThetaOverTwo * normValues[2];
		deltaRotationVector[3] = cosThetaOverTwo;
	}
    private static JKalman kalman = null;
	
	private static void initKalman(){
    	try {
			kalman = new JKalman(6, 3);
			double[][] tr = { {1,0,0,1,0,0},
 		           {0,1,0,0,1,0},
 		           {0,0,1,0,0,1},
 		           {0,0,0,1,0,0},
 		           {0,0,0,0,1,0},
 		           {0,0,0,0,0,1}};
			 kalman.setTransition_matrix(new Matrix(tr));
			 kalman.setError_cov_post(kalman.getError_cov_post().identity());
			 
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
	private static Matrix m = new Matrix(3, 1); // measurement [x]
//	private static Matrix s;
	private static Matrix c;
	
	public static float[]  kalmanFilter(float[] v){
		if(kalman == null){
			initKalman();
		}
		float[] _val = new float[3];
		kalman.Predict();
		m.set(0, 0, v[0]);
		m.set(1, 0, v[1]);
		m.set(2, 0, v[2]);
		c = kalman.Correct(m);
		_val[0] = (float)c.get(0, 0);
		_val[1] = (float)c.get(1, 0);
		_val[2] = (float)c.get(2, 0);
		return _val;
	}
	
	public static float getDirectionByAccelerator(float x,float y){
		double direction = 0d;
		if((x>0 || x<0) && y >0){
			direction = Math.atan(x/y);
		}else if(x<0 && y <0){
			direction = (0d - Math.atan(x/y))- Math.PI/2;
		}else if(x>0 && y< 0){
			direction = Math.atan(x/y)+ Math.PI;
		}
		return (float)Math.toDegrees(direction);
	}
	
	
	
	
}
