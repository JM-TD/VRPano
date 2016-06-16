package com.vr.vrpano;

import android.app.ActivityManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ConfigurationInfo;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

import com.vr.utils.SensorInfo;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private static final String TAG = "MainActivity";
    private Button mFileSelBtn = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        mFileSelBtn = (Button) findViewById(R.id.button);
        mFileSelBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View arg0)
    {
        if(arg0 == findViewById(R.id.button))
        {
            Intent intent = new Intent();
            intent.setClass(MainActivity.this, FXExplore.class);
            //startActivity(intent);
//            startActivityForResult(intent, 100);
            startActivity(intent);
        }
    }
}
