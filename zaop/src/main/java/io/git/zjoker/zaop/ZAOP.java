package io.git.zjoker.zaop;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.git.zjoker.zaop.utils.Action;
import io.git.zjoker.zaop.utils.OnActResultBridge;
import io.git.zjoker.zaop.utils.ParamEntity;
import io.git.zjoker.zaop.utils.PermissionRequestBridge;

public class ZAOP {

    //    /***********************************OnActivityResult*********************************/
    public static void startActivityForResult(
            @NonNull FragmentActivity activity
            , Intent intent
            , OnActResultBridge.ActivityResultCallback callback) {
        OnActResultBridge bridge = OnActResultBridge.obtain(activity.getSupportFragmentManager());
        bridge.startActivityForResult(intent, callback);
    }

    //
//    /***********************************Permission*********************************/
    @RequiresApi(api = Build.VERSION_CODES.M)
    public static void requestPermissions(
            @NonNull FragmentActivity activity
            , String[] permissionsNeeded
            , PermissionRequestBridge.PermissionResultCallback callback) {
        PermissionRequestBridge bridge = PermissionRequestBridge.obtain(activity.getSupportFragmentManager());
        bridge.requestPermissions(permissionsNeeded, callback);
    }

    //
//
    @RequiresApi(api = Build.VERSION_CODES.M)
    public static void checkSelfPermissions(
            @NonNull final FragmentActivity context
            , String[] permissions
            , final PermissionCallback permissionCallback) {
        if (checkSelfPermissions(context, permissions)) {
            permissionCallback.onPermissionsGranted();
        } else {
            requestPermissions(
                    context
                    , permissions
                    , new PermissionRequestBridge.PermissionResultCallback() {
                        @Override
                        public void onRequestPermissionsResult(
                                @NonNull String[] permissions
                                , @NonNull int[] grantResults) {
                            String deniedPermission = checkGrantResults(permissions, grantResults);
                            if (deniedPermission == null) {
                                permissionCallback.onPermissionsGranted();
                            } else {
                                if (ActivityCompat
                                        .shouldShowRequestPermissionRationale((Activity) context, deniedPermission)) {
                                    permissionCallback.onShouldShowRational(deniedPermission);
                                } else {
                                    permissionCallback.onPermissionReject(deniedPermission);
                                }
                            }
                        }
                    });
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public static void checkSelfPermissions(@NonNull final FragmentActivity context
            , String[] permissions, final Action action) {
        ZAOP.checkSelfPermissions(context, permissions, new PermissionCallback() {
            @Override
            public void onPermissionsGranted() {
                Log.d("checkSelfPermissions", "onPermissionsGranted");
                if (action != null) {
                    action.run();
                }
            }

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

    @RequiresApi(api = Build.VERSION_CODES.M)
    public static void checkSelfPermissions(@NonNull final Object context
            , String[] permissions, final Action action) {
        if (context instanceof FragmentActivity) {
            checkSelfPermissions((FragmentActivity) context, permissions, action);
        } else {
            action.run();
        }
    }

    private static String checkGrantResults(@NonNull String[] permissions
            , @NonNull int[] grantResults) {
        for (int i = 0; i < grantResults.length; i++) {
            if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                return permissions[i];
            }
        }
        return null;
    }

    private static boolean checkSelfPermissions(Context context, String... permissions) {
        for (String permission : permissions) {
            int hasPermission = ActivityCompat.checkSelfPermission(context, permission);
            if (hasPermission == PackageManager.PERMISSION_DENIED) {
                return false;
            }
        }
        return true;
    }

    /***********************************FastClick*********************************/
    private static long lastClickTime;
    private static final long FAST_CLICK_INTERVAL_TIME = 500;

    public static boolean isFastClick() {
        long space = System.currentTimeMillis() - lastClickTime;
        lastClickTime = System.currentTimeMillis();

        if (space < FAST_CLICK_INTERVAL_TIME) {
            Log.d("isFastClick", "过滤了一次快速点击");
            return true;
        }
        return false;
    }

    /***********************************TheadOn*********************************/
    private static Handler mainHandler = new Handler(Looper.getMainLooper());
    private static ExecutorService threadPool = Executors.newCachedThreadPool();

    public static void posting(Action action) {
        action.run();
    }

    public static void background(Action action) {
        if (!isMainThread()) {
            posting(action);
        } else {
            async(action);
        }
    }

    public static void async(final Action action) {
        threadPool.execute(new Runnable() {
            @Override
            public void run() {
                action.run();
            }
        });
    }

    private static boolean isMainThread() {
        return Looper.myLooper() == mainHandler.getLooper();
    }

    public static void main(final Action action) {
        if (isMainThread()) {
            posting(action);
        } else {
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    action.run();
                }
            });
        }
    }


    public static void checkNonnull(List<ParamEntity> params) {
        for (int i = 0; i < params.size(); i++) {
            ParamEntity paramEntity = params.get(i);
            if (paramEntity.value == null) {
                throw new NullPointerException(String.format("Parameter '%s' is null.", paramEntity.name));
            }
        }
    }


    public interface PermissionCallback {
        void onPermissionsGranted();

        void onShouldShowRational(String permission);

        void onPermissionReject(String permission);
    }
}
