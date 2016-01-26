package org.kfjc.android.player.activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;

import org.kfjc.android.player.R;
import org.kfjc.android.player.control.PreferenceControl;
import org.kfjc.android.player.fragment.LiveStreamFragment;
import org.kfjc.android.player.fragment.PlaylistFragment;
import org.kfjc.android.player.service.StreamService;
import org.kfjc.android.player.util.GraphicsUtil;
import org.kfjc.android.player.util.NotificationUtil;

import java.util.Calendar;

public class HomeScreenDrawerActivity extends AppCompatActivity implements HomeScreenInterface {

    private boolean isForegroundActivity = false;
    private ServiceConnection streamServiceConnection;
    private Intent streamServiceIntent;

    public static PreferenceControl preferenceControl;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle drawerToggle;
    private StreamService streamService;

    private NotificationUtil notificationUtil;
    private AudioManager audioManager;
    private TelephonyManager telephonyManager;
    private AudioManager.OnAudioFocusChangeListener audioFocusListener;
    private PhoneStateListener phoneStateListener;

    private LiveStreamFragment liveStreamFragment;
    private PlaylistFragment playlistFragment;
    private ListView navigationListView;

    private View view;
    private Snackbar snackbar;

    public enum NavItem {
        LIVESTREAM(R.drawable.ic_radio_black_24dp, R.string.nav_livestream),
        PLAYLIST(R.drawable.ic_book_black_24dp, R.string.nav_playlists);
        static final NavItem[] ORDER = new NavItem[] {LIVESTREAM, PLAYLIST};

        int icon;
        int label;

        NavItem(int icon, int label) {
            this.icon = icon;
            this.label = label;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        setContentView(R.layout.activity_home_screen_drawer);
        view = findViewById(R.id.home_screen_main_content);

        this.liveStreamFragment = new LiveStreamFragment();
        this.playlistFragment = new PlaylistFragment();

        setupDrawer();
        setupStreamService();
        setupListenersAndManagers();

        streamServiceIntent = new Intent(this, StreamService.class);
        startService(streamServiceIntent);

        loadFragment(0);
    }

    private void setupStreamService() {
        if (streamServiceConnection == null) {
            streamServiceConnection = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    StreamService.LiveStreamBinder binder = (StreamService.LiveStreamBinder) service;
                    streamService = binder.getService();
                    streamService.setMediaEventListener(mediaEventHandler);
                    liveStreamFragment.setState(streamService.getPlayerState());
                }

                @Override
                public void onServiceDisconnected(ComponentName arg0) {
                }
            };
        }
    }

    private StreamService.MediaListener mediaEventHandler = new StreamService.MediaListener() {
        @Override
        public void onBuffer() {}

        @Override
        public void onPlay() {
            liveStreamFragment.setState(LiveStreamFragment.PlayerState.PLAY);
        }

        @Override
        public void onError(String info) {
            stopStream();
            String message = getString(R.string.error_generic);
            if (info.contains("HttpDataSourceException")) {
                message = getString(R.string.error_unable_to_connect);
            }
            snack(message, Snackbar.LENGTH_LONG);
        }

        @Override
        public void onEnd() {
            liveStreamFragment.setState(LiveStreamFragment.PlayerState.STOP);
        }
    };

    private void setupListenersAndManagers() {
        this.notificationUtil = new NotificationUtil(this);
        this.audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        this.telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        this.audioFocusListener = EventHandlerFactory.onAudioFocusChange(this, audioManager);
        this.phoneStateListener = EventHandlerFactory.onPhoneStateChange(this);
    }

    private void setupStreams() {
        new AsyncTask<Void, Void, Void>() {
            @Override protected Void doInBackground(Void... unsedParams) {
                preferenceControl = new PreferenceControl(getApplicationContext(),
                        HomeScreenDrawerActivity.this);
                liveStreamFragment.setState(LiveStreamFragment.State.LOADING_STREAMS);
                return null;
            }

            @Override protected void onPostExecute(Void aVoid) {
                liveStreamFragment.setState(LiveStreamFragment.State.CONNECTED);
            }
        }.execute();
    }

    private void setupDrawer() {
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        navigationListView = (ListView) findViewById(R.id.navlist);
        navigationListView.setVerticalFadingEdgeEnabled(true);

        NavigationListAdapter adapter = new NavigationListAdapter(
                getApplicationContext(), NavItem.ORDER);
        navigationListView.setAdapter(adapter);
        navigationListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                loadFragment(position);
                drawerLayout.closeDrawers();
            }
        });
        navigationListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        navigationListView.setItemChecked(0, true);

        Toolbar toolbar = (Toolbar) findViewById(R.id.home_screen_toolbar);
        setSupportActionBar(toolbar);
        drawerToggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close
        );

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        drawerToggle.setDrawerIndicatorEnabled(true);
        drawerLayout.setDrawerListener(drawerToggle);
    }

    private void loadFragment(int navItemIndex) {
        navigationListView.setSelection(navItemIndex);
        NavItem navItem = NavItem.ORDER[navItemIndex];

        switch (navItem) {
            case LIVESTREAM:
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.home_screen_main_fragment, liveStreamFragment)
                        .commit();
                break;
            case PLAYLIST:
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.home_screen_main_fragment, playlistFragment)
                        .commit();
                break;
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
    }

    @Override
    protected void onPause() {
        super.onPause();
        isForegroundActivity = false;
    }

    @Override
    protected void onStart() {
        super.onStart();
        bindService(streamServiceIntent, streamServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(streamServiceConnection);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupStreams();
        isForegroundActivity = true;
        int hourOfDay = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        ImageView backgroundImageView = (ImageView) findViewById(R.id.backgroundImageView);
        backgroundImageView.setImageResource(GraphicsUtil.imagesOfTheHour[hourOfDay]);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public void setActionbarTitle(String title) {
        getSupportActionBar().setTitle(title);
    }

    @Override
    public void playStream() {
        audioManager.requestAudioFocus(
                audioFocusListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN);
        streamService.play(getApplicationContext(), PreferenceControl.getUrlPreference());
        liveStreamFragment.setState(LiveStreamFragment.PlayerState.BUFFER);
    }

    @Override
    public void stopStream() {
        liveStreamFragment.setState(LiveStreamFragment.PlayerState.STOP);
        streamService.stop();
        audioManager.abandonAudioFocus(audioFocusListener);
        notificationUtil.cancelNowPlayNotification();
        if (!isForegroundActivity) {
            liveStreamFragment.stopPlaylistService();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        notificationUtil.cancelNowPlayNotification();
        stopStream();
        stopService(streamServiceIntent);
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
    }

    @Override
    public boolean isStreamServicePlaying() {
        return false;
    }

    @Override
    public void snack(String message, int snackbarLength) {
        snackDone();
        snackbar = Snackbar.make(view, message, snackbarLength);
        snackbar.show();
    }

    @Override
    public void snackDone() {
        if (snackbar != null) {
            snackbar.dismiss();
        }
    }

    private int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }
}
