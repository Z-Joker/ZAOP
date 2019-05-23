package io.git.zjoker.zaop.utils;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.util.SparseArray;

public class OnActResultBridge extends Fragment {
    public static final String TAG_BRIDGE = OnActResultBridge.class.getSimpleName();

    private SparseArray<ActivityResultCallback> onActivityResultCallbacks;
    private int autoGenerateActResultReqCode = 10000;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        onActivityResultCallbacks = new SparseArray<>();
    }

    public void startActivityForResult(Intent intent, ActivityResultCallback callback) {
        int requestCode = generateActResultRequestCode();
        onActivityResultCallbacks.put(requestCode, callback);
        startActivityForResult(intent, requestCode);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callOnActivityResult(requestCode, resultCode, data);
    }

    boolean callOnActivityResult(int requestCode, int resultCode, Intent data) {
        ActivityResultCallback resultCallback = onActivityResultCallbacks.get(requestCode);
        if (resultCallback != null) {
            onActivityResultCallbacks.remove(requestCode);
            resultCallback.onActivityResult(resultCode, data);
            return true;
        }
        return false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        onActivityResultCallbacks.clear();
    }

    private int generateActResultRequestCode() {
        return autoGenerateActResultReqCode++;
    }

    public interface ActivityResultCallback {
        void onActivityResult(int resultCode, Intent data);
    }

    public static OnActResultBridge obtain(FragmentManager fragmentManager) {
        OnActResultBridge bridge = (OnActResultBridge) fragmentManager.findFragmentByTag(OnActResultBridge.TAG_BRIDGE);
        if (bridge == null) {
            bridge = new OnActResultBridge();
            fragmentManager.beginTransaction().add(bridge, TAG_BRIDGE).commitAllowingStateLoss();
            fragmentManager.executePendingTransactions();
        }
        return bridge;
    }

    public static boolean handleResult(Object obj, int requestCode, int resultCode, Intent data) {
        if (!(obj instanceof FragmentActivity)) {
            return false;
        }
        return handleResult((FragmentActivity) obj,requestCode,resultCode,data);
    }

    public static boolean handleResult(FragmentActivity activity, int requestCode, int resultCode, Intent data) {
        Log.d("OnActResultBridge","handleResult");
        OnActResultBridge bridge = (OnActResultBridge) activity.getSupportFragmentManager().findFragmentByTag(TAG_BRIDGE);
        if (bridge == null) {
            return false;
        }

        return bridge.callOnActivityResult(requestCode & '\uffff', resultCode, data);
    }
}
