package me.sheimi.sgit.fragments;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ProgressBar;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

import me.sheimi.android.activities.SheimiFragmentActivity;
import me.sheimi.android.utils.CodeGuesser;
import me.sheimi.android.utils.Profile;
import me.sheimi.sgit.R;
import me.sheimi.sgit.activities.ViewFileActivity;
import timber.log.Timber;

/**
 * Created by phcoder on 09.12.15.
 */
public class ViewFileFragment extends BaseFragment {

    private static final String TAG = "ViewFileFragment";

    private WebView fileContent;
    private static final String JS_INF = "CodeLoader";
    private ProgressBar loading;
    private File file;
    private short activityMode = ViewFileActivity.TAG_MODE_NORMAL;
    private boolean editMode = false;

    @SuppressLint({"AddJavascriptInterface", "SetJavaScriptEnabled"})
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_view_file, container, false);

        fileContent = (WebView) v.findViewById(R.id.fileContent);
        loading = (ProgressBar) v.findViewById(R.id.loading);

        String fileName = null;
        if (savedInstanceState != null) {

            fileName = savedInstanceState.getString(ViewFileActivity.TAG_FILE_NAME);
            activityMode = savedInstanceState.getShort(ViewFileActivity.TAG_MODE, ViewFileActivity.TAG_MODE_NORMAL);
        }
        if (getArguments() != null && TextUtils.isEmpty(fileName)) {

            fileName = getArguments().getString(ViewFileActivity.TAG_FILE_NAME);
            activityMode = getArguments().getShort(ViewFileActivity.TAG_MODE, ViewFileActivity.TAG_MODE_NORMAL);
        }
        if (!TextUtils.isEmpty(fileName)) {

            file = new File(fileName);
            fileContent.addJavascriptInterface(new CodeLoader(), JS_INF);
            WebSettings webSettings = fileContent.getSettings();
            if (webSettings != null) {

                webSettings.setJavaScriptEnabled(true);
                webSettings.setAllowFileAccess(true);
                webSettings.setAllowFileAccessFromFileURLs(true);
                fileContent.setWebChromeClient(new WebChromeClient() {

                    @Override
                    public void onConsoleMessage(String message, int lineNumber, String sourceID) {
                        Log.d(TAG, message + " -- From line " + lineNumber
                            + " of " + sourceID);
                    }
                    public boolean shouldOverrideUrlLoading(WebView view, String url) {
                        return false;
                    }

                });
            }
        }
        fileContent.setBackgroundColor(Color.TRANSPARENT);
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadFileContent();
    }

    @Override
    public void onPause() {

        super.onPause();
        if (editMode) {
            fileContent.loadUrl(CodeGuesser.wrapUrlScript("save();"));
        }
    }

    private void loadFileContent() {

        fileContent.loadUrl("file:///android_asset/editor.html");
        fileContent.setFocusable(editMode);
    }

    public boolean getEditMode() {
        return editMode;
    }

    public File getFile() {
        return file;
    }

    public void setEditMode(boolean mode) {

        editMode = mode;
        fileContent.setFocusable(editMode);
        fileContent.setFocusableInTouchMode(editMode);
        if (editMode) {
            fileContent.loadUrl(CodeGuesser.wrapUrlScript("setEditable();"));
            showToastMessage(R.string.msg_now_you_can_edit);
        } else {
            fileContent.loadUrl(CodeGuesser.wrapUrlScript("save();"));
        }
    }

    public void copyAll() {
        fileContent.loadUrl(CodeGuesser.wrapUrlScript("copy_all();"));
    }

    public void setLanguage(String lang) {

        String js = String.format("setLang('%s')", lang);
        fileContent.loadUrl(CodeGuesser.wrapUrlScript(js));
    }

    private class CodeLoader {

        private String code;

        @JavascriptInterface
        public String getCode() {
            return code;
        }

        @JavascriptInterface
        public void copy_all(final String content) {

            if (getActivity() != null) {

                ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard != null) {

                    ClipData clip = ClipData.newPlainText("forker", content);
                    clipboard.setPrimaryClip(clip);
                }
            }
        }

        @JavascriptInterface
        public void save(final String content) {

            if (activityMode == ViewFileActivity.TAG_MODE_SSH_KEY) {
                return;
            }
            if (content == null) {
                showToastMessage(R.string.alert_save_failed);
                return;
            }
            Thread thread = new Thread(new Runnable() {

                @Override
                public void run() {

                    try {
                        FileUtils.writeStringToFile(file, content, "UTF-8");
                    } catch (IOException e) {
                        showUserError(e, R.string.alert_save_failed);
                    }
                    if (getActivity() != null) {

                        getActivity().runOnUiThread(new Runnable() {

                            @Override
                            public void run() {

                                loadFileContent();
                                showToastMessage(R.string.success_save);
                            }

                        });
                    }
                }
            });
            thread.start();
        }

        @JavascriptInterface()
        public void loadCode() {

            Thread thread = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        code = FileUtils.readFileToString(file, "UTF-8");
                    } catch (IOException e) {
                        showUserError(e, R.string.error_can_not_open_file);
                    }
                    display();
                }

            });
            thread.start();
        }

        @JavascriptInterface
        public String getTheme() {
            return Profile.getCodeMirrorTheme(getActivity());
        }

        private void display() {

            if (getActivity() != null) {

                getActivity().runOnUiThread(new Runnable() {

                    @Override
                    public void run() {

                        String lang;
                        if (activityMode == ViewFileActivity.TAG_MODE_SSH_KEY) {
                            lang = null;
                        } else {
                            lang = CodeGuesser.guessCodeType(file.getName());
                        }
                        String js = String.format("setLang('%s')", lang);
                        fileContent.loadUrl(CodeGuesser.wrapUrlScript(js));
                        loading.setVisibility(View.INVISIBLE);
                        fileContent.loadUrl(CodeGuesser .wrapUrlScript("display();"));
                        if (editMode) {
                            fileContent.loadUrl(CodeGuesser.wrapUrlScript("setEditable();"));
                        }
                    }

                });
            }
        }

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {

        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            editMode = savedInstanceState.getBoolean("EditMode", false);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle savedInstanceState) {

        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putBoolean("EditMode", editMode);
    }

    @Override
    public void reset() {
    }

    @Override
    public SheimiFragmentActivity.OnBackClickListener getOnBackClickListener() {

        return new SheimiFragmentActivity.OnBackClickListener() {

            @Override
            public boolean onClick() {
                return false;
            }

        };
    }

    private void showUserError(Throwable e, final int errorMessageId) {

        Timber.e(e);
        if (getActivity() != null) {

            getActivity().runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    ((SheimiFragmentActivity)getActivity()).showMessageDialog(R.string.dialog_error_title, getString(errorMessageId));
                }

            });
        }

    }
}
