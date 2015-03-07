package com.yangjiao.sensorsdemo.util;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.yangjiao.sensorsdemo.SettingConfig;

import android.util.Log;

public class SensorLog {
private static boolean isAllLogEnable = true;
private static boolean isInfoLogEnable = false;
private static boolean isWarnLogEnable = false;
private static boolean isSaveLogEnable = true;
private static SimpleDateFormat sdf = new SimpleDateFormat("dd HH:mm:ss SS");
public static void i(String tag,String message){
	if(isAllLogEnable&&isInfoLogEnable){
		Log.i(tag, message);
	}
}

public static void w(String tag,String message){
	if(isWarnLogEnable&&isAllLogEnable){
		Log.w(tag, message);
	}
}
public static StringBuffer loginfo = new StringBuffer();
public static void saveLog(final Date currentDate,final String message){
	if(!isSaveLogEnable||!SettingConfig.DEBUGMODE){
		return;
	}
	loginfo.append(sdf.format(currentDate)+"--"+message+"\n");
		 
			 
	
}

public static void saveLogTofile(){
	if(!isSaveLogEnable){
		return;
	}
	final String message = loginfo.toString();
		new Thread("onSensorChanged"){
            public void run(){
            	String pathName="/sdcard/sensorlog/";   
    	        String fileName="sensorlog.txt";   
    	        File path = new File(pathName);   
    	        File file = new File(pathName + fileName);   
    	        if( !path.exists()) { 
    	        	try{
    	            Log.d("TestFile", "Create the path:" + pathName);   
    	            path.mkdir();   
    	        	} catch(Exception e){
    	        		Log.d("TestFile", "Create the path failed!");   
    	        		e.printStackTrace();
    	        	}
    	        }   
    	        
    			try {
    				if( !file.exists()) {   
    		            Log.d("TestFile", "Create the file:" + fileName);   
    		            file.createNewFile();
    		        }
    			
    				
    				FileOutputStream stream = new FileOutputStream(file,true);
    				Log.d("TestFile", "Save log message:" + message);   
//    				String messagedetail =sdf.format(currentDate)+"--"+message+"\n";
    		        byte[] buf = message.getBytes();   
    		        stream.write(buf);             
    		        stream.close();   
    			} catch (Exception e) {
    				// TODO Auto-generated catch block
    				e.printStackTrace();
    			} 
            }
			
		}.start();
		 
			 
	
}

public static void removeLogFile(){
	loginfo= new StringBuffer();
	String pathName="/sdcard/sensorlog/";   
    String fileName="sensorlog.txt"; 
//    File path = new File(pathName);   
    File file = new File(pathName + fileName);   
    if( file.exists()) {   
    	file.delete();
    }
}

}
