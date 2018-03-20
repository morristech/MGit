package me.sheimi.sgit;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.widget.SearchView;
import android.support.v4.view.MenuItemCompat;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.security.ProviderInstaller;
import com.manichord.mgit.transport.MGitHttpConnectionFactory;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import me.sheimi.android.activities.SheimiFragmentActivity;
import me.sheimi.sgit.activities.RepoDetailActivity;
import me.sheimi.sgit.activities.UserSettingsActivity;
import me.sheimi.sgit.activities.explorer.ExploreFileActivity;
import me.sheimi.sgit.activities.explorer.ImportRepositoryActivity;
import me.sheimi.sgit.adapters.RepoListAdapter;
import me.sheimi.sgit.database.RepoDbManager;
import me.sheimi.sgit.database.models.Repo;
import me.sheimi.sgit.dialogs.CloneDialog;
import me.sheimi.sgit.dialogs.DummyDialogListener;
import me.sheimi.sgit.dialogs.ImportLocalRepoDialog;
import me.sheimi.sgit.repo.tasks.repo.CloneTask;
import me.sheimi.sgit.ssh.PrivateKeyUtils;
import timber.log.Timber;

public class RepoListActivity extends SheimiFragmentActivity {

    private ListView mRepoList;
    private Context mContext;
    private RepoListAdapter mRepoListAdapter;

    private static final int REQUEST_IMPORT_REPO = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PrivateKeyUtils.migratePrivateKeys();

        initUpdatedSSL();

        setContentView(R.layout.activity_main);
        mRepoList = (ListView) findViewById(R.id.repoList);
        mRepoListAdapter = new RepoListAdapter(this);
        mRepoList.setAdapter(mRepoListAdapter);
        mRepoListAdapter.queryAllRepo();
        mRepoList.setOnItemClickListener(mRepoListAdapter);
        mRepoList.setOnItemLongClickListener(mRepoListAdapter);
        mContext = getApplicationContext();

        Uri uri = this.getIntent().getData();
        if (uri != null) {

            URL mRemoteRepoUrl = null;
            try {
                mRemoteRepoUrl = new URL(uri.getScheme(), uri.getHost(), uri.getPath());
            } catch (MalformedURLException e) {
                Toast.makeText(mContext, R.string.invalid_url, Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
            if (mRemoteRepoUrl != null) {

                String remoteUrl = mRemoteRepoUrl.toString();
                String repoName = remoteUrl.substring(remoteUrl.lastIndexOf("/") + 1);
                StringBuilder repoUrlBuilder = new StringBuilder(remoteUrl);
                //need git extension to clone some repos
                if (!remoteUrl.toLowerCase().endsWith(getString(R.string.git_extension))) {
                    repoUrlBuilder.append(getString(R.string.git_extension));
                } else {
                    repoName = repoName.substring(0, repoName.lastIndexOf('.'));
                }
                //Check if there are others repositories with same remote
                List<Repo> repositoriesWithSameRemote = Repo.getRepoList(mContext, RepoDbManager.searchRepo(remoteUrl));
                //if so, just open it
                if (repositoriesWithSameRemote.size() > 0) {

                    Toast.makeText(mContext, R.string.repository_already_present, Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(mContext, RepoDetailActivity.class);
                    intent.putExtra(Repo.TAG, repositoriesWithSameRemote.get(0));
                    startActivity(intent);
                }
                else{
    final String cloningStatus = getString(R.string.cloning);                Repo mRepo = Repo.createRepo(repoName , repoUrlBuilder.toString() , cloningStatus);
                    Boolean isRecursive = true;
    	            CloneTask task = new CloneTask(mRepo, true, cloningStatus, null);
                    task.executeTask();
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        configSearchAction(searchItem);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        Intent intent;
        switch (item.getItemId()) {

            case R.id.action_new:
                CloneDialog cloneDialog = new CloneDialog();
                cloneDialog.show(getSupportFragmentManager(), "clone-dialog");
                return true;
            case R.id.action_import_repo:
                intent = new Intent(this, ImportRepositoryActivity.class);
                startActivityForResult(intent, REQUEST_IMPORT_REPO);
                forwardTransition();
                return true;
            case R.id.action_settings:
                intent = new Intent(this, UserSettingsActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);

        }
    }

    public void configSearchAction(MenuItem searchItem) {

        SearchView searchView = (SearchView) searchItem.getActionView();
        if (searchView == null) {
            return;
        }
        SearchListener searchListener = new SearchListener();
        MenuItemCompat.setOnActionExpandListener(searchItem, searchListener);
        searchView.setIconifiedByDefault(true);
        searchView.setOnQueryTextListener(searchListener);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode != Activity.RESULT_OK) {
            return;
        }
        switch (requestCode) {
            case REQUEST_IMPORT_REPO:
                final String path = data.getExtras().getString(
                    ExploreFileActivity.RESULT_PATH);
                File file = new File(path);
                File dotGit = new File(file, Repo.DOT_GIT_DIR);
                if (!dotGit.exists()) {
                    showToastMessage(getString(R.string.error_no_repository));
                    return;
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(
                    this);
                builder.setTitle(R.string.dialog_comfirm_import_repo_title);
                builder.setMessage(R.string.dialog_comfirm_import_repo_msg);
                builder.setNegativeButton(R.string.label_cancel,
                    new DummyDialogListener());
                builder.setPositiveButton(R.string.label_import,
                    new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(
                            DialogInterface dialogInterface, int i) {
                            Bundle args = new Bundle();
                            args.putString(ImportLocalRepoDialog.FROM_PATH, path);
                            ImportLocalRepoDialog rld = new ImportLocalRepoDialog();
                            rld.setArguments(args);
                            rld.show(getSupportFragmentManager(), "import-local-dialog");
                        }

                    });
                builder.show();
                break;
        }
    }

    public class SearchListener implements SearchView.OnQueryTextListener,
            MenuItemCompat.OnActionExpandListener {

        @Override
        public boolean onQueryTextSubmit(String s) {

            if (mRepoListAdapter != null) {

                mRepoListAdapter.searchRepo(s);
                return true;
            }
            return false;
        }

        @Override
        public boolean onQueryTextChange(String s) {

            if (mRepoListAdapter != null) {

                mRepoListAdapter.searchRepo(s);
                return true;
            }
            return false;
        }

        @Override
        public boolean onMenuItemActionExpand(MenuItem menuItem) {
            return true;
        }

        @Override
        public boolean onMenuItemActionCollapse(MenuItem menuItem) {
            mRepoListAdapter.queryAllRepo();
            return true;
        }

    }

    public void finish() {
        rawfinish();
    }

    private void initUpdatedSSL() {
        try {
            if (Build.VERSION.SDK_INT < 21) {
                ProviderInstaller.installIfNeeded(this);
            }
        } catch (GooglePlayServicesRepairableException e) {
            showGooglePlayError(e);
        } catch (GooglePlayServicesNotAvailableException e) {
            showGooglePlayError(e);
        }
        MGitHttpConnectionFactory.install();
        Timber.i("Installed custom HTTPS factory");
    }

    private void showGooglePlayError(Exception e) {
        Timber.e(e);
        showMessageDialog(R.string.error_need_play_services_title,
            getString(R.string.error_need_play_services_message));
    }

}
