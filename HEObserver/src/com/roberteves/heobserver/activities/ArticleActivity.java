package com.roberteves.heobserver.activities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;

import com.roberteves.heobserver.R;
import com.roberteves.heobserver.core.Article;
import com.roberteves.heobserver.core.Util;

import java.net.SocketTimeoutException;

public class ArticleActivity extends Activity {
    private static Article article;
    private static MenuItem comments;
    private static String link;
    private static Activity activity;
    private static Boolean closeOnResume;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Util.LogMessage("ArticleActivity", "Activity Started");
        activity = this;
        closeOnResume = false;
        setContentView(R.layout.activity_article);

        if (getIntent().getSerializableExtra("article") != null) {
            article = (Article) getIntent().getSerializableExtra("article");
        } else if (link == null) {
            link = getIntent().getStringExtra("link");
            if(link == null){
                link = getIntent().getDataString();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Util.LogMessage("ArticleActivity","Activity Ended");
        link = null;
        article = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        if(closeOnResume) {
            finish(); // close when resumed
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.article_activity_menu, menu);
        comments = menu.findItem(R.id.action_bar_comment);
        if (article != null) {
            DisplayArticle();
        } else {
            if (Util.isNetworkAvailable(this)) {
                new DownloadArticleTask().execute(link);
            } else {
                Util.DisplayToast(this, getString(R.string.error_no_internet));
                activity.finish();
            }
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Util.LogMessage("ArticleActivity","Option Selected: " + item.getTitle());
        switch (item.getItemId()) {
            case R.id.action_bar_share:
                Intent share = new Intent(android.content.Intent.ACTION_SEND);
                share.setType("text/plain");

                // Add data to the intent, the receiving app will decide
                // what to do with it.
                share.putExtra(Intent.EXTRA_SUBJECT, article.getTitle());
                share.putExtra(Intent.EXTRA_TEXT, article.getLink());

                startActivity(Intent.createChooser(share,
                        getString(R.string.action_share_via)));
                return true;
            case R.id.action_bar_comment:
                Intent i = new Intent(ArticleActivity.this,
                        CommentActivity.class);

                i.putExtra("comments", article.getComments());
                startActivity(i);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void DisplayArticle() {
        //dataString
        //.matches("http://((www.)?)hertsandessexobserver.co.uk/.*story.html") && !dataString.toUpperCase().contains("UNDEFINED-HEADLINE"))
        //article.isreadable
        Util.LogMessage("ArticleActivity","Display Article");
        if(article.isReadable() && article.getLink().matches("http://((www.)?)hertsandessexobserver.co.uk/.*story.html") && !article.getLink().toUpperCase().contains("UNDEFINED-HEADLINE")) {
            TextView txtTitle = (TextView) findViewById(R.id.txtTitle);
            TextView txtBody = (TextView) findViewById(R.id.txtBody);
            TextView txtPubDate = (TextView) findViewById(R.id.txtPubDate);

            txtTitle.setText(article != null ? article.getTitle() : null);
            txtBody.setText(Html.fromHtml(article.getBody()));

            if (article.getPublishedDate() != null) {
                txtPubDate.setText(getString(R.string.published) + article.getPublishedDate());
            } else {
                txtPubDate.setText("");
            }

            article.processComments();

            comments.setVisible(article.hasComments());
        }
        else
        {
            Util.DisplayToast(getApplicationContext(),getString(R.string.error_not_supported));
            closeOnResume = true;
            Intent intent = new Intent(ArticleActivity.this,WebActivity.class);
            intent.putExtra("link",article.getLink());
            startActivity(intent);
        }
    }

    private class DownloadArticleTask extends AsyncTask<String, Void, Boolean> {
        private final ProgressDialog dialog = new ProgressDialog(ArticleActivity.this);
        private String toastMessage;

        @Override
        protected void onPreExecute() {
            Util.LogMessage("DownloadArticleAsync","Pre Execute");
            this.dialog.setMessage(getString(R.string.dialog_fetching_article));
            this.dialog.setCancelable(false);
            this.dialog.show();
        }

        @Override
        protected Boolean doInBackground(String... params) {
            Util.LogMessage("DownloadArticleAsync", "Execute");
            if (Util.isInternetAvailable()) {
                try {
                    article = new Article(link);
                    
                    return true;
                } catch (Exception e) {
                    if (!(e instanceof SocketTimeoutException)) { //Don't log if timeout exception
                        Util.LogException("load article", link, e);
                    } else {
                        Util.LogMessage("SocketTimeout", "Article: " + link);
                    }
                    toastMessage = getString(R.string.error_load_article);
                    return false;
                }
            } else {
                Util.LogMessage("DownloadArticleAsync", "No Internet");
                toastMessage = getString(R.string.error_no_internet);
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            Util.LogMessage("DownloadArticleAsync", "Post Execute");
            if (dialog.isShowing()) {
                dialog.dismiss();
            }

            if (result) {
                DisplayArticle();
            } else {
                Util.DisplayToast(ArticleActivity.this,toastMessage);
                activity.finish();
            }
        }
    }
}
