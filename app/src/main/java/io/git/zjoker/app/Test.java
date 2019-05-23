package io.git.zjoker.app;

import android.Manifest;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.util.Log;

import io.git.zjoker.zaop.annotations.CheckPermission;

public class Test {
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        Log.d("Test", "onActivityResult111");
    }

    public static void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        Log.d("Test", "onRequestPermissionsResult");
    }

    @CheckPermission(Manifest.permission.CAMERA)
    public void f() {
        Log.d("Test", "f");
    }
}
