package io.git.zjoker.app;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.Arrays;

import io.git.zjoker.zaop.ZAOP;
import io.git.zjoker.zaop.annotations.CheckPermission;
import io.git.zjoker.zaop.annotations.FastClickAllowed;
import io.git.zjoker.zaop.annotations.RTSupport;
import io.git.zjoker.zaop.annotations.ThreadMode;
import io.git.zjoker.zaop.annotations.ThreadOn;
import io.git.zjoker.zaop.utils.OnActResultBridge;
import io.git.zjoker.zaop.utils.PermissionRequestBridge;


public class MainActivity extends TestActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.fast_click).setOnClickListener(new View.OnClickListener() {
            @Override
            //@FastClickAllowed
            public void onClick(View v) {
                for(int i=0;i<100000;i++) {}
                Log.d("demoFastClick", "demoFastClick");
            }
        });
    }

    //    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("onActivityResult", "onActivityResult111");
    }

    @CheckPermission(Manifest.permission.CAMERA)
    @RTSupport
    public int testRequestPermission(String str) {
        Log.d("testRequestPermission", "testRequestPermission");
        return 10;
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.rt_support:
                demoRTSupport(null, null);
                break;
            case R.id.start_act:
                demoStartActivityForResult();
                break;
            case R.id.check_permission:
                demoCheckPermission("demo");
                break;
            case R.id.posting:
                demoPosting();
                break;
            case R.id.main:
                new Thread() {
                    @Override
                    public void run() {
                        super.run();
                        demoMain();
                    }
                }.start();
                break;
            case R.id.background:
                demoBackgroud();
                break;
            case R.id.async:
                new Thread() {
                    @Override
                    public void run() {
                        super.run();
                        demoAsync();
                    }
                }.start();
                break;
        }
    }


    @RTSupport
    public void demoRTSupport(Object o, @NonNull String str) {
        Log.d("RTSupport", o + str);
    }

    public void demoStartActivityForResult() {
        ZAOP.startActivityForResult(
                this
                , new Intent(this, Main2Activity.class)
                , new OnActResultBridge.ActivityResultCallback() {
                    @Override
                    public void onActivityResult(int resultCode, Intent data) {
                        Toast.makeText(MainActivity.this, "来自第2个Acitivity : " + resultCode + ", " + data.getStringExtra("Data"), Toast.LENGTH_LONG).show();

                    }
                });
    }

    @CheckPermission({Manifest.permission.CAMERA, Manifest.permission.READ_CALENDAR})
    public void demoCheckPermission(String str) {
        Log.d("CheckPermission", str);
    }

    @ThreadOn(ThreadMode.POSTING)
    public void demoPosting() {
        Log.d("DemoPosting", Thread.currentThread().toString());
    }

    @ThreadOn(ThreadMode.MAIN)
    public void demoMain() {
        Log.d("DemoMain", Thread.currentThread().toString());
    }

    @ThreadOn(ThreadMode.BACKGROUND)
    public void demoBackgroud() {
        Log.d("DemoBackgroud", Thread.currentThread().toString());
    }

    @ThreadOn(ThreadMode.POSTING)
    public void demoAsync() {
        Log.d("DemoAsync", Thread.currentThread().toString());
    }
}
