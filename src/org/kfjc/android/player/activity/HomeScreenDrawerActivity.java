package org.kfjc.android.player.activity;

import android.Manifest;
import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

import org.kfjc.android.player.KfjcApplication;
import org.kfjc.android.player.R;
import org.kfjc.android.player.control.PreferenceControl;
import org.kfjc.android.player.fragment.LiveStreamFragment;
import org.kfjc.android.player.fragment.PlaylistFragment;
import org.kfjc.android.player.fragment.PodcastFragment;
import org.kfjc.android.player.model.Playlist;
import org.kfjc.android.player.model.PlaylistJsonImpl;
import org.kfjc.android.player.service.PlaylistService;
import org.kfjc.android.player.service.StreamService;
import org.kfjc.android.player.util.HttpUtil;
import org.kfjc.android.player.util.NotificationUtil;

import java.util.Calendar;

public class HomeScreenDrawerActivity extends AppCompatActivity implements HomeScreenInterface {

    private static final String KEY_ACTIVE_FRAGMENT = "active-fragment";
    private static final int KFJC_PERM_READ_PHONE_STATE = 0;

    private KfjcApplication application;

    private ServiceConnection streamServiceConnection;
    private StreamService streamService;
    private Intent streamServiceIntent;

    private ServiceConnection playlistServiceConnection;
    private PlaylistService playlistService;
    private Intent playlistServiceIntent;

    private NotificationUtil notificationUtil;

    private AudioManager audioManager;
    private TelephonyManager telephonyManager;
    private AudioManager.OnAudioFocusChangeListener audioFocusListener;
    private PhoneStateListener phoneStateListener;

    private LiveStreamFragment liveStreamFragment;
    private PlaylistFragment playlistFragment;
    private PodcastFragment podcastFragment;
    private NavigationView navigationView;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle drawerToggle;
    private View view;
    private Snackbar snackbar;

    private boolean isForegroundActivity = false;
    private int activeFragmentId = R.id.nav_livestream;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        setContentView(R.layout.activity_home_screen_drawer);
        view = findViewById(R.id.home_screen_main_content);
        application = (KfjcApplication) getApplicationContext();

        if (savedInstanceState != null) {
            activeFragmentId = savedInstanceState.getInt(KEY_ACTIVE_FRAGMENT);
        }

        HttpUtil.installCache(getApplicationContext());
        loadResources();

        setupPlaylistService();
        streamServiceIntent = new Intent(this, StreamService.class);
        startService(streamServiceIntent);

        this.liveStreamFragment = new LiveStreamFragment();
        this.playlistFragment = new PlaylistFragment();
        this.podcastFragment = new PodcastFragment();

        setupDrawer();
        setupStreamService();
        setupListenersAndManagers();
    }

    private void loadResources() {
        application.loadResources(new KfjcApplication.ResourcesLoadedHandler() {
            @Override
            public void onResourcesLoaded() {
                updateBackground();
            }
        });
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

        maybeAddPhoneStateListener();
        this.audioFocusListener = EventHandlerFactory.onAudioFocusChange(this, audioManager);
    }

    private void maybeAddPhoneStateListener() {
        if (hasPhonePermission()) {
            this.telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            this.phoneStateListener = EventHandlerFactory.onPhoneStateChange(this);
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        } else {
            requestPhonePermission();
        }
    }

    private boolean hasPhonePermission() {
        return ContextCompat.checkSelfPermission(
                this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPhonePermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.READ_PHONE_STATE)) {
            AlertDialog alertDialog = new AlertDialog.Builder(this, R.style.KfjcDialog).create();
            alertDialog.setTitle(R.string.permission_phone_title);
            alertDialog.setMessage(getString(R.string.permission_phone_rationale));
            alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(android.R.string.ok),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            requestAndroidPhonePermissions();
                        }
                    });
            alertDialog.show();
        } else {
            requestAndroidPhonePermissions();
        }
    }

    private void requestAndroidPhonePermissions() {
        ActivityCompat.requestPermissions(
                HomeScreenDrawerActivity.this,
                new String[]{Manifest.permission.READ_PHONE_STATE},
                KFJC_PERM_READ_PHONE_STATE);
    }

    private boolean askPermissionsAgain = true;
    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case KFJC_PERM_READ_PHONE_STATE:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Got permissions
                    maybeAddPhoneStateListener();
                } else {
                    // Permission not granted
                    if (askPermissionsAgain) {
                        askPermissionsAgain = false;
                        requestPhonePermission();
                    }
                }
                return;
        }
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
        loadFragment(activeFragmentId);
    }

    public void setNavigationItemChecked(int navigationItemId) {
        navigationView.setCheckedItem(navigationItemId);
    }

    private void loadFragment(int fragmentId) {
        activeFragmentId = fragmentId;
        switch (fragmentId) {
            case R.id.nav_livestream:
                replaceFragment(liveStreamFragment);
                if (playlistService != null) {
                    liveStreamFragment.updatePlaylist(playlistService.getPlaylist());
                }
                // streamService is null while still connecting at application launch
                if (streamService != null) {
                    liveStreamFragment.setState(streamService.getPlayerState());
                }
                break;
            case R.id.nav_playlist:
                replaceFragment(playlistFragment);
                drawerToggle.syncState();
                if (playlistService != null) {
                    playlistFragment.updatePlaylist(playlistService.getPlaylist());
                }
                break;
            case R.id.nav_podcast:
                replaceFragment(podcastFragment);
                break;
        }
    }

    private void replaceFragment(Fragment fragment) {
        getFragmentManager().beginTransaction()
                .replace(R.id.home_screen_main_fragment, fragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (!isStreamServicePlaying() && playlistService != null) {
            playlistService.stop();
        }
        isForegroundActivity = false;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(KEY_ACTIVE_FRAGMENT, activeFragmentId);
        super.onSaveInstanceState(outState);
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
        // Don't pop back to no active fragment
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
        loadResources();
        PreferenceControl.updateStreams();
        isForegroundActivity = true;
        updateBackground();
    }

    @Override
    public void updateBackground() {
        final ImageView backgroundImageView = (ImageView) findViewById(R.id.backgroundImageView);
        if (!PreferenceControl.areBackgroundsEnabled()) {
            backgroundImageView.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.bg_default));
        } else {
            int hourOfDay = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
            Futures.addCallback(application.getKfjcResources().getBackgroundImage(hourOfDay),
                    new FutureCallback<Drawable>() {
                @Override
                public void onSuccess(final Drawable backgroundImage) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                        backgroundImageView.setImageDrawable(backgroundImage);
                        }
                    });
                }

                @Override
                public void onFailure(Throwable t) {
                }
            });
        }
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
        streamService.play(getApplicationContext(), PreferenceControl.getStreamPreference());
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
        streamService.reload(getApplicationContext(), PreferenceControl.getStreamPreference());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        notificationUtil.cancelNowPlayNotification();
        stopStream();
        stopService(streamServiceIntent);
        stopService(playlistServiceIntent);
        if (telephonyManager != null) {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
        }
    }

    @Override
    public boolean isStreamServicePlaying() {
        if (streamService == null) {
            return false;
        }
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
}
