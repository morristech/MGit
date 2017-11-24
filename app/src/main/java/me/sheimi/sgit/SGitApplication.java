package me.sheimi.sgit;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatDelegate;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;
import org.eclipse.jgit.transport.CredentialsProvider;

import me.sheimi.android.utils.SecurePrefsException;
import me.sheimi.android.utils.SecurePrefsHelper;
import me.sheimi.sgit.preference.PreferenceHelper;
import timber.log.Timber;

/**
 * Custom Application Singleton
 */
@ReportsCrashes(
    mailTo = "wade.fbi@gmail.com",
    mode = ReportingInteractionMode.NOTIFICATION,
    nonBlockingReadForLogcat = true,
    resNotifTitle = R.string.crash_title_text,
    resNotifIcon = R.drawable.ic_logo,
    resNotifText = R.string.crash_toast_text,
    resNotifTickerText = R.string.crash_toast_text // optional, displayed as soon as the crash occurs, before collecting data which can take a few seconds
)
public class SGitApplication extends Application {

    private static Context mContext;
    private static CredentialsProvider mCredentialsProvider;

    private SecurePrefsHelper mSecPrefs;
    private PreferenceHelper mPrefsHelper;

    @Override
    public void onCreate() {
        super.onCreate();

        mContext = getApplicationContext();
        setAppVersionPref();
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        mPrefsHelper = new PreferenceHelper(this);
        try {
            mSecPrefs = new SecurePrefsHelper(this);
            mCredentialsProvider = new AndroidJschCredentialsProvider(mSecPrefs);
        } catch (SecurePrefsException e) {
            Timber.e(e);
        }
    }


    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        // The following line triggers the initialization of ACRA
        ACRA.init(this);
    }


    public SecurePrefsHelper getSecurePrefsHelper() {
        return mSecPrefs;
    }

    public PreferenceHelper getPrefenceHelper() {
        return mPrefsHelper;
    }

    public static Context getContext() {
        return mContext;
    }

    private void setAppVersionPref() {
        SharedPreferences sharedPreference = getSharedPreferences(
                getString(R.string.preference_file_key),
                Context.MODE_PRIVATE);
        String version = BuildConfig.VERSION_NAME;
        sharedPreference
            .edit()
            .putString(getString(R.string.preference_key_app_version), version)
            .apply();
    }

    public static CredentialsProvider getJschCredentialsProvider() {
        return mCredentialsProvider;
    }
}
