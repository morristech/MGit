package me.sheimi.android.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import me.sheimi.android.activities.SheimiFragmentActivity;
import me.sheimi.sgit.BuildConfig;
import me.sheimi.sgit.R;

/**
 * Created by sheimi on 8/8/13.
 */
public class FsUtils {

    public static final SimpleDateFormat TIMESTAMP_FORMATTER = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
    public static final String TEMP_DIR = "temp";
    private static final String LOGTAG = FsUtils.class.getSimpleName();

    private FsUtils() {
    }

    public static File createTempFile(@NonNull String subfix) throws IOException {

        File dir = getExternalDir(TEMP_DIR);
        String fileName = TIMESTAMP_FORMATTER.format(new Date());
        File file = File.createTempFile(fileName, subfix, dir);
        file.deleteOnExit();
        return file;
    }

    /**
     * Get a File representing a dir within the external shared location where files can be stored specific to this app
     * creating the dir if it doesn't already exist
     *
     * @param dirname
     * @return
     */
    public static File getExternalDir(String dirname) {
        return getExternalDir(dirname, true);
    }

    /**
     *
     * @param dirname
     * @return
     */
    public static File getInternalDir(String dirname) { return getExternalDir(dirname, true, false); }

    /**
     * Get a File representing a dir within the external shared location where files can be stored specific to this app
     *
     * @param dirname
     * @param isCreate  create the dir if it does not already exist
     * @return
     */
    public static File getExternalDir(String dirname, boolean isCreate) { return getExternalDir(dirname, isCreate, true); }

    /**
     *
     * Get a File representing a dir within the location where files can be stored specific to this app
     *
     * @param dirname  name of the dir to return
     * @param isCreate  create the dir if it does not already exist
     * @param isExternal if true, will use external *shared* storage
     * @return
     */
    public static File getExternalDir(String dirname, boolean isCreate, boolean isExternal) {

        File mDir = new File(getAppDir(isExternal), dirname);
        if (!mDir.exists() && isCreate) {
            mDir.mkdir();
        }
        return mDir;
    }

    /**
     * Get a File representing the location where files can be stored specific to this app
     *
     * @param isExternal if true, will use external *shared* storage
     * @return
     */
    public static File getAppDir(boolean isExternal) {

        SheimiFragmentActivity activeActivity = BasicFunctions.getActiveActivity();
        if (isExternal) {
            return activeActivity.getExternalFilesDir(null);
        } else {
            return activeActivity.getFilesDir();
        }
    }

    /**
     * To find out the extension of required object in given uri
     * Solution by http://stackoverflow.com/a/36514823/1171484
     */
    public static String getMimeType(@NonNull Context context, @NonNull Uri uri) {

        String extension;
        //Check uri format to avoid null
        if (uri.getScheme().equals(ContentResolver.SCHEME_CONTENT)) {
            //If scheme is a content
            final MimeTypeMap mime = MimeTypeMap.getSingleton();
            extension = mime.getExtensionFromMimeType(context.getContentResolver().getType(uri));
        } else {
            //If scheme is a File
            //This will replace white spaces with %20 and also other special characters.
            // This will avoid returning null values on file name with spaces and special characters.
            extension = MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(new File(uri.getPath())).toString());
        }
        return extension;
    }

    @Deprecated
    public static String getMimeType(@NonNull String url) {

        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(url.toLowerCase(Locale.getDefault()));
        if (extension != null) {
            final MimeTypeMap mime = MimeTypeMap.getSingleton();
            type = mime.getMimeTypeFromExtension(extension);
        }
        if (type == null) {
            type = "text/plain";
        }
        return type;
    }

    public static String getMimeType(@NonNull File file) {
        //return getMimeType(Uri.fromFile(file).toString());
        return MimeTypeUtil.getMimeType(file.getPath());
    }

    public static void openFile(@NonNull Context context, @NonNull File file) {
        openFile(context, file, MimeTypeUtil.getMimeType(file.getPath()));
    }

    public static void openFile(@NonNull Context context, @NonNull File file, @Nullable String mimeType) {

        Uri uri = getUriFromFile(context, file);
        if (mimeType == null || TextUtils.isEmpty(mimeType)) {
            mimeType = getMimeType(context, uri);
        }
        Intent intent = new Intent();
        intent.setAction(android.content.Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        if (!TextUtils.isEmpty(mimeType)) {
            intent.setDataAndType(uri, mimeType);
        } else {
            intent.setData(uri);
        }
        context.startActivity(Intent.createChooser(intent, context.getResources().getString(R.string.label_choose_app_to_open)));
    }

    public static void deleteFile(File file) {
        File to = new File(file.getAbsolutePath() + System.currentTimeMillis());
        file.renameTo(to);
        deleteFileInner(to);
    }

    private static void deleteFileInner(File file) {

        if (!file.isDirectory()) {
            file.delete();
            return;
        }
        try {
            FileUtils.deleteDirectory(file);
        } catch (IOException e) {
            //TODO 
            e.printStackTrace();
        }
    }

    public static void copyFile(File from, File to) {
        try {
            FileUtils.copyFile(from, to);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void copyDirectory(File from, File to) {
        if (!from.exists())
            return;
        try {
            FileUtils.copyDirectory(from, to);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean renameDirectory(File dir, String name) {

        String newDirPath = dir.getParent() + File.separator + name;
        File newDirFile = new File(newDirPath);
        return dir.renameTo(newDirFile);
    }

    public static String getRelativePath(File file, File base) {
        return base.toURI().relativize(file.toURI()).getPath();
    }

    public static File joinPath(File dir, String relative_path) {
        return new File(dir.getAbsolutePath() + File.separator + relative_path);
    }

    public static Uri getUriFromFile(@NonNull Context context, @NonNull File file) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return FileProvider.getUriForFile(context, BuildConfig.FILES_AUTHORITY, file);
        } else {
            return Uri.fromFile(file);
        }
    }

}
