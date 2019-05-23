package io.git.zjoker.zaop.utils;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

@SuppressLint("ValidFragment")
public
class PermissionRequestBridge extends Fragment {
    public static final String TAG_BRIDGE = PermissionRequestBridge.class.getSimpleName();

    private Map<Integer, PermissionResultCallback> permissionResultCallbacks;
    private int autoGeneratePermissionResultReqCode = 10000;

    public PermissionRequestBridge() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        permissionResultCallbacks = new HashMap<>();
    }

    //    @RequiresApi(api = Build.VERSION_CODES.M)
    public void requestPermissions(String[] permissionsNeeded, PermissionResultCallback callback) {
        int requestCode = generateReqPermissionRequestCode();
        permissionResultCallbacks.put(requestCode, callback);
        requestPermissions(permissionsNeeded, requestCode);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        callRequestPermissionResult(requestCode, permissions, grantResults);
    }

    boolean callRequestPermissionResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        PermissionResultCallback resultCallback = permissionResultCallbacks.get(requestCode);
        if (resultCallback != null) {
            resultCallback.onRequestPermissionsResult(permissions, grantResults);
            return true;
        }
        return false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        permissionResultCallbacks.clear();
    }

    private int generateReqPermissionRequestCode() {
        return autoGeneratePermissionResultReqCode++;
    }

    public interface PermissionResultCallback {
        void onRequestPermissionsResult(@NonNull String[] permissions, @NonNull int[] grantResults);
    }

    public static PermissionRequestBridge obtain(FragmentManager fragmentManager) {
        PermissionRequestBridge bridge = (PermissionRequestBridge) fragmentManager.findFragmentByTag(PermissionRequestBridge.TAG_BRIDGE);
        if (bridge == null) {
            bridge = new PermissionRequestBridge();
            fragmentManager.beginTransaction().add(bridge, TAG_BRIDGE).commitAllowingStateLoss();
            fragmentManager.executePendingTransactions();
        }
        return bridge;
    }


    public static boolean handleResult(Object obj, int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (!(obj instanceof FragmentActivity)) {
            return false;
        }
        return handleResult((FragmentActivity) obj, requestCode, permissions, grantResults);
    }

    public static boolean handleResult(FragmentActivity activity, int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d("PermissionRequestBridge","handleResult");
        PermissionRequestBridge bridge = (PermissionRequestBridge) activity.getSupportFragmentManager().findFragmentByTag(PermissionRequestBridge.TAG_BRIDGE);
        if (bridge == null) {
            return false;
        }

        return bridge.callRequestPermissionResult(requestCode & '\uffff', permissions, grantResults);
    }
}
