package com.daifan.activity;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.widget.ImageView;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockListActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.daifan.MainActivity;
import com.daifan.R;
import com.daifan.activity.adapter.PostAdapter;
import com.daifan.activity.lib.PullToRefreshBase.OnRefreshListener;
import com.daifan.activity.lib.PullToRefreshListView;
import com.daifan.domain.Post;
import com.daifan.service.PostService;
import com.daifan.service.UserService;

import java.util.ArrayList;

public class PostListActivity extends SherlockListActivity {

    private PullToRefreshListView mPullRefreshListView;
    private PostAdapter postAdapter;
    private ArrayList<Post> postList = new ArrayList<Post>();

    private UserService userService;
    private PostService postService;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        //TODO:refactoring to remove such bad code
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        setTheme(R.style.Theme_Sherlock_Light);
        super.onCreate(savedInstanceState);
        userService = new UserService(getApplicationContext());
        postService = new PostService();

        if (userService.isLoggedIn()) {
            setContentView(R.layout.pull_to_refresh_list);

            mPullRefreshListView = (PullToRefreshListView) findViewById(R.id.pull_refresh_list);

            // Set a listener to be invoked when the list should be refreshed.
            mPullRefreshListView.setOnRefreshListener(new OnRefreshListener() {
                @Override
                public void onRefresh(int refreshMode) {
                    Log.d("51daifan", "refreshMode:" + refreshMode);
                    // Do work to refresh the list here.
                    new GetDataTask(refreshMode).execute();
                }
            });
            ImageView mTopImageView = (ImageView) this.findViewById(R.id.lv_backtotop);
            mPullRefreshListView.setBackToTopView(mTopImageView);
            ListView actualListView = mPullRefreshListView.getRefreshableView();

            ArrayList<Post> posts = postService.getPosts();
            postList.addAll(posts);

            postAdapter = new PostAdapter(this, postList);

            // You can also just use setListAdapter(mAdapter)
            actualListView.setAdapter(postAdapter);
        } else {
            Intent login = new Intent(getApplicationContext(), LoginActivity.class);
            login.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(login);
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        Intent postNew = new Intent(getApplicationContext(), PostNewActivity.class);
        postNew.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        menu.add("Create")
                .setIcon(R.drawable.ic_compose_inverse)
                .setIntent(postNew)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

        menu.add("退出")
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(MainActivity.DAIFAN_TAG, "Menu item title:" + item.getTitle() + " id:" + item.getItemId() + " is selected.");
        if (item.getTitle().equals("退出")) {
            userService.logout();
            Intent login = new Intent(getApplicationContext(), LoginActivity.class);
            login.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(login);
            finish();
        } else if (item.getTitle().equals("Create")) {
            Intent postNew = new Intent(getApplicationContext(), PostNewActivity.class);
            postNew.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivityForResult(postNew, 0);
        }
        return true;
    }

    private class GetDataTask extends AsyncTask<Void, Void, ArrayList<Post>> {

        private int refreshMode;

        private GetDataTask(int refreshMode) {
            this.refreshMode = refreshMode;
        }

        @Override
        protected ArrayList<Post> doInBackground(Void... params) {
            // Simulates a background job.
            ArrayList<Post> posts = new ArrayList<Post>();
            if (refreshMode == PullToRefreshListView.REFRESHING_DOWN) {
                Post lastPost=postList.get(postList.size()-1);
                posts = postService.getOldestPosts(lastPost.getId());
            } else if (refreshMode == PullToRefreshListView.REFRESHING_TOP) {
                Post firstPost=postList.get(0);
                posts = postService.getLatestPosts(firstPost.getId());
            }
            return posts;
        }

        @Override
        protected void onPostExecute(ArrayList<Post> posts) {
            boolean hasNewData = posts.size() != 0;
            if (refreshMode == PullToRefreshListView.REFRESHING_DOWN) {
                postList.addAll(posts);
            } else {
                postList.addAll(0, posts);
            }
            postAdapter.notifyDataSetChanged();

            // Call onRefreshComplete when the list has been refreshed.
            mPullRefreshListView.onRefreshComplete(hasNewData);

            super.onPostExecute(posts);
        }
    }

}
