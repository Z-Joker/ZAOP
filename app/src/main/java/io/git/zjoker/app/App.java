package io.git.zjoker.app;

import android.app.Application;
import android.util.Log;

import io.git.zjoker.zaop.ZAOP;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        ZAOP.config()
                .setGlobalRTSupportCallback(new ZAOP.GlobalRTSupportCallback() {
                    @Override
                    public void onParamIsNull(String className, String method, int paramIndex, String paramName) {
                        throw new NullPointerException(String.format("Parameter '%s' is null. Index: %s,  Class : %s, Method : %s", paramName, paramIndex, className, method));
                    }
                })
                .setGlobalPermissionCallback(new ZAOP.GlobalPermissionCallback() {
                    @Override
                    public void onShouldShowRational(String permission) {
                        Log.d("checkSelfPermissions", "onShouldShowRational");
                    }

                    @Override
                    public void onPermissionReject(String permission) {
                        Log.d("checkSelfPermissions", "onPermissionReject");
                    }
                });
    }
}
