package de.bsd.zwitscher;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import de.bsd.zwitscher.account.Account;
import de.bsd.zwitscher.helper.NetworkHelper;
import de.bsd.zwitscher.helper.PicHelper;
import twitter4j.User;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Show details about a user and allow to (un)follow
 * the user or add it to a list.
 *
 * @author Heiko W. Rupp
 */
public class UserDetailActivity extends Activity  {

    Bundle bundle;
    TwitterHelper thTwitterHelper;
    ProgressBar pg;
    TextView titleTextBox;
    User theUser;
    boolean weAreFollowing = false;
    Button followButton ;
    private int userId;
    private Account account;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.user_detail);
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,R.layout.window_title);
        pg = (ProgressBar) findViewById(R.id.title_progress_bar);
        titleTextBox = (TextView) findViewById(R.id.title_msg_box);
        followButton = (Button) findViewById(R.id.userDetail_follow_button);
        followButton.setEnabled(false);


        bundle = getIntent().getExtras();
        if (bundle!=null) {
            String userName = bundle.getString("userName");
            TextView userNameView = (TextView) findViewById(R.id.UserName);
            userNameView.setText(userName);
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.edit().putBoolean("newUser",false).commit();
    }

    public void onResume() {
        super.onResume();
        account = getIntent().getExtras().getParcelable("account");
        thTwitterHelper = new TwitterHelper(this, account);
        userId = bundle.getInt("userId");
        String userName = bundle.getString("userName");

        // If the user is in the DB, show the saved state while reloading its data
        if (userId!=0) {
            theUser = thTwitterHelper.getUserById(userId,true);
            fillDetails(theUser,false);
        }

        if (userId!=0)
            new UserDetailDownloadTask().execute(userId);
        else
            new UserDetailDownloadTask().execute(userName);
    }

    /**
     * Fill data of the passed user in the form fields.
     * @param user User object to display
     * @param weAreFollowing True if we are following that user
     */
    private void fillDetails(User user, boolean weAreFollowing) {
        if (user!=null) {
            TextView userNameView = (TextView) findViewById(R.id.UserName);
            String uName = "<b>" + user.getName() + "</b>" + " (" + user.getScreenName() + ")";
            userNameView.setText(Html.fromHtml(uName));
            userId = user.getId();
            findViewById(R.id.view_users_tweets_button).setEnabled(true);
            findViewById(R.id.view_user_on_web_button).setEnabled(true);

            String colorString = user.getProfileBackgroundColor();
            if (colorString.equals("")) {
                colorString = "#EFEFEF";
            }
            if (!colorString.startsWith("#"))  // identi.ca sends the # , but twitter does not
                colorString = "#" + colorString;
            getWindow().setTitleColor(Color.parseColor(colorString));
            boolean downloadImages = new NetworkHelper(this).mayDownloadImages();
            if (downloadImages) {
                String profileBackgroundImageUrl = user.getProfileBackgroundImageUrl();
                if (!profileBackgroundImageUrl.equals("")) {
                    try {
                        URL url = new URL(profileBackgroundImageUrl);
                        InputStream is = url.openStream();
                        Drawable background = Drawable.createFromStream(is,"lala");
                        getWindow().setBackgroundDrawable(background);
                    } catch (IOException e) {
                        e.printStackTrace();  // TODO: Customise this generated block
                    } catch (OutOfMemoryError oome) {
                        oome.printStackTrace();
                    }
                }
            }
            String textColorString = user.getProfileTextColor();
            if (textColorString.equals(""))
                textColorString = colorString;

            if (!textColorString.startsWith("#"))
                textColorString = "#" + textColorString;
            int textColor = Color.parseColor(textColorString);

            userNameView.setTextColor(textColor);


            TableLayout tl = (TableLayout) findViewById(R.id.user_table_layout);
            for (int i = 0 ; i < tl.getChildCount(); i++) {
                TableRow row = (TableRow) tl.getChildAt(i);
                for (int j = 0 ; j < row.getChildCount(); j++) {
                    TextView tv = (TextView) row.getChildAt(j);
                    tv.setTextColor(textColor);
                }
            }
            String backgroundColorString = user.getProfileSidebarFillColor();
            if (!backgroundColorString.equals("")) {
                if (!backgroundColorString.startsWith("#"))
                    backgroundColorString = "#" + backgroundColorString;
                int backgroundColor = Color.parseColor(backgroundColorString);
                tl.setBackgroundColor(backgroundColor);
                userNameView.setBackgroundColor(backgroundColor);
            }

            PicHelper picHelper = new PicHelper();
            Bitmap bitmap;
            if (downloadImages)
                bitmap = picHelper.fetchUserPic(user);
            else
                bitmap = picHelper.getBitMapForUserFromFile(user);

            if (bitmap!=null) {
                ImageView iv = (ImageView) findViewById(R.id.UserPictureImageView);
                iv.setImageBitmap(bitmap);
            }
            TextView locationView = (TextView) findViewById(R.id.userDetail_location);
            locationView.setText(user.getLocation());

            TextView bioView = (TextView) findViewById(R.id.userDetail_bio);
            bioView.setText(user.getDescription());

            TextView webView = (TextView) findViewById(R.id.userDetail_web);
            if (user.getURL()!=null) {
                webView.setText(user.getURL().toString());
                String plc = user.getProfileBackgroundColor();
                if (!plc.equals("")) {
                    if (!plc.startsWith("#"))
                        plc = "#" + plc;
                    webView.setTextColor(Color.parseColor(plc));
                }
            }

            TextView tweetView = (TextView) findViewById(R.id.userDetail_tweetCount);
            tweetView.setText("" + user.getStatusesCount());

            TextView followersView = (TextView) findViewById(R.id.userDetail_followerCount);
            followersView.setText("" + user.getFollowersCount());

            TextView followingView = (TextView) findViewById(R.id.userDetail_followingCount);
            followingView.setText("" + user.getFriendsCount());

            TextView listedView = (TextView) findViewById(R.id.userDetail_listedCount);
            listedView.setText("" + user.getListedCount());


            followButton.setEnabled(true);
            setFollowingButton(weAreFollowing);
            ImageButton addToListButton = (ImageButton) findViewById(R.id.userDetail_addListButton);
            addToListButton.setEnabled(true);
        }
    }

    /**
     * Set the appropriate text on the follow button
     * @param weAreFollowing Are we folloing that user (show 'unfollow' message in this case).
     */
    private void setFollowingButton(boolean weAreFollowing) {
        if (weAreFollowing)
            followButton.setText(R.string.unfollow_user);
        else
            followButton.setText(R.string.follow_user);
    }

    /**
     * Called from the back button to finish the activity
     * @param v View object touched
     */
    @SuppressWarnings("unused")
    public void done(View v) {
        finish();
    }

   /**
    * Allow sending a direct message to the user.
    * Called from the direct button.
    * TODO check if he follows us and thus sending is possible at all.
    * @param v View object touched
    */
    @SuppressWarnings("unused")
    public void directMessage(View v) {
       Intent i = new Intent(this, NewTweetActivity.class);
       i.putExtra("user",theUser);
       i.putExtra("op", getString(R.string.direct));
       i.putExtra("account",account);
       startActivity(i);

    }
    /**
     * Called from the followUser button
     * @param v View object touched
     */
    @SuppressWarnings("unused")
    public void followUser(View v) {

        boolean success = thTwitterHelper.followUnfollowUser(theUser.getId(),!weAreFollowing);
        if (success) {
            weAreFollowing = !weAreFollowing;
            setFollowingButton(weAreFollowing);
        }
    }

    /**
     * Start a browser to view user on server
     * Triggered from a button
     * @param v View object touched
     */
    @SuppressWarnings("unused")
    public void viewOnWeb(View v) {
        Intent i = new Intent(Intent.ACTION_VIEW);
        String u = "http://twitter.com/#!/" + theUser.getScreenName();
        i.setData(Uri.parse(u));
        startActivity(i);
    }

    /**
     * View users's recent tweets
     * Triggered from a button
     * @param v View object touched
     */
    @SuppressWarnings("unused")
    public void showUserTweets(View v) {


        Intent intent = new Intent().setClass(this,TweetListActivity.class);
        intent.putExtra("userId",userId);
        intent.putExtra("account", account);

        startActivity(intent);

    }

    /**
     * Add the user to a list
     * Called from the addToList button
     * @param v View object touched
     */
    @SuppressWarnings("unused")
    public void addToList(View v) {

        TweetDB tdb = new TweetDB(this,account.getId());

        List<String> data = new ArrayList<String>();
        Set<Map.Entry<String, Integer>> userListsEntries;

        userListsEntries = tdb.getLists().entrySet();
        for (Map.Entry<String, Integer> userList : userListsEntries) {
            data.add(userList.getKey());
        }

        Intent intent = new Intent(this,MultiSelectListActivity.class);
        intent.putStringArrayListExtra("data", (ArrayList<String>) data);
        intent.putExtra("mode","single");

        startActivityForResult(intent, 1);
    }

    /**
     * Callback that is called after the user has selected a list
     * @param requestCode selection code of the original event
     * @param resultCode result (ok, not ok)
     * @param data Data from the called Intent
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode==1 && resultCode==RESULT_OK) {
            String o = (String) data.getExtras().get("data");

            TweetDB tdb = new TweetDB(this,account.getId());
            Set<Map.Entry<String, Integer>> userListsEntries;

            int listId;
            userListsEntries = tdb.getLists().entrySet();
            for (Map.Entry<String, Integer> userList : userListsEntries) {
                if (userList.getKey().equals(o)) {
                    listId = userList.getValue();
                    thTwitterHelper.addUserToLists(theUser.getId(),listId);
                }
            }
        }
    }

    /**
     * Async task to download the userdata from server (or db) and
     * trigger its display.
     */
    private class UserDetailDownloadTask extends AsyncTask<Object,Void, Object[]> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pg.setVisibility(ProgressBar.VISIBLE);
            String s = getString(R.string.get_user_detail);
            titleTextBox.setText(s);
        }


        @Override
        protected Object[] doInBackground(Object... params) {

            Integer userId;
            User user;

            if (params[0] instanceof Integer) {
                userId = (Integer) params[0];
                user = thTwitterHelper.getUserById(userId, false);
            }
            else if (params[0] instanceof String) {
                String name= (String) params[0];
                user = thTwitterHelper.getUserByScreenName(name, false);
                if (user==null)
                    return new Object[]{};
                userId = user.getId();
            } else {
                // Should not happen
                return new Object[]{};
            }


            Boolean isFriend = thTwitterHelper.areWeFollowing(userId);
            weAreFollowing = isFriend;
            Object[] res = new Object[2];
            res[0] = user;
            res[1] = isFriend;
            return res;
        }


        @Override
        protected void onPostExecute(Object[] params) {
            super.onPostExecute(params);
            User user = (User) params[0];
            Boolean isFriend = (Boolean) params[1];
            theUser = user;
            fillDetails(user,isFriend);
            findViewById(R.id.userDetail_follow_button).setEnabled(true);
            pg.setVisibility(ProgressBar.INVISIBLE);
            titleTextBox.setText("");
        }
    }
}