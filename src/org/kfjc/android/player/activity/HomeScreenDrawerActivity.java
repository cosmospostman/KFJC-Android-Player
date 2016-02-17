package org.kfjc.android.player.activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import org.kfjc.android.player.R;
import org.kfjc.android.player.control.PreferenceControl;
import org.kfjc.android.player.fragment.LiveStreamFragment;
import org.kfjc.android.player.fragment.PlaylistFragment;
import org.kfjc.android.player.model.Playlist;
import org.kfjc.android.player.model.PlaylistJsonImpl;
import org.kfjc.android.player.service.PlaylistService;
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
    private NavigationView navigationView;

    private ServiceConnection playlistServiceConnection;
    private PlaylistService playlistService;
    private Intent playlistServiceIntent;

    private View view;
    private Snackbar snackbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        setContentView(R.layout.activity_home_screen_drawer);
        view = findViewById(R.id.home_screen_main_content);

        setupPlaylistService();
        streamServiceIntent = new Intent(this, StreamService.class);
        startService(streamServiceIntent);

        this.liveStreamFragment = new LiveStreamFragment();
        this.playlistFragment = new PlaylistFragment();

        setupDrawer();
        setupStreamService();
        setupListenersAndManagers();

        loadFragment(0);
    }

    private void setupPlaylistService() {
        if (playlistServiceConnection == null) {
            playlistServiceConnection = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    PlaylistService.PlaylistBinder binder = (PlaylistService.PlaylistBinder) service;
                    playlistService = binder.getService();
                    playlistService.start();
                    playlistService.registerPlaylistCallback(new PlaylistService.PlaylistCallback() {
                        @Override
                        public void onPlaylistUpdate(Playlist playlist) {
                            liveStreamFragment.updatePlaylist(playlist);
                            playlistFragment.updatePlaylist(playlist);
                            if (streamService != null && streamService.isPlaying()) {
                                notificationUtil.updateNowPlayNotification(playlist);
                            }
                        }
                    });
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {}
            };
        }

        playlistServiceIntent = new Intent(this, PlaylistService.class);
        startService(playlistServiceIntent);
        this.notificationUtil = new NotificationUtil(this);
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
        public void onBuffer() {
            liveStreamFragment.setState(LiveStreamFragment.PlayerState.BUFFER);
        }

        @Override
        public void onPlay() {
            liveStreamFragment.setState(LiveStreamFragment.PlayerState.PLAY);
            notificationUtil.updateNowPlayNotification(playlistService.getPlaylist());
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

    private void setupDrawer() {
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {
                drawerLayout.closeDrawers();
                item.setChecked(true);
                loadFragment(item.getItemId());
                return false;
            }
        });
        loadFragment(R.id.nav_livestream);
        navigationView.setCheckedItem(R.id.nav_livestream);

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

    private void loadFragment(int navItemId) {
        switch (navItemId) {
            case R.id.nav_livestream:
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.home_screen_main_fragment, liveStreamFragment)
                        .addToBackStack(null)
                        .commit();
                // streamService is null while still connecting at application launch
                if (streamService != null) {
                    liveStreamFragment.setState(streamService.getPlayerState());
                }
                break;
            case R.id.nav_playlist:
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.home_screen_main_fragment, playlistFragment)
                        .addToBackStack(null)
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
        if (!isStreamServicePlaying()) {
            playlistService.stop();
        }
        isForegroundActivity = false;
    }

    @Override
    protected void onStart() {
        super.onStart();
        bindService(playlistServiceIntent, playlistServiceConnection, Context.BIND_AUTO_CREATE);
        bindService(streamServiceIntent, streamServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(streamServiceConnection);
        unbindService(playlistServiceConnection);
    }

    @Override
    public void onBackPressed() {
        // First transition was loading the first fragment.
        // TODO: don't add first transition to stack.
        if (getSupportFragmentManager().getBackStackEntryCount() > 1 ){
            getSupportFragmentManager().popBackStack();
        } else {
            // Don't quit when back is pressed
            moveTaskToBack(true);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        preferenceControl = new PreferenceControl(getApplicationContext(),
                HomeScreenDrawerActivity.this);
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
    }

    @Override
    public void stopStream() {
        liveStreamFragment.setState(LiveStreamFragment.PlayerState.STOP);
        streamService.stop();
        audioManager.abandonAudioFocus(audioFocusListener);
        notificationUtil.cancelNowPlayNotification();
        if (!isForegroundActivity) {
            playlistService.stop();
        }
    }

    @Override
    public void restartStream() {
        streamService.reload(getApplicationContext(), PreferenceControl.getUrlPreference());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        notificationUtil.cancelNowPlayNotification();
        stopStream();
        stopService(streamServiceIntent);
        stopService(playlistServiceIntent);
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
    }

    @Override
    public boolean isStreamServicePlaying() {
        return streamService.isPlaying();
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

    @Override
    public Playlist getLatestPlaylist() {
        if (playlistService == null) {
            return new PlaylistJsonImpl("");
        }
        return playlistService.getPlaylist();
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
