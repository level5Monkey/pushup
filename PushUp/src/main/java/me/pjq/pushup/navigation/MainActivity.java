/*
 * Copyright 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.pjq.pushup.navigation;

import java.util.Locale;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.SearchManager;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.*;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.*;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.*;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.games.GamesClient;
import com.google.android.gms.games.Player;
import com.google.example.games.basegameutils.BaseGameActivity;
import com.squareup.otto.Bus;
import me.pjq.pushup.*;
import me.pjq.pushup.activity.DashboardActivity;
import me.pjq.pushup.fragment.*;
import me.pjq.pushup.utils.TitlebarHelper;
import me.pjq.pushup.utils.Utils;

/**
 * This example illustrates a common usage of the DrawerLayout widget
 * in the Android support library.
 * <p/>
 * <p>When a navigation (left) drawer is present, the host activity should detect presses of
 * the action bar's Up affordance as a signal to open and close the navigation drawer. The
 * ActionBarDrawerToggle facilitates this behavior.
 * Items within the drawer should fall into one of two categories:</p>
 * <p/>
 * <ul>
 * <li><strong>View switches</strong>. A view switch follows the same basic policies as
 * list or tab navigation in that a view switch does not create navigation history.
 * This pattern should only be used at the root activity of a task, leaving some form
 * of Up navigation active for activities further down the navigation hierarchy.</li>
 * <li><strong>Selective Up</strong>. The drawer allows the user to choose an alternate
 * parent for Up navigation. This allows a user to jump across an app's navigation
 * hierarchy at will. The application should treat this as it treats Up navigation from
 * a different task, replacing the current task stack using TaskStackBuilder or similar.
 * This is the only form of navigation drawer that should be used outside of the root
 * activity of a task.</li>
 * </ul>
 * <p/>
 * <p>Right side drawers should be used for actions, not navigation. This follows the pattern
 * established by the Action Bar that navigation should be to the left and actions to the right.
 * An action should be an operation performed on the current contents of the window,
 * for example enabling or disabling a data overlay on top of the current content.</p>
 */
public class MainActivity extends BaseGameActivity implements View.OnClickListener, FragmentController {
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private ActionBarDrawerToggle mDrawerToggle;

    private CharSequence mDrawerTitle;
    private CharSequence mTitle;
    private String[] mPlanetTitles;

    private static final String TAG = MainActivity.class.getSimpleName();
    private TextView userInfo;
    private ImageView userIcon;
    private TextView share;

    Bus bus;
    AppPreference appPreference;

    int CONTENT_VIEW_ID;
    DashboardFragment dashboardFragment;
    ProximityFragment proximityFragment;

    private String currentFragmentTag;
    private TitlebarHelper titlebarHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_drawer);
        CONTENT_VIEW_ID = R.id.content_frame;

        mTitle = mDrawerTitle = getTitle();
        mPlanetTitles = getResources().getStringArray(R.array.planets_array);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);

        // set a custom shadow that overlays the main content when the drawer opens
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        // set up the drawer's list view with items and click listener
        mDrawerList.setAdapter(new ArrayAdapter<String>(this,
                R.layout.drawer_list_item, mPlanetTitles));
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

        // enable ActionBar app icon to behave as action to toggle nav drawer
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);

        // ActionBarDrawerToggle ties together the the proper interactions
        // between the sliding drawer and the action bar app icon
        mDrawerToggle = new ActionBarDrawerToggle(
                this,                  /* host Activity */
                mDrawerLayout,         /* DrawerLayout object */
                R.drawable.ic_drawer,  /* nav drawer image to replace 'Up' caret */
                R.string.drawer_open,  /* "open drawer" description for accessibility */
                R.string.drawer_close  /* "close drawer" description for accessibility */
                ) {
            public void onDrawerClosed(View view) {
                getActionBar().setTitle(mTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            public void onDrawerOpened(View drawerView) {
                getActionBar().setTitle(mDrawerTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        if (savedInstanceState == null) {
            selectItem(0);
        }

        bus = ServiceProvider.getBus();
        bus.register(this);

        appPreference = AppPreference.getInstance(getApplicationContext());
        titlebarHelper = new TitlebarHelper(MainActivity.this, new TitlebarHelper.OnTitlebarClickListener() {
            @Override
            public void onClickIcon() {
            }

            @Override
            public void onClickTitle() {
            }
        });

        userIcon = (ImageView) findViewById(R.id.user_icon);
        userInfo = (TextView) findViewById(R.id.user_info);
        share = (TextView) findViewById(R.id.share_textview);

        userInfo.setOnClickListener(this);
        userIcon.setOnClickListener(this);
        share.setOnClickListener(this);
        findViewById(R.id.button_sign_in).setOnClickListener(this);
        findViewById(R.id.button_sign_out).setOnClickListener(this);

        getGamesClient().registerConnectionCallbacks(new GooglePlayServicesClient.ConnectionCallbacks() {
            @Override
            public void onConnected(Bundle bundle) {
                submitScore();
            }

            @Override
            public void onDisconnected() {
                showAlert("Connect", "Disconnected!");
            }
        });

        getGamesClient().connect();

        showDashboardFragment();
    }


    public void showProximityFragment() {
        currentFragmentTag = ProximityFragment.TAG;
        android.support.v4.app.Fragment fragment = findFragmentByTag(ProximityFragment.TAG);
        hideTheOtherFragment();

        if (null == fragment) {
            fragment = ProximityFragment.newInstance(new Bundle());
//            replaceChildFragment(fragment, ProximityFragment.TAG, fromLeft2Right());
            addChildFragment(fragment, ProximityFragment.TAG, fromLeft2Right());

        } else {
            showFragment(fragment, ProximityFragment.TAG, fromLeft2Right());
        }

        notifyFragmentChangeAll(ProximityFragment.TAG);
    }

    public void showGameBoardFragment() {
        currentFragmentTag = GameBoardFragment.TAG;
        android.support.v4.app.Fragment fragment = findFragmentByTag(GameBoardFragment.TAG);
        hideTheOtherFragment();

        if (null == fragment) {
            fragment = GameBoardFragment.newInstance(new Bundle());
//            replaceChildFragment(fragment, GameBoardFragment.TAG, fromLeft2Right());
            addChildFragment(fragment, GameBoardFragment.TAG, fromLeft2Right());

        } else {
            showFragment(fragment, GameBoardFragment.TAG, fromLeft2Right());
        }

        notifyFragmentChangeAll(GameBoardFragment.TAG);
    }

    public void showDashboardFragment() {
        currentFragmentTag = DashboardFragment.TAG;
        android.support.v4.app.Fragment fragment = findFragmentByTag(DashboardFragment.TAG);
        hideTheOtherFragment();

        if (null == fragment) {
            fragment = DashboardFragment.newInstance(new Bundle());
//            replaceChildFragment(fragment, DashboardFragment.TAG, fromLeft2Right());
            addChildFragment(fragment, DashboardFragment.TAG, fromLeft2Right());

        } else {
            showFragment(fragment, DashboardFragment.TAG, fromLeft2Right());
        }

        notifyFragmentChangeAll(DashboardFragment.TAG);
    }

    public void showLanGameFragment() {
        currentFragmentTag = LanGameFragment.TAG;
        android.support.v4.app.Fragment fragment = findFragmentByTag(LanGameFragment.TAG);
        hideTheOtherFragment();

        if (null == fragment) {
            fragment = LanGameFragment.newInstance(new Bundle());
//            replaceChildFragment(fragment, LanGameFragment.TAG, fromLeft2Right());
            addChildFragment(fragment, LanGameFragment.TAG, fromLeft2Right());

        } else {
            showFragment(fragment, LanGameFragment.TAG, fromLeft2Right());
        }

        notifyFragmentChangeAll(LanGameFragment.TAG);
    }

    private void notifyFragmentChange(String fragmentTag, String tag) {
        android.support.v4.app.Fragment fragment = findFragmentByTag(fragmentTag);
        if (null != fragment) {
            ((FragmentBridge) fragment).changeToFragment(tag);
        }
    }

    private void notifyFragmentChangeAll(String tag) {
        notifyFragmentChange(ProximityFragment.TAG, tag);
        notifyFragmentChange(DashboardFragment.TAG, tag);
        notifyFragmentChange(GameBoardFragment.TAG, tag);
        notifyFragmentChange(LanGameFragment.TAG, tag);
    }


    private boolean fromLeft2Right() {
        return true;
//        if (currentClickedIndex == 0 && previewClickedIndex == 0) {
//            return true;
//        }
//
//
//        if (currentClickedIndex > previewClickedIndex) {
//            return true;
//        } else {
//            return false;
//        }
    }

    private void hideTheOtherFragment() {
        android.support.v4.app.Fragment fragment = findFragmentByTag(ProximityFragment.TAG);
        hideFragment(fragment, fromLeft2Right());

        fragment = findFragmentByTag(DashboardFragment.TAG);
        hideFragment(fragment, fromLeft2Right());

        fragment = findFragmentByTag(GameBoardFragment.TAG);
        hideFragment(fragment, fromLeft2Right());

        fragment = findFragmentByTag(LanGameFragment.TAG);
        hideFragment(fragment, fromLeft2Right());
    }

    private void submitScore() {
        if (!getGamesClient().isConnected()) {
            return;
        }

        getGamesClient().unlockAchievement(
                getString(R.string.achievement_activate));

        int total = AppPreference.getInstance(getApplicationContext()).getTotalNumber();
        getGamesClient().submitScore(getString(R.string.leaderboard_total_pushups), total);

        if (total >= 100) {
            getGamesClient().unlockAchievement(
                    getString(R.string.achievement_100_pushup));
        }

        if (total >= 500) {
            getGamesClient().unlockAchievement(
                    getString(R.string.achievement_master_level500));
        }

        int totalDay = AppPreference.getInstance(getApplicationContext()).getHowManyDays();
        getGamesClient().submitScore(getString(R.string.leaderboard_total_days), totalDay);

        if (totalDay >= 2) {
            getGamesClient().unlockAchievement(
                    getString(R.string.achievement_2days));
        }

        if (totalDay >= 10) {
            getGamesClient().unlockAchievement(
                    getString(R.string.achievement_10days));
        }

    }

    // Shows the "sign in" bar (explanation and button).
    private void showSignInBar() {
        findViewById(R.id.sign_in_bar).setVisibility(View.VISIBLE);
        findViewById(R.id.sign_out_bar).setVisibility(View.GONE);
    }

    // Shows the "sign out" bar (explanation and button).
    private void showSignOutBar() {
        findViewById(R.id.sign_in_bar).setVisibility(View.GONE);
        findViewById(R.id.sign_out_bar).setVisibility(View.VISIBLE);
    }

    /**
     * Called to notify us that sign in failed. Notice that a failure in sign in is not
     * necessarily due to an error; it might be that the user never signed in, so our
     * attempt to automatically sign in fails because the user has not gone through
     * the authorization flow. So our reaction to sign in failure is to show the sign in
     * button. When the user clicks that button, the sign in process will start/resume.
     */
    @Override
    public void onSignInFailed() {
        // Sign-in has failed. So show the user the sign-in button
        // so they can click the "Sign-in" button.
        showSignInBar();
    }

    /**
     * Called to notify us that sign in succeeded. We react by loading the loot from the
     * cloud and updating the UI to show a sign-out button.
     */
    @Override
    public void onSignInSucceeded() {
        // Sign-in worked!
        showSignOutBar();

        playerInfo();
    }

    private void playerInfo() {
        Player player = getGamesClient().getCurrentPlayer();
        String name = player.getDisplayName();
        Uri uri = player.getIconImageUri();

        String displayName;
        if (player == null) {
            Log.w(TAG, "mGamesClient.getCurrentPlayer() is NULL!");
            displayName = "???";
        } else {
            displayName = player.getDisplayName();
            appPreference.setLoginName(displayName);
        }


        userInfo.setText(String.format(getString(R.string.you_are_signed_in_as), displayName));
        userIcon.setImageURI(uri);
    }

    private void addChildFragment(android.support.v4.app.Fragment childFragment) {
        android.support.v4.app.FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add(CONTENT_VIEW_ID, childFragment);
        fragmentTransaction.commit();
    }

    protected void addChildFragment(android.support.v4.app.Fragment fragment, String tag) {
        android.support.v4.app.FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        fragmentTransaction.add(CONTENT_VIEW_ID, fragment, tag);
        fragmentTransaction.commitAllowingStateLoss();
    }

    protected void showChildFragment(android.support.v4.app.Fragment fragment, String tag) {
        android.support.v4.app.FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(CONTENT_VIEW_ID, fragment, tag);
        fragmentTransaction.commit();
    }

    private void replaceChildFragment(android.support.v4.app.Fragment fragment) {
        android.support.v4.app.FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(CONTENT_VIEW_ID, fragment);

        fragmentTransaction.commitAllowingStateLoss();
    }

    protected void replaceChildFragment(android.support.v4.app.Fragment fragment, String tag, boolean fromLeftToRight) {
        android.support.v4.app.FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        if (fromLeftToRight) {
            fragmentTransaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left);
        } else {

            fragmentTransaction.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right);
        }
        fragmentTransaction.replace(CONTENT_VIEW_ID, fragment, tag);
        //fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        //fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        // fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commitAllowingStateLoss();
    }

    protected void addChildFragment(android.support.v4.app.Fragment fragment, String tag, boolean fromLeftToRight) {
        android.support.v4.app.FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        if (fromLeftToRight) {
            fragmentTransaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left);
        } else {

            fragmentTransaction.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right);
        }
        fragmentTransaction.add(CONTENT_VIEW_ID, fragment, tag);
        //fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        //fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        // fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commitAllowingStateLoss();
    }

    protected void showFragment(android.support.v4.app.Fragment fragment, String tag, boolean fromLeftToRight) {
        android.support.v4.app.FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        if (fromLeftToRight) {
            fragmentTransaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left);
        } else {
            fragmentTransaction.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right);
        }
        fragmentTransaction.show(fragment);
        fragmentTransaction.commit();
    }


    protected void replaceChildFragment(android.support.v4.app.Fragment fragment, String tag) {
        android.support.v4.app.FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        fragmentTransaction.replace(CONTENT_VIEW_ID, fragment, tag);
        //fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        // fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commitAllowingStateLoss();
    }

    protected void removeFragment(android.support.v4.app.Fragment fragment) {
        android.support.v4.app.FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.remove(fragment);
        fragmentTransaction.commitAllowingStateLoss();
    }


    protected android.support.v4.app.Fragment findFragmentByTag(String tag) {
        android.support.v4.app.FragmentManager fragmentManager = getSupportFragmentManager();
        android.support.v4.app.Fragment fragment = fragmentManager.findFragmentByTag(tag);
        EFLogger.i(TAG, "findFragmentByTag,tag=" + tag + ",fragment=" + fragment);

        return fragment;
    }

    protected void hideFragment(android.support.v4.app.Fragment fragment, boolean fromLeftToRight) {
        if (null == fragment) {
            return;
        }

        android.support.v4.app.FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        if (fromLeftToRight) {
            fragmentTransaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left);
        } else {

            fragmentTransaction.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right);
        }

        fragmentTransaction.hide(fragment);
        fragmentTransaction.commitAllowingStateLoss();
    }

    protected void hideFragment(android.support.v4.app.Fragment fragment) {
        if (null == fragment) {
            return;
        }

        android.support.v4.app.FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.hide(fragment);
        fragmentTransaction.commitAllowingStateLoss();
    }

    @Override
    public void showFragment(String tag) {
        if (tag.equalsIgnoreCase(DashboardFragment.TAG)) {
            showDashboardFragment();
        } else if (tag.equalsIgnoreCase(ProximityFragment.TAG)) {
            showProximityFragment();
        } else if (tag.equalsIgnoreCase(GameBoardFragment.TAG)) {
            showGameBoardFragment();
        } else if (tag.equalsIgnoreCase(LanGameFragment.TAG)) {
            showLanGameFragment();
        }
    }

    @Override
    public GamesClient getGamesClientPublic() {
        return getGamesClient();
    }

    @Override
    public boolean isSignedInPublic() {
        return isSignedIn();
    }

    @Override
    public void showAlertPublic(String message) {
        showAlert(message);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (KeyEvent.KEYCODE_BACK == keyCode) {
            if (!currentFragmentTag.equalsIgnoreCase(DashboardFragment.TAG)) {
                showDashboardFragment();
                return true;
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        MyApplication.getPeersMgr().stop();
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /* Called whenever we call invalidateOptionsMenu() */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // If the nav drawer is open, hide action items related to the content view
        boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);
        menu.findItem(R.id.action_websearch).setVisible(!drawerOpen);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
         // The action bar home/up action should open or close the drawer.
         // ActionBarDrawerToggle will take care of this.
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        // Handle action buttons
        switch(item.getItemId()) {
        case R.id.action_websearch:
            // create intent to perform web search for this planet
            Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
            intent.putExtra(SearchManager.QUERY, getActionBar().getTitle());
            // catch event that there's no activity to handle intent
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                Toast.makeText(this, R.string.app_not_available, Toast.LENGTH_LONG).show();
            }
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    /* The click listner for ListView in the navigation drawer */
    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            selectItem(position);
        }
    }

    private void selectItem(int position) {
        // update the main content by replacing fragments
        Fragment fragment = new PlanetFragment();
        Bundle args = new Bundle();
        args.putInt(PlanetFragment.ARG_PLANET_NUMBER, position);
        fragment.setArguments(args);

        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();

        // update selected item and title, then close the drawer
        mDrawerList.setItemChecked(position, true);
        setTitle(mPlanetTitles[position]);
        mDrawerLayout.closeDrawer(mDrawerList);
    }

    @Override
    public void setTitle(CharSequence title) {
        mTitle = title;
        getActionBar().setTitle(mTitle);
    }

    /**
     * When using the ActionBarDrawerToggle, you must call it during
     * onPostCreate() and onConfigurationChanged()...
     */

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggls
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    /**
     * Fragment that appears in the "content_frame", shows a planet
     */
    public static class PlanetFragment extends Fragment {
        public static final String ARG_PLANET_NUMBER = "planet_number";

        public PlanetFragment() {
            // Empty constructor required for fragment subclasses
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_planet, container, false);
            int i = getArguments().getInt(ARG_PLANET_NUMBER);
            String planet = getResources().getStringArray(R.array.planets_array)[i];

            int imageId = getResources().getIdentifier(planet.toLowerCase(Locale.getDefault()),
                            "drawable", getActivity().getPackageName());
            ((ImageView) rootView.findViewById(R.id.image)).setImageResource(imageId);
            getActivity().setTitle(planet);
            return rootView;
        }
    }


    @Override
    public void onClick(View v) {
        int id = v.getId();

        switch (id) {
            case R.id.user_icon:
            case R.id.user_info: {
                showGameBoardFragment();

                break;
            }

            case R.id.button_sign_in:
                // start the sign-in flow
                beginUserInitiatedSignIn();
                break;

            case R.id.button_sign_out:
                // sign out.
                signOut();
                showSignInBar();
                break;

            case R.id.share_textview: {
                final String text = String.format(getString(R.string.share_text_full_total), appPreference.getTotalNumber());
                final String filename = ScreenshotUtils.getshotFilePath();
                ScreenshotUtils.shotBitmap(MainActivity.this, filename);
                Animation animation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_out);
                animation.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        Utils.share(MainActivity.this, MainActivity.this.getString(R.string.app_name), text, filename);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
                share.startAnimation(animation);
                break;
            }
        }
    }
}