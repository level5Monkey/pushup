package me.pjq.pushup.navigation;


import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.*;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.*;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.*;
import android.widget.*;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.games.GamesClient;
import com.google.android.gms.games.Player;
import com.google.example.games.basegameutils.BaseGameActivity;
import com.squareup.otto.Bus;
import com.tencent.mm.sdk.openapi.*;
import me.pjq.pushup.*;
import me.pjq.pushup.Constants;
import me.pjq.pushup.activity.*;
import me.pjq.pushup.adapter.DrawerListAdapter;
import me.pjq.pushup.fragment.*;
import me.pjq.pushup.msg.MsgSignIn;
import me.pjq.pushup.msg.MsgSignOut;
import me.pjq.pushup.slidingmenu.BaseActivity;
import me.pjq.pushup.utils.*;
import me.pjq.pushup.R;
import net.sourceforge.simcpux.*;

import java.util.ArrayList;

public class MainActivity extends BaseGameActivity implements View.OnClickListener, FragmentController, IWXAPIEventHandler {
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private ActionBarDrawerToggle mDrawerToggle;

    private CharSequence mDrawerTitle;
    private CharSequence mTitle;
//    private String[] mDrawerItems;

    private static final String TAG = MainActivity.class.getSimpleName();
    private TextView userInfo;
    private ImageView userIcon;
    private TextView share;

    Bus bus;
    AppPreference appPreference;

    int CONTENT_VIEW_ID;
    DashboardFragment dashboardFragment;
    PushupsFragment proximityFragment;

    private String currentFragmentTag;
    private TitlebarHelper titlebarHelper;
    private DrawerListAdapter drawerListAdapter;
    private ArrayList<Object> drawerItemArrayList;

    WeChatUtils weChatUtils;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_drawer);
        CONTENT_VIEW_ID = R.id.content_frame;

        Utils.showUserGuardIfNeed(this, UserGuideActivity.START_FROM_SPLASH);

        mTitle = mDrawerTitle = getTitle();
//        mDrawerItems = getResources().getStringArray(R.array.drawer_items_array);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);

        // set a custom shadow that overlays the main content when the drawer opens
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        // set up the drawer's list view with items and click listener
        drawerListAdapter = new DrawerListAdapter(getApplicationContext());
        drawerItemArrayList = new ArrayList<Object>();
        drawerItemArrayList.add(new DrawerListAdapter.DrawerItem(Constants.DRAWER_ITEM_DASHBOARD, getString(R.string.menu_item_dashboard)));
        drawerItemArrayList.add(new DrawerListAdapter.DrawerItem(Constants.DRAWER_ITEM_PUSHUPS, getString(R.string.menu_item_pushup)));
        drawerItemArrayList.add(new DrawerListAdapter.DrawerItem(Constants.DRAWER_ITEM_MULTI, getString(R.string.menu_item_multi)));
        drawerItemArrayList.add(new DrawerListAdapter.DrawerItem(Constants.DRAWER_ITEM_WRIST, getString(R.string.menu_item_wrist)));
        drawerItemArrayList.add(new DrawerListAdapter.DrawerItem(Constants.DRAWER_ITEM_GOOGLE, getString(R.string.menu_item_google)));
        drawerItemArrayList.add(new DrawerListAdapter.DrawerItem(Constants.DRAWER_ITEM_LEADERBOARD, getString(R.string.menu_item_leaderboard)));
        drawerItemArrayList.add(new DrawerListAdapter.DrawerItem(Constants.DRAWER_ITEM_ARCHIEVEMENT, getString(R.string.menu_item_archievement)));
        drawerItemArrayList.add(new DrawerListAdapter.DrawerItem(Constants.DRAWER_ITEM_HELPER, getString(R.string.menu_item_helper)));
        drawerItemArrayList.add(new DrawerListAdapter.DrawerItem(Constants.DRAWER_ITEM_ABOUT, getString(R.string.menu_item_about)));

        drawerListAdapter.setDataList(drawerItemArrayList);
        mDrawerList.setAdapter(drawerListAdapter);

//        mDrawerList.setAdapter(new ArrayAdapter<String>(this,
//                R.layout.drawer_list_item, mDrawerItems));
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

        // enable ActionBar app icon to behave as action to toggle nav drawer
        getActionBarImpl().setDisplayHomeAsUpEnabled(true);
        getActionBarImpl().setHomeButtonEnabled(true);


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
                getActionBarImpl().setTitle(mTitle);
                supportInvalidateOptionsMenu();
//                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            public void onDrawerOpened(View drawerView) {
                getActionBarImpl().setTitle(mDrawerTitle);
                supportInvalidateOptionsMenu();
//                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        if (savedInstanceState == null) {
            selectItem((DrawerListAdapter.DrawerItem) drawerListAdapter.getItem(0));
        } else {
            selectItem((DrawerListAdapter.DrawerItem) drawerListAdapter.getItem(0));
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

//        if (!isSignedIn()) {
//            beginUserInitiatedSignIn();
//        }

//        showDashboardFragment();
        initWeChat();
    }

    private void initWeChat() {
        weChatUtils = WeChatUtils.getInstance(this);
        if (weChatUtils.getIWXAPI().isWXAppInstalled()) {
            weChatUtils.register();
            weChatUtils.getIWXAPI().handleIntent(getIntent(), this);
        }
    }

    private void showWX() {
//        Intent intent = new Intent();
//        intent.setClass(this, WXEntryActivity.class);
//        startActivity(intent);


    }

    public void showPushupFragment() {
        updateItemSelected(Constants.DRAWER_ITEM_PUSHUPS);
        currentFragmentTag = PushupsFragment.TAG;
        android.support.v4.app.Fragment fragment = findFragmentByTag(PushupsFragment.TAG);
        hideTheOtherFragment();

        if (null == fragment) {
            fragment = PushupsFragment.newInstance(new Bundle());
//            replaceChildFragment(fragment, PushupsFragment.TAG, fromLeft2Right());
            addChildFragment(fragment, PushupsFragment.TAG, fromLeft2Right());

        } else {
            showFragment(fragment, PushupsFragment.TAG, fromLeft2Right());
        }

        notifyFragmentChangeAll(PushupsFragment.TAG);
    }

    public void showGameBoardFragment() {
        updateItemSelected(Constants.DRAWER_ITEM_LEADERBOARD);
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
        updateItemSelected(Constants.DRAWER_ITEM_DASHBOARD);
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

    public void showMultiPlayerGameFragment() {
        updateItemSelected(Constants.DRAWER_ITEM_MULTI);
        currentFragmentTag = MultiPlayerFragment.TAG;
        android.support.v4.app.Fragment fragment = findFragmentByTag(MultiPlayerFragment.TAG);
        hideTheOtherFragment();

        if (null == fragment) {
            fragment = MultiPlayerFragment.newInstance(new Bundle());
//            replaceChildFragment(fragment, MultiPlayerFragment.TAG, fromLeft2Right());
            addChildFragment(fragment, MultiPlayerFragment.TAG, fromLeft2Right());

        } else {
            showFragment(fragment, MultiPlayerFragment.TAG, fromLeft2Right());
        }

        notifyFragmentChangeAll(MultiPlayerFragment.TAG);
    }

    public void showWristGameFragment() {
        updateItemSelected(Constants.DRAWER_ITEM_WRIST);
        currentFragmentTag = WristGameFragment.TAG;
        android.support.v4.app.Fragment fragment = findFragmentByTag(WristGameFragment.TAG);
        hideTheOtherFragment();

        if (null == fragment) {
            fragment = WristGameFragment.newInstance(new Bundle());
//            replaceChildFragment(fragment, MultiPlayerFragment.TAG, fromLeft2Right());
            addChildFragment(fragment, WristGameFragment.TAG, fromLeft2Right());

        } else {
            showFragment(fragment, WristGameFragment.TAG, fromLeft2Right());
        }

        notifyFragmentChangeAll(WristGameFragment.TAG);
    }

    private void notifyFragmentChange(String fragmentTag, String tag) {
        android.support.v4.app.Fragment fragment = findFragmentByTag(fragmentTag);
        if (null != fragment) {
            ((FragmentBridge) fragment).changeToFragment(tag);
        }
    }

    private void notifyFragmentChangeAll(String tag) {
        notifyFragmentChange(PushupsFragment.TAG, tag);
        notifyFragmentChange(DashboardFragment.TAG, tag);
        notifyFragmentChange(GameBoardFragment.TAG, tag);
        notifyFragmentChange(MultiPlayerFragment.TAG, tag);
        notifyFragmentChange(WristGameFragment.TAG, tag);

//        updateShareIntent();
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
        android.support.v4.app.Fragment fragment = findFragmentByTag(PushupsFragment.TAG);
        hideFragment(fragment, fromLeft2Right());

        fragment = findFragmentByTag(DashboardFragment.TAG);
        hideFragment(fragment, fromLeft2Right());

        fragment = findFragmentByTag(GameBoardFragment.TAG);
        hideFragment(fragment, fromLeft2Right());

        fragment = findFragmentByTag(MultiPlayerFragment.TAG);
        hideFragment(fragment, fromLeft2Right());

        fragment = findFragmentByTag(WristGameFragment.TAG);
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

        int numberOfTimes = AppPreference.getInstance(getApplicationContext()).getNumberOfTimes();
        getGamesClient().submitScore(getString(R.string.leaderboard_number_of_times), numberOfTimes);
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

        bus.post(new MsgSignOut());
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

        bus.post(new MsgSignIn());
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
        //userIcon.setImageURI(uri);
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
        } else if (tag.equalsIgnoreCase(PushupsFragment.TAG)) {
            showPushupFragment();
        } else if (tag.equalsIgnoreCase(MultiPlayerFragment.TAG)) {
            showMultiPlayerGameFragment();
        } else if (tag.equalsIgnoreCase(WristGameFragment.TAG)) {
            showWristGameFragment();
        } else if (tag.equalsIgnoreCase(GameBoardFragment.TAG)) {
            showGameBoardFragment();
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
            } else {
                pressAgainToExit();
                return true;
            }
        }

        return super.onKeyDown(keyCode, event);
    }


    private long lastPressBackTime;
    private static final int PRESS_AGAIN_LIMIT_DURATION = 3;

    private void pressAgainToExit() {
        long current = System.currentTimeMillis();
        long duration = (current - lastPressBackTime) / 1000;

        if (duration <= PRESS_AGAIN_LIMIT_DURATION) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

//                    exitApp();
                    finish();
                }
            });
        } else {
            lastPressBackTime = current;
            ToastUtil.showToast(getApplicationContext(), getString(R.string.press_again_to_exit));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        exitApp();
    }

    private void exitApp() {
        MyApplication.getPeersMgr().stop();
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    MyShareActionProvider mShareActionProvider;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        if (weChatUtils.getIWXAPI().isWXAppInstalled()) {
            inflater.inflate(R.menu.main_wechat, menu);
        } else {
            inflater.inflate(R.menu.main, menu);
        }

        MenuItem shareItem = menu.findItem(R.id.action_share);
        android.support.v4.view.ActionProvider actionProvider = MenuItemCompat.getActionProvider(shareItem);
        if (null != actionProvider && actionProvider instanceof ShareActionProvider) {
            mShareActionProvider.setShareHistoryFileName(ShareActionProvider.DEFAULT_SHARE_HISTORY_FILE_NAME);
            //mShareActionProvider.setShareIntent(Utils.getShareRawIntent(this));

            mShareActionProvider.setOnShareTargetSelectedListener(new ShareActionProvider.OnShareTargetSelectedListener() {
                @Override
                public boolean onShareTargetSelected(ShareActionProvider shareActionProvider, Intent intent) {
                    updateShareIntent();
                    return false;
                }
            });
            updateShareIntent();

            for (int i = 0; i < menu.size(); i++) {
                MenuItem item = menu.getItem(i);
                if (item.getItemId() == R.id.action_share) {
                    View itemChooser = MenuItemCompat.getActionView(item);
                    if (itemChooser != null) {
                        itemChooser.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                EFLogger.i(TAG, "onClick");
                                ScreenshotUtils.shotBitmap(MainActivity.this, shareFileName);
                            }
                        });
                    }
                }
            }
        }

        return super.onCreateOptionsMenu(menu);
    }

    /**
     * Defines a default (dummy) share intent to initialize the action provider.
     * However, as soon as the actual content to be used in the intent
     * is known or changes, you must update the share intent by again calling
     * mShareActionProvider.setShareIntent()
     */
    private Intent updateShareIntent() {
        if (null == appPreference) {
            return null;
        }

        final String text = String.format(getString(R.string.share_text_full_total), appPreference.getTotalNumber());
        takeScreenshot();
        Intent intent = Utils.getShareIntent(MainActivity.this, MainActivity.this.getString(R.string.app_name), text, shareFileName);

        if (null != mShareActionProvider) {
//            mShareActionProvider.setShareIntent(Utils.getShareRawIntent(this));
            mShareActionProvider.setShareIntent(intent);
        }

        return intent;
    }

    private void takeScreenshot() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ScreenshotUtils.shotBitmap(MainActivity.this, shareFileName);
            }
        });
    }

    /* Called whenever we call invalidateOptionsMenu() */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // If the nav drawer is open, hide action items related to the content view
        boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);
//        menu.findItem(R.id.action_share).setVisible(!drawerOpen);
//        menu.findItem(R.id.action_about).setVisible(!drawerOpen);
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
        switch (item.getItemId()) {

            case R.id.action_share:
//                updateShareIntent();
                showShare();
                return true;

            case R.id.action_share_wechat:
//                updateShareIntent();
                showShareWeChat();
                return true;
//            case R.id.action_about:
//                showAbout();
//                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /* The click listner for ListView in the navigation drawer */
    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            DrawerListAdapter.DrawerItem drawerItem = (DrawerListAdapter.DrawerItem) parent.getAdapter().getItem(position);
            selectItem(drawerItem);
        }
    }

    private void selectItem(DrawerListAdapter.DrawerItem drawerItem) {
        int position = drawerItem.getPosition();

        switch (position) {
            case Constants.DRAWER_ITEM_DASHBOARD:
                showDashboardFragment();
                break;

            case Constants.DRAWER_ITEM_PUSHUPS:
                showPushupFragment();
                break;

            case Constants.DRAWER_ITEM_MULTI:
                showMultiPlayerGameFragment();
                break;

            case Constants.DRAWER_ITEM_WRIST:
                showWristGameFragment();
                break;

            case Constants.DRAWER_ITEM_GOOGLE:
                showGameBoardFragment();
                break;

            case Constants.DRAWER_ITEM_LEADERBOARD:
                onShowLeaderboardsRequested();
                break;

            case Constants.DRAWER_ITEM_ARCHIEVEMENT:
                onShowAchievementsRequested();
                break;

            case Constants.DRAWER_ITEM_HELPER:
                showUserGuide();
                break;

            case Constants.DRAWER_ITEM_ABOUT:
                showAbout();
                break;
        }

        // update selected item and title, then close the drawer
        if (position == Constants.DRAWER_ITEM_ABOUT || position == Constants.DRAWER_ITEM_HELPER) {

        } else {
            updateItemSelected(drawerItem);
        }

        mDrawerLayout.closeDrawer(mDrawerList);
    }

    private void updateItemSelected(int position) {
        DrawerListAdapter.DrawerItem drawerItem = (DrawerListAdapter.DrawerItem) drawerItemArrayList.get(position);
        updateItemSelected(drawerItem);
    }

    private void updateItemSelected(DrawerListAdapter.DrawerItem drawerItem) {
        mDrawerList.setItemChecked(drawerItem.getPosition(), true);

        setTitle(drawerItem.getTitle());
    }

    private void showUserGuide() {
        Utils.showUserGuard(this, UserGuideActivity.START_FROM_SETTINGS);
        Utils.overridePendingTransitionRight2Left(this);
    }

    private void showAbout() {
//        Utils.openUrlInsideApp(this, getString(R.string.about_url));
//        Utils.overridePendingTransitionRight2Left(this);

        Intent intent = new Intent();
//        intent.setClass(this, AboutActivity.class);
        intent.setClass(this, BaseActivity.class);
        startActivity(intent);
        Utils.overridePendingTransitionRight2Left(this);
    }

    @Override
    public void setTitle(CharSequence title) {
        mTitle = title;
        getActionBarImpl().setTitle(mTitle);
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

    private String shareFileName = ScreenshotUtils.getshotFilePathByDay();

    private void showShare() {
        final String text = String.format(getString(R.string.share_text_full_total), appPreference.getTotalNumber());
//        ScreenshotUtils.shotBitmap(MainActivity.this, shareFileName);
//        Animation animation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_out);
//        animation.setAnimationListener(new Animation.AnimationListener() {
//            @Override
//            public void onAnimationStart(Animation animation) {
//            }
//
//            @Override
//            public void onAnimationEnd(Animation animation) {
//
//            }
//
//            @Override
//            public void onAnimationRepeat(Animation animation) {
//
//            }
//        });
//        share.startAnimation(animation);
        takeScreenshot();
        Utils.share(MainActivity.this, MainActivity.this.getString(R.string.app_name), text, shareFileName);
    }

    private void showShareWeChat() {
        if (true){
//            weChatUtils.showDemo(this);
//            return;
        }

        final String text = String.format(getString(R.string.share_text_full_total), appPreference.getTotalNumber());
        Bitmap bitmap = ScreenshotUtils.shotBitmap2(this, shareFileName);
//        weChatUtils.send(bitmap, MainActivity.this.getString(R.string.app_name), text, shareFileName);
        weChatUtils.createAppendReq(bitmap, this.getString(R.string.app_name), text, shareFileName);
//        weChatUtils.createAppendReq2(this.getString(R.string.app_name), text, shareFileName);

        Intent intent = new Intent();
        intent.setClass(this, me.pjq.pushup.activity.SendToWXActivity.class);
        startActivity(intent);

        Utils.overridePendingTransitionRight2Left(this);
    }

    private void sendToWeChat() {
        final String text = String.format(getString(R.string.share_text_full_total), appPreference.getTotalNumber());
        ScreenshotUtils.shotBitmap2(this, shareFileName);
        weChatUtils.createAppendReq2(this.getString(R.string.app_name), text, shareFileName);

        Intent intent = new Intent();
        intent.setClass(this, me.pjq.pushup.activity.SendToWXActivity.class);
        startActivity(intent);

        Utils.overridePendingTransitionRight2Left(this);
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
                beginUserInitiatedSignInImpl();
                break;

            case R.id.button_sign_out:
                // sign out.
                signOutImpl();
                showSignInBar();
                break;

            case R.id.share_textview: {
                showShare();
                break;
            }
        }
    }

    public void signOutImpl() {
        signOut();
    }

    public void beginUserInitiatedSignInImpl() {
        MainActivity.this.beginUserInitiatedSignIn();
    }

    // request codes we use when invoking an external activity
    final int RC_RESOLVE = 5000, RC_UNUSED = 5001;

    public void onShowAchievementsRequested() {
        if (isSignedInPublic()) {
            if (!currentFragmentTag.equalsIgnoreCase(GameBoardFragment.TAG)) {
                showGameBoardFragment();
            }
            startActivityForResult(getGamesClient().getAchievementsIntent(), RC_UNUSED);
            Utils.overridePendingTransitionRight2Left(this);
        } else {
            showAlertPublic(getString(R.string.achievements_not_available));
            if (!currentFragmentTag.equalsIgnoreCase(GameBoardFragment.TAG)) {
                showGameBoardFragment();
            }
        }
    }

    public void onShowLeaderboardsRequested() {
        if (isSignedInPublic()) {
            if (!currentFragmentTag.equalsIgnoreCase(GameBoardFragment.TAG)) {
                showGameBoardFragment();
            }
            startActivityForResult(getGamesClient().getAllLeaderboardsIntent(), RC_UNUSED);
            Utils.overridePendingTransitionRight2Left(this);
        } else {
            showAlertPublic(getString(R.string.leaderboards_not_available));
            if (!currentFragmentTag.equalsIgnoreCase(GameBoardFragment.TAG)) {
                showGameBoardFragment();
            }
        }
    }

    public void onShowInvitationRequested() {
        if (isSignedInPublic()) {
            startActivityForResult(getGamesClient().getInvitationInboxIntent(), RC_UNUSED);
            Utils.overridePendingTransitionRight2Left(this);
        } else {
            showAlertPublic(getString(R.string.invitation_not_available));
            if (!currentFragmentTag.equalsIgnoreCase(GameBoardFragment.TAG)) {
                showGameBoardFragment();
            }
        }
    }


    // 微信发送请求到第三方应用时，会回调到该方法
    @Override
    public void onReq(BaseReq req) {
        switch (req.getType()) {
            case ConstantsAPI.COMMAND_GETMESSAGE_FROM_WX:
                goToGetMsg();
                break;
            case ConstantsAPI.COMMAND_SHOWMESSAGE_FROM_WX:
                goToShowMsg((ShowMessageFromWX.Req) req);
                break;
            default:
                break;
        }
    }

    public void onResp(BaseResp resp) {
        int result = 0;

        switch (resp.errCode) {
            case BaseResp.ErrCode.ERR_OK:
                result = net.sourceforge.simcpux.R.string.errcode_success;
                break;
            case BaseResp.ErrCode.ERR_USER_CANCEL:
                result = net.sourceforge.simcpux.R.string.errcode_cancel;
                break;
            case BaseResp.ErrCode.ERR_AUTH_DENIED:
                result = net.sourceforge.simcpux.R.string.errcode_deny;
                break;
            default:
                result = net.sourceforge.simcpux.R.string.errcode_unknown;
                break;
        }

        Toast.makeText(this, result, Toast.LENGTH_LONG).show();
    }

    private void goToGetMsg() {
        Intent intent = new Intent(this, GetFromWXActivity.class);
        intent.putExtras(getIntent());
        startActivity(intent);
        //finish();
    }

    private void goToShowMsg(ShowMessageFromWX.Req showReq) {
        WXMediaMessage wxMsg = showReq.message;
        WXAppExtendObject obj = (WXAppExtendObject) wxMsg.mediaObject;

        StringBuffer msg = new StringBuffer(); // 组织一个待显示的消息内容
        msg.append("description: ");
        msg.append(wxMsg.description);
        msg.append("\n");
        msg.append("extInfo: ");
        msg.append(obj.extInfo);
        msg.append("\n");
        msg.append("filePath: ");
        msg.append(obj.filePath);

        Intent intent = new Intent(this, ShowFromWXActivity.class);
        intent.putExtra(net.sourceforge.simcpux.Constants.ShowMsgActivity.STitle, wxMsg.title);
        intent.putExtra(net.sourceforge.simcpux.Constants.ShowMsgActivity.SMessage, msg.toString());
        intent.putExtra(net.sourceforge.simcpux.Constants.ShowMsgActivity.BAThumbData, wxMsg.thumbData);
        startActivity(intent);
        //finish();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        setIntent(intent);
        weChatUtils.getIWXAPI().handleIntent(intent, this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {

            case 0x101: {
//                final WXAppExtendObject appdata = new WXAppExtendObject();
//                final String path = CameraUtil.getResultPhotoPath(this, data, SDCARD_ROOT + "/tencent/");
//                appdata.filePath = path;
//                appdata.extInfo = "this is ext info";
//
//                final WXMediaMessage msg = new WXMediaMessage();
//                msg.setThumbImage(Util.extractThumbNail(path, 150, 150, true));
//                msg.title = "this is title";
//                msg.description = "this is description";
//                msg.mediaObject = appdata;
//
//                SendMessageToWX.Req req = new SendMessageToWX.Req();
//                req.transaction = buildTransaction("appdata");
//                req.message = msg;
//                req.scene = isTimeline() ? SendMessageToWX.Req.WXSceneTimeline : SendMessageToWX.Req.WXSceneSession;
//                weChatUtils.getIWXAPI().sendReq(req);

                sendToWeChat();
                //finish();
                break;
            }
            default:
                break;
        }
    }

    private boolean isTimeline() {
        return true;
    }

    private String buildTransaction(final String type) {
        return (type == null) ? String.valueOf(System.currentTimeMillis()) : type + System.currentTimeMillis();
    }

}