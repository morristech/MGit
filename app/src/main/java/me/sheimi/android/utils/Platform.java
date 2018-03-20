package me.sheimi.android.utils;

import android.os.Build;

/**
 * Created by Wade Morris on 2018/03/12.
 */
public final class Platform {

    public static boolean hasICS() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH;
    }

    public static boolean hasKitKat() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
    }

    private Platform() {
    }

}
