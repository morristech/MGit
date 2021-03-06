package me.sheimi.sgit.activities;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.PagerTitleStrip;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.io.File;

import me.sheimi.android.activities.SheimiFragmentActivity;
import me.sheimi.android.utils.FsUtils;
import me.sheimi.android.utils.MimeTypeUtil;
import me.sheimi.sgit.R;
import me.sheimi.sgit.RepoListActivity;
import me.sheimi.sgit.database.models.Repo;
import me.sheimi.sgit.dialogs.ChooseLanguageDialog;
import me.sheimi.sgit.fragments.BaseFragment;
import me.sheimi.sgit.fragments.CommitsFragment;
import me.sheimi.sgit.fragments.ViewFileFragment;

public class ViewFileActivity extends SheimiFragmentActivity {

    private static final String TAG = "ViewFileActivity";

    public static String TAG_FILE_NAME = "file_name";
    public static String TAG_MODE = "mode";
    public static short TAG_MODE_NORMAL = 0;
    public static short TAG_MODE_SSH_KEY = 1;
    private CommitsFragment commitsFragment;
    private short activityMode = TAG_MODE_NORMAL;
    private static final int FILE_FRAGMENT_INDEX = 0;
    private static final int COMMITS_FRAGMENT_INDEX = 1;
    private ViewPager viewPager;
    private Repo repo;
    private TabItemPagerAdapter tabItemPagerAdapter;
    private ViewFileFragment fileFragment;
    private PowerManager.WakeLock wakeLock;
    private int currentTab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_file);
        repo = (Repo) getIntent().getSerializableExtra(Repo.TAG);
        viewPager = (ViewPager) findViewById(R.id.pager);
        tabItemPagerAdapter = new TabItemPagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(tabItemPagerAdapter);
        viewPager.addOnPageChangeListener(tabItemPagerAdapter);
        Bundle b = new Bundle();
        Bundle extras = getIntent().getExtras();
        String fileName = extras.getString(TAG_FILE_NAME);
        activityMode = extras.getShort(TAG_MODE, TAG_MODE_NORMAL);
        b.putString(TAG_FILE_NAME, fileName);
        if (repo != null) {

            b.putSerializable(Repo.TAG, repo);
            commitsFragment = CommitsFragment.newInstance(repo, FsUtils.getRelativePath(new File(fileName), repo.getDir()));
        }
        if (repo == null) {
            PagerTitleStrip strip = (PagerTitleStrip) findViewById(R.id.pager_title_strip);
            strip.setVisibility(View.GONE);
        }
        fileFragment = new ViewFileFragment();
        fileFragment.setArguments(b);
        activityMode = extras.getShort(TAG_MODE, TAG_MODE_NORMAL);
        b.putShort(TAG_MODE, activityMode);
        if (!TextUtils.isEmpty(fileName)) {
            setTitle(new File(fileName).getName());
        }
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        if (powerManager != null) {

            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
            wakeLock.setReferenceCounted(false);
        }
    }


    class TabItemPagerAdapter extends FragmentPagerAdapter implements ViewPager.OnPageChangeListener, SearchView.OnQueryTextListener, MenuItem.OnActionExpandListener {

        private final int[] PAGE_TITLE = {R.string.tab_file_label, R.string.tab_commits_label};

        public TabItemPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public BaseFragment getItem(int position) {
            switch (position) {
                case FILE_FRAGMENT_INDEX:
                    return fileFragment;
                case COMMITS_FRAGMENT_INDEX:
                    return commitsFragment;
            }
            return fileFragment;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return getString(PAGE_TITLE[position]);
        }

        @Override
        public int getCount() {

            if (repo == null) {
                return 1;
            }
            return PAGE_TITLE.length;
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        @Override
        public void onPageSelected(int position) {
            currentTab = position;
            invalidateOptionsMenu();
        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }

        @Override
        public boolean onQueryTextSubmit(String query) {

            switch (viewPager.getCurrentItem()) {
                case COMMITS_FRAGMENT_INDEX:
                    commitsFragment.setFilter(query);
                    break;
            }
            return true;
        }

        @Override
        public boolean onQueryTextChange(String query) {

            switch (viewPager.getCurrentItem()) {
                case COMMITS_FRAGMENT_INDEX:
                    commitsFragment.setFilter(query);
                    break;
            }
            return true;
        }

        @Override
        public boolean onMenuItemActionExpand(MenuItem menuItem) {
            return true;
        }

        @Override
        public boolean onMenuItemActionCollapse(MenuItem menuItem) {

            switch (viewPager.getCurrentItem()) {
                case COMMITS_FRAGMENT_INDEX:
                    commitsFragment.setFilter(null);
                    break;
            }
            return true;
        }

    }

    private void setSaveStatus(MenuItem mi) {

        if (fileFragment.getEditMode()) {
            mi.setIcon(R.drawable.ic_action_save);
            mi.setTitle(R.string.action_edit_save);
        } else {
            mi.setIcon(R.drawable.ic_action_edit);
            mi.setTitle(R.string.action_edit);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.view_file, menu);
        if (activityMode == TAG_MODE_SSH_KEY) {
            menu.removeItem(R.id.action_edit);
            menu.removeItem(R.id.action_edit_in_other_app);
            menu.removeItem(R.id.action_choose_language);
        } else {
            menu.removeItem(R.id.action_copy_all);
        }
        if (activityMode != TAG_MODE_SSH_KEY) {
            MenuItem mi = menu.findItem(R.id.action_edit);
            setSaveStatus(mi);
            mi.setVisible(currentTab == FILE_FRAGMENT_INDEX);
            mi = menu.findItem(R.id.action_edit_in_other_app);
            mi.setVisible(currentTab == FILE_FRAGMENT_INDEX);
            mi = menu.findItem(R.id.action_choose_language);
            mi.setVisible(currentTab == FILE_FRAGMENT_INDEX);
        }
        if (repo != null) {
            MenuItem searchItem = menu.findItem(R.id.action_search);
            searchItem.setOnActionExpandListener(tabItemPagerAdapter);
            SearchView searchView = (SearchView) searchItem.getActionView();
            if (searchView != null) {

                searchView.setIconifiedByDefault(true);
                searchView.setOnQueryTextListener(tabItemPagerAdapter);
            }
            searchItem.setVisible(currentTab == COMMITS_FRAGMENT_INDEX);
        } else {
            menu.removeItem(R.id.action_search);
        }
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            case android.R.id.home:
                finish();
                return true;
            case R.id.action_edit_in_other_app:
                if (activityMode == TAG_MODE_SSH_KEY) {
                    return true;
                }
                if (fileFragment != null && fileFragment.getFile() != null) {

                    Uri uri = Uri.fromFile(fileFragment.getFile());
                    String mimeType = getContentResolver().getType(uri);
                    if (TextUtils.isEmpty(mimeType)) {
                        //mimeType = FsUtils.getMimeType(uri.toString());
                        mimeType = MimeTypeUtil.getMimeType(uri.toString());
                    }
                    Intent viewIntent = new Intent(Intent.ACTION_VIEW);
                    Intent editIntent = new Intent(Intent.ACTION_EDIT);
                    viewIntent.setDataAndType(uri, mimeType);
                    editIntent.setDataAndType(uri, mimeType);
                    try {
                        Intent chooserIntent = Intent.createChooser(viewIntent, getString(R.string.label_choose_app_to_edit));
                        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{editIntent});
                        startActivity(chooserIntent);
                        forwardTransition();
                    } catch (ActivityNotFoundException e) {
                        showMessageDialog(R.string.dialog_error_title, getString(R.string.error_no_edit_app));
                    }
                }
                break;
            case R.id.action_edit:
                if (activityMode == TAG_MODE_SSH_KEY) {
                    return true;
                }
                fileFragment.setEditMode(!fileFragment.getEditMode());
                setSaveStatus(item);
                return true;
            case R.id.action_choose_language:
                if (activityMode == TAG_MODE_SSH_KEY) {
                    return true;
                }
                ChooseLanguageDialog cld = new ChooseLanguageDialog();
                cld.show(getSupportFragmentManager(), "choose language");
                return true;
            case R.id.action_copy_all:
                fileFragment.copyAll();
                return true;

        }
        return super.onOptionsItemSelected(item);
    }

    public void setLanguage(String lang) {
        fileFragment.setLanguage(lang);
    }

    @Override
    protected void onResume() {

        super.onResume();
        if (wakeLock != null && !wakeLock.isHeld()) {
            wakeLock.acquire();
        }
    }

    @Override
    protected void onPause() {

        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        super.onPause();
    }

}
