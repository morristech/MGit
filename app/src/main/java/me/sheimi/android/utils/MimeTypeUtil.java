package me.sheimi.android.utils;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.webkit.MimeTypeMap;

/**
 * Created by Wade Morris on 2018/03/09.
 */
public final class MimeTypeUtil {

    //public static final String UNKNOWN_MIME_TYPE = "unknown/unknown";
    public static final String UNKNOWN_MIME_TYPE = "text/plain";

    public static String getMimeType(@Nullable String path) {

        int index;
        if (path == null || (index = path.lastIndexOf('.')) == -1) {
            return UNKNOWN_MIME_TYPE;
        }
        String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(path.substring(index + 1).toLowerCase());
        return mime != null ? mime : UNKNOWN_MIME_TYPE;
    }

    public static String getGenericMIME(@NonNull String mime) {
        return mime.split("/")[0] + "/*";
    }

    public static String getTypeMime(@NonNull String mime) {
        return mime.split("/")[0];
    }

    private MimeTypeUtil() {
    }

}
