package me.sheimi.android.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.FileProvider;
import android.support.v4.os.EnvironmentCompat;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import me.sheimi.sgit.BuildConfig;

/**
 * Created by Wade Morris on 2018/03/12.
 */
public class MediaStoreCompat {

    private static final String TAG = "MediaStoreCompat";

    private final WeakReference<Activity> activityReference;
    private final WeakReference<Fragment> fragmentReference;
    //private       CaptureStrategy         mCaptureStrategy;
    private       Uri currentPhotoUri;
    private       String currentPhotoPath;

    public MediaStoreCompat(@NonNull Activity activity) {

        this.activityReference = new WeakReference<>(activity);
        this.fragmentReference = null;
    }

    public MediaStoreCompat(@NonNull Activity activity, @Nullable Fragment fragment) {

        this.activityReference = new WeakReference<>(activity);
        this.fragmentReference = new WeakReference<>(fragment);
    }

    /**
     * Checks whether the device has a camera feature or not.
     *
     * @param context a activityReference to check for camera feature.
     * @return true if the device has a camera feature. false otherwise.
     */
    public static boolean hasCameraFeature(@NonNull Context context) {

        PackageManager pm = context.getApplicationContext().getPackageManager();
        return pm.hasSystemFeature(PackageManager.FEATURE_CAMERA);
    }

//    public void setCaptureStrategy(CaptureStrategy strategy) {
//        mCaptureStrategy = strategy;
//    }

    public void dispatchCaptureIntent(@NonNull Context context, int requestCode) {

        Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (captureIntent.resolveActivity(context.getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "dispatchCaptureIntent: ", e);
            }
            if (photoFile != null) {

                currentPhotoPath = photoFile.getAbsolutePath();
                //currentPhotoUri = FileProvider.getUriForFile(activityReference.get(), mCaptureStrategy.authority, photoFile);
                currentPhotoUri = FileProvider.getUriForFile(this.activityReference.get(), BuildConfig.FILES_AUTHORITY, photoFile);
                captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoUri);
                captureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {

                    List<ResolveInfo> resInfoList = context.getPackageManager().queryIntentActivities(captureIntent, PackageManager.MATCH_DEFAULT_ONLY);
                    for (ResolveInfo resolveInfo : resInfoList) {
                        String packageName = resolveInfo.activityInfo.packageName;
                        context.grantUriPermission(packageName, currentPhotoUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    }
                }
                if (fragmentReference != null) {
                    this.fragmentReference.get().startActivityForResult(captureIntent, requestCode);
                } else {
                    this.activityReference.get().startActivityForResult(captureIntent, requestCode);
                }
            }
        }
    }

    @Nullable
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = String.format("JPEG_%s.jpg", timeStamp);
        File storageDir;
        //if (mCaptureStrategy.isPublic) {
            storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        //} else {
        //    storageDir = activityReference.get().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        //}
        // Avoid joining path components manually
        File tempFile = new File(storageDir, imageFileName);
        // Handle the situation that user's external storage is not ready
        if (!Environment.MEDIA_MOUNTED.equals(EnvironmentCompat.getStorageState(tempFile))) {
            return null;
        }
        return tempFile;
    }

    public Uri getCurrentPhotoUri() {
        return currentPhotoUri;
    }

    public String getCurrentPhotoPath() {
        return currentPhotoPath;
    }

}
