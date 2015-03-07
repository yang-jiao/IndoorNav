package com.yangjiao.sensorsdemo;

import java.util.Date;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.text.InputFilter;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMyLocationButtonClickListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.yangjiao.sensorsdemo.location.SensorsMgr;
import com.yangjiao.sensorsdemo.util.SensorLog;

public class IndoorNavigationActivity extends FragmentActivity implements
		ConnectionCallbacks, OnConnectionFailedListener, LocationListener,
		OnMyLocationButtonClickListener, UIListener {

	public static final String TAG = "IndoorNavDemo";

	private Location mLocation;
	private Handler mHandler;
	private Bundle mBundle = new Bundle();

	private SensorsMgr mSensorListener;

	private final int DIALOG_STEPLENGTH = 0;
	private int mTotalStep = 0;

	private final static int WAITINGGPS = 0;
	private final static int PLEASESTART = 1;

	private boolean mIsTracking = false;
	private Toast mToast;

	private float mDegree = 0f;

	private float meterPerLong;
	private float meterPerLat;

	private GoogleMap mMap;
	private TextView mTextView;
	private LocationClient mLocationClient;

	private Marker mLocationMarker;

	private static final LocationRequest REQUEST = LocationRequest.create()
			.setNumUpdates(1)
			.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

	private Bitmap mBitmap;

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.main_view);

		mBitmap = BitmapFactory.decodeResource(getResources(),
				R.drawable.location_heading);

		mTextView = (TextView) findViewById(R.id.warning_msg);
		mMap = ((SupportMapFragment) getSupportFragmentManager()
				.findFragmentById(R.id.map)).getMap();
		UiSettings uiSettings = mMap.getUiSettings();
		uiSettings.setMyLocationButtonEnabled(false);
		uiSettings.setZoomControlsEnabled(false);
		uiSettings.setAllGesturesEnabled(false);
		mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);

		mHandler = new Handler() {

			@Override
			public void handleMessage(Message msg) {
				if (msg.what == 0) {
					mLocation.set((Location) msg.obj);
					mBundle.putBoolean("mock", true);
					mLocation.setExtras(mBundle);
					onLocationChanged(mLocation);
				} else if (msg.what == 1) {
					float[] array = (float[]) msg.obj;
					float lastDegree = mDegree;
					mDegree = array[0];
					float pixel_x = array[1];
					float pixel_y = array[2];

					Message m = obtainMessage();
					m.what = 0;
					if (pixel_x == 0.0f && pixel_y == 0.0f && Math.abs(lastDegree-mDegree)>30f) {
						sendEmptyMessage(2);
						return;
					} else {
						float deltaLong = pixel_x / meterPerLong;
						float deltaLat = pixel_y / meterPerLat;

						int latitude = (int) (mLocation.getLatitude() * 1E6 + Math
								.round(deltaLat));
						int longitude = (int) (mLocation.getLongitude() * 1E6 + Math
								.round(deltaLong));

						LatLng fixGeo = new LatLng((double) latitude / 1E6,
								(double) longitude / 1E6);
//						Log.e(TAG, "test-[MapViewCompassDemo]:xStepSum:"
//								+ pixel_x + "||" + "yStepSum:" + pixel_y
//								+ "latitude:" + latitude + "||longitude="
//								+ longitude);
						SensorLog.saveLog(new Date(),
								"test-[MapViewCompassDemo]:xStepSum:" + pixel_x
										+ "||" + "yStepSum:" + pixel_y
										+ "latitude:" + latitude
										+ "||longitude=" + longitude);
						m.obj = LatLng2Location(fixGeo);
					}
					sendMessage(m);
				}else if (msg.what == 2) {
//					Log.e("ernest", "xxxxxxxxxxxxxxxx rotate ava");
					onLocationChanged(mLocation);
				}
				super.handleMessage(msg);
			}

		};

		calculateMetersByGPS();

		mLocation = new Location("mock");
		mLocation.setLatitude(999d);
		
		mSensorListener = new SensorsMgr(this);

	}

	public void onPause() {
		super.onPause();
		clearStatus();
		if (mLocationClient != null) {
			mLocationClient.disconnect();
		}

		if (mSensorListener != null) {
			mSensorListener.onPause();
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
		LocationManager.getInstance().unregisterStepListener();
	}

	@Override
	public void finish() {
		if (mSensorListener != null) {
			mSensorListener.finish();
		}
		super.finish();
	}

	@Override
	public boolean onMyLocationButtonClick() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onLocationChanged(Location location) {

		int width = mBitmap.getWidth();
		int height = mBitmap.getHeight();
		Matrix matrix = new Matrix();
		matrix.preRotate(mDegree);
		Bitmap avatar = Bitmap.createBitmap(mBitmap, 0, 0, width, height,
				matrix, true);

		if (mLocationMarker != null) {
			mLocationMarker.remove();
		}

		mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(
				location.getLatitude(), location.getLongitude()), mMap
				.getMaxZoomLevel()));
		mLocationMarker = mMap.addMarker(new MarkerOptions().position(
				new LatLng(location.getLatitude(), location.getLongitude()))
				.icon(BitmapDescriptorFactory.fromBitmap(avatar)));
		if (location.getExtras() == null
				|| !location.getExtras().getBoolean("mock")) {
			setHintText(PLEASESTART);
			Message m = Message.obtain(mHandler);
			m.what = 0;
			m.obj = location;
			mHandler.sendMessage(m);
		}

	}

	@Override
	public void onConnectionFailed(ConnectionResult arg0) {
		Log.e(TAG, arg0.toString());
	}

	@Override
	public void onConnected(Bundle arg0) {
		mLocationClient.requestLocationUpdates(REQUEST, this);
	}

	@Override
	public void onDisconnected() {
		// Do nothing
	}

	@Override
	protected void onResume() {
		super.onResume();
		setUpMapIfNeeded();
		setUpLocationClientIfNeeded();
		mLocationClient.connect();
		setHintText(WAITINGGPS);
		if (mIsTracking && mSensorListener != null) {
			mSensorListener.onResume();
		}
	}

	private void setUpMapIfNeeded() {
		if (mMap == null) {
			mMap = ((SupportMapFragment) getSupportFragmentManager()
					.findFragmentById(R.id.map)).getMap();
		}
		if (mMap != null) {
			mMap.setMyLocationEnabled(false);
			mMap.setOnMyLocationButtonClickListener(this);
		}
	}

	private void setUpLocationClientIfNeeded() {
		if (mLocationClient == null) {
			mLocationClient = new LocationClient(getApplicationContext(), this,
					this);
		}
	}

	private Location LatLng2Location(LatLng point) {
		Location loc = new Location("mock");
		loc.setLatitude(point.latitude);
		loc.setLongitude(point.longitude);
		return loc;
	}

	private void calculateMetersByGPS() {
		Location _l = new Location("");
		_l.setLongitude(-122.084095);
		_l.setLatitude(37.422006);

		Location _ll = new Location("");
		_ll.setLongitude(-122.084096);
		_ll.setLatitude(37.422006);

		Location _lll = new Location("");
		_lll.setLongitude(-122.084095);
		_lll.setLatitude(37.422007);

		meterPerLong = _ll.distanceTo(_l);
		meterPerLat = _lll.distanceTo(_l);

//		Log.e(TAG, "xxxxx meterPerLong=" + meterPerLong + ",meterPerLat="
//				+ meterPerLat);

	}

	private void inputLocationData(float degree, float x, float y) {
		Message m = Message.obtain(mHandler);
		m.what = 1;
		m.obj = new float[] { degree, x, y };
		mHandler.sendMessage(m);

	}

	@Override
	public void onStep(float degree) {
		mTotalStep += 1;
		if (mToast == null) {
			mToast = Toast.makeText(this, "onstep " + mTotalStep,
					Toast.LENGTH_SHORT);
		}
		mToast.setText("onstep " + mTotalStep);
		mToast.show();
		inputLocationData(degree,
				(float) (Math.sin(Math.toRadians(degree)) * SettingConfig
						.getSTEP_LENGTH(this)),
				(float) (Math.cos(Math.toRadians(degree)) * SettingConfig
						.getSTEP_LENGTH(this)));

	}

	@Override
	public void onHeading(float degree) {
		inputLocationData(degree, 0, 0);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the currently selected menu XML resource.
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		if (SettingConfig.IS_INDOOR) {
			menu.findItem(R.id.start_tracking).setVisible(false);
			menu.findItem(R.id.reset_tracking).setVisible(false);
		}
		if (!SettingConfig.DEBUGMODE) {
			menu.findItem(R.id.savelog).setVisible(false);
			// menu.findItem(R.id.stepmode).setVisible(false);
		}

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if (!SettingConfig.IS_INDOOR) {
			if (mIsTracking) {
				menu.findItem(R.id.start_tracking).setEnabled(false);
				menu.findItem(R.id.reset_tracking).setEnabled(true);
			} else {
				menu.findItem(R.id.start_tracking).setEnabled(true);
				menu.findItem(R.id.reset_tracking).setEnabled(false);
			}
			if (mLocation.getLatitude() == 999d) {
				menu.findItem(R.id.start_tracking).setEnabled(false);
			}
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.step_length) {
			showDialog(DIALOG_STEPLENGTH);
		} else if (item.getItemId() == R.id.start_tracking) {
			startTracking();
		} else if (item.getItemId() == R.id.reset_tracking) {
			resetTracking();
		} else if (item.getItemId() == R.id.savelog) {
			SensorLog.saveLogTofile();
		}

		return true;
	}

	private void startTracking() {
		mSensorListener.initListener(SensorManager.SENSOR_DELAY_NORMAL);
		SensorLog.removeLogFile();
		mIsTracking = true;
		mSensorListener.onResume();
		mTextView.setVisibility(View.GONE);
	}

	private void resetTracking() {
		if (mSensorListener != null)
			mSensorListener.onPause();
		setHintText(WAITINGGPS);
		clearStatus();
		if (mLocationClient != null) {
			mLocationClient.connect();
		}

	}

	private void clearStatus() {
		mIsTracking = false;
		mTotalStep = 0;
		mDegree = 0f;
		mLocation.setLatitude(999d);
	}

	private void setHintText(int id) {
		switch (id) {
		case WAITINGGPS:
			Log.e(TAG, "Waiting For GPS ...");
			mTextView.setVisibility(View.VISIBLE);
			mTextView.setText(R.string.waiting_gps);
			break;
		case PLEASESTART:
			Log.e(TAG, "Received GPS.Please Start Tracking");
			mTextView.setVisibility(View.VISIBLE);
			mTextView.setText(R.string.get_ready);
			break;
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		if (id == DIALOG_STEPLENGTH) {
			final EditText et = new EditText(this);
			et.setHint("CM");
			InputFilter[] filters = { new InputFilter.LengthFilter(4) };
			et.setFilters(filters);
			et.setInputType(InputType.TYPE_CLASS_NUMBER);
			final double step = SettingConfig.getSTEP_LENGTH(this) * 100;
			et.setText(String.valueOf((int) step));

			return new AlertDialog.Builder(this)
					.setTitle("Step Length")
					.setView(et)
					.setOnCancelListener(new OnCancelListener() {
						@Override
						public void onCancel(DialogInterface dialog) {
							et.setText(String.valueOf((int) step));
						}

					})
					.setPositiveButton("OK",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									String str = et.getText().toString()
											.length() == 0 ? "0" : et.getText()
											.toString();
									SettingConfig.setSTEP_LENGTH(
											IndoorNavigationActivity.this,
											Integer.valueOf(str));
								}
							}).create();
		}

		return super.onCreateDialog(id);
	}
}
