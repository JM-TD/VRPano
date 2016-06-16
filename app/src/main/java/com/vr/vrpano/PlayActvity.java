package com.vr.vrpano;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ConfigurationInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import com.vr.utils.SensorInfo;

/**
 * Created by BJ-CHXB on 2016/6/16.
 */
public class PlayActvity extends Activity implements SensorEventListener {

    private static final String TAG = "PlayActivity";
    private VideoSurfaceView mVideoSurfaceView;
    private MeidaPlayer mMeidaPlayer;
    private boolean mRestart = false;
    private String mFileName = null;

    private SensorManager mSensorManager;
    private Sensor mMagneticSensor;
    private Sensor mAccelerometerSensor;
    private Sensor mGyroscopeSensor;
    private double mPreviousX;
    private double mPreviousY;

    private static final float NS2S = 1.0f / 1000000000.0f;
    private long mTimeStamp;
    private double[] mAngle = new double[3];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.play_activity);

        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        ConfigurationInfo info = am.getDeviceConfigurationInfo();

        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        String str = bundle.getString("path");
        mFileName = str;

        mVideoSurfaceView = (VideoSurfaceView) findViewById(R.id.view);
        mMeidaPlayer = new MeidaPlayer(mVideoSurfaceView);

        initSensor();
    }

    private void initSensor()
    {
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mMagneticSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mAccelerometerSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mGyroscopeSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        mSensorManager.registerListener(this, mGyroscopeSensor, SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, mMagneticSensor, SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, mAccelerometerSensor, SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    protected void onRestart()
    {
        mRestart = true;
        super.onRestart();
    }

    @Override
    protected void onStart()
    {
//        String fileName = "/mnt/sdcard/DCIM/xz.mp4";
        mRestart = true;
        if(mRestart)
        {
            mVideoSurfaceView.onResume();
            mRestart = false;
        }
        mMeidaPlayer.start(mFileName);

        super.onStart();
    }

    @Override
    protected void onStop()
    {
        mMeidaPlayer.stop();
        super.onStop();
    }

    @Override
    protected void onDestroy()
    {
        mSensorManager.unregisterListener(this, mAccelerometerSensor);
        mSensorManager.unregisterListener(this, mGyroscopeSensor);
        mSensorManager.unregisterListener(this, mMagneticSensor);
        super.onDestroy();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy)
    {

    }

    @Override
    public void onSensorChanged(SensorEvent event)
    {
        if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
        {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            float a = (float) Math.sqrt(x * x + y * y + z * z);
        }
        else if(event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
        {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
        }
        else if(event.sensor.getType() == Sensor.TYPE_GYROSCOPE)
        {
            if(mTimeStamp != 0)
            {
                final float dT = (event.timestamp - mTimeStamp) * NS2S;
                mAngle[0] += event.values[0] * dT;
                mAngle[1] += event.values[1] * dT;
                mAngle[2] += event.values[2] * dT;

                double angleX = Math.toDegrees(mAngle[0]);
                double angleY = Math.toDegrees(mAngle[1]);
                double angleZ = Math.toDegrees(mAngle[2]);

                SensorInfo info = new SensorInfo();
                info.setSensorX(angleX);
                info.setSensorY(angleY);
                info.setSensorZ(angleZ);

                Message msg = new Message();
                msg.what = 101;
                msg.obj = info;
                mHandler.sendMessage(msg);
            }
            mTimeStamp = event.timestamp;
        }
    }

    Handler mHandler = new Handler() {
        public void handleMessage(Message msg)
        {
            switch (msg.what)
            {
                case 101:
                    SensorInfo info = (SensorInfo) msg.obj;
                    double y = info.getSensorY();
                    double x = info.getSensorX();
                    double dx = x - mPreviousX;
                    double dy = y - mPreviousY;

                    mVideoSurfaceView.setSensorAngle((float)dx * 1.0f, (float)dy * 1.0f);
                    mPreviousX = x;
                    mPreviousY = y;
                    break;

                default:
                    break;
            }
        }
    };

//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        if(resultCode==200){
//            Bundle bundle = data.getExtras();
//            mFileName = bundle.getString("path");
//
//            mRestart = true;
//            if(mRestart)
//            {
//                mVideoSurfaceView.onResume();
//                mRestart = false;
//            }
//            mMeidaPlayer.start(mFileName);
//        }
//    }
}
