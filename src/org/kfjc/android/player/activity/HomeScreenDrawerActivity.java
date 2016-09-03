package org.kfjc.android.player.activity;

import android.Manifest;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

import org.kfjc.android.player.KfjcApplication;
import org.kfjc.android.player.R;
import org.kfjc.android.player.control.PreferenceControl;
import org.kfjc.android.player.fragment.KfjcFragment;
import org.kfjc.android.player.fragment.LiveStreamFragment;
import org.kfjc.android.player.fragment.PodcastFragment;
import org.kfjc.android.player.fragment.PodcastPlayerFragment;
import org.kfjc.android.player.intent.PlayerState;
import org.kfjc.android.player.model.MediaSource;
import org.kfjc.android.player.model.Playlist;
import org.kfjc.android.player.model.PlaylistJsonImpl;
import org.kfjc.android.player.model.ShowDetails;
import org.kfjc.android.player.receiver.DownloadReceiver;
import org.kfjc.android.player.receiver.MediaStateReceiver;
import org.kfjc.android.player.service.PlaylistService;
import org.kfjc.android.player.service.StreamService;
import org.kfjc.android.player.util.DownloadUtil;
import org.kfjc.android.player.util.HttpUtil;
import org.kfjc.android.player.intent.PlayerControl;
import org.kfjc.android.player.util.NotificationUtil;

import java.io.IOException;
import java.util.Calendar;

public class HomeScreenDrawerActivity extends AppCompatActivity implements HomeScreenInterface {

    private static final String KEY_ACTIVE_FRAGMENT = "active-fragment";
    private static final int KFJC_PERM_READ_PHONE_STATE = 0;
    private static final int KFJC_PERM_WRITE_EXTERNAL = 1;

    private KfjcApplication application;

    private ServiceConnection streamServiceConnection;
    private ServiceConnection playlistServiceConnection;
    private StreamService streamService;
    private Intent streamServiceIntent;
    private PlaylistService playlistService;
    private Intent playlistServiceIntent;

    private NotificationUtil notificationUtil;
    private TelephonyManager telephonyManager;
    private PhoneStateListener phoneStateListener;

    private LiveStreamFragment liveStreamFragment;
    private PodcastFragment podcastFragment;
    private PodcastPlayerFragment podcastPlayerFragment;

    private NavigationView navigationView;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle drawerToggle;
    private View view;
    private Snackbar snackbar;

    private boolean askPermissionsAgain = true;
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
            podcastPlayerFragment = (PodcastPlayerFragment)
                    getFragmentManager().getFragment(savedInstanceState, "PodcastPlayerFragment");
        }

        HttpUtil.installCache(getApplicationContext());
        loadResources();

        setupPlaylistService();
        streamServiceIntent = new Intent(this, StreamService.class);
        startService(streamServiceIntent);

        this.liveStreamFragment = new LiveStreamFragment();
        this.podcastFragment = new PodcastFragment();
//        this.podcastPlayerFragment = new PodcastPlayerFragment();

        setupDrawer();
        setupStreamService();
        setupListenersAndManagers();

        LocalBroadcastManager.getInstance(this).registerReceiver(mediaStateReceiver,
                new IntentFilter(PlayerState.INTENT_PLAYER_STATE));
        mediaStateReceiver.onReceive(this, PlayerState.getLastPlayerState());
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
                }

                @Override
                public void onServiceDisconnected(ComponentName arg0) {
                }
            };
        }
    }

    private BroadcastReceiver mediaStateReceiver = new MediaStateReceiver() {
        @Override
        protected void onStateChange(PlayerState.State state, MediaSource source) {
            if (!PlayerState.State.STOP.equals(state)
                    && source != null
                    && source.type == MediaSource.Type.LIVESTREAM) {
                notificationUtil.updateNowPlayNotification(
                        playlistService.getPlaylist(), source);
            }
        }

        @Override
        protected void onError(PlayerState.State state, String message) {
            stopPlayer();
            String errorMessage = getString(R.string.error_generic);
            if (message != null && message.contains("HttpDataSourceException")) {
                errorMessage = getString(R.string.error_unable_to_connect);
            }
            snack(errorMessage, Snackbar.LENGTH_LONG);
        }
    };

    private void setupListenersAndManagers() {
        this.notificationUtil = new NotificationUtil(this);
        maybeAddPhoneStateListener();
    }

    private void maybeAddPhoneStateListener() {
        if (hasPhonePermission()) {
            this.telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            this.phoneStateListener = new KfjcPhoneStateListener(this);
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        } else {
            requestPhonePermission();
        }
    }

    private boolean hasPhonePermission() {
        return ContextCompat.checkSelfPermission(
                this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void startDownload(final ShowDetails show) {
        new AsyncTask<Void, Void, Void>() {
            protected Void doInBackground(Void... unusedParams) {
                try {
                    DownloadUtil.ensureDownloaded(HomeScreenDrawerActivity.this, show);
                } catch (IOException e) {}
                return null;
            }
        }.execute();
    }

    private void requestPhonePermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.READ_PHONE_STATE)) {
            AlertDialog alertDialog = new AlertDialog.Builder(this, R.style.AppTheme).create();
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

    public void requestAndroidWritePermissions() {
        ActivityCompat.requestPermissions(
                HomeScreenDrawerActivity.this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                KFJC_PERM_WRITE_EXTERNAL);
    }

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
            case KFJC_PERM_WRITE_EXTERNAL:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    onWritePermissionGranted(true);
                } else {
                    onWritePermissionGranted(false);
                }
        }
    }

    private void onWritePermissionGranted(boolean wasGranted) {
        if (podcastPlayerFragment != null) {
            podcastPlayerFragment.onWritePermissionResult(wasGranted);
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
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (drawerToggle.isDrawerIndicatorEnabled()) {
                    drawerLayout.openDrawer(Gravity.LEFT);
                } else {
                    if (activeFragmentId == R.id.nav_podcast_player) {
                        loadPodcastListFragment(true);
                    }
                }
            }
        });

        drawerLayout.addDrawerListener(drawerToggle);
    }

    @Override
    public void setActionBarBackArrow(boolean isBackArrow) {
        if (isBackArrow) {
            drawerToggle.setDrawerIndicatorEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        } else {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            drawerToggle.setDrawerIndicatorEnabled(true);
        }
    }

    public void setNavigationItemChecked(int navigationItemId) {
        navigationView.setCheckedItem(navigationItemId);
    }

    private void loadFragment(int fragmentId) {
        activeFragmentId = fragmentId;
        switch (fragmentId) {
            case R.id.nav_livestream:
                replaceFragment(liveStreamFragment);
                break;
            case R.id.nav_podcast:
                loadPodcastListFragment(false);
                break;
            case R.id.nav_podcast_player:
                if (streamService == null || streamService.getSource() == null) {
                    if (podcastPlayerFragment != null) {
                        replaceFragment(podcastPlayerFragment);
                    } else {
                        // Don't load empty podcast player
                        loadPodcastListFragment(false);
                    }
                } else {
                    loadPodcastPlayer(streamService.getSource().show, false);
                }
                break;
        }
    }

    @Override
    public void loadPodcastPlayer(ShowDetails show, boolean animate) {
        if (activeFragmentId == R.id.nav_podcast_player
                && podcastPlayerFragment != null
                && show.equals(podcastPlayerFragment.getShow())) {
            return;
        }
        activeFragmentId = R.id.nav_podcast_player;
        podcastPlayerFragment = PodcastPlayerFragment.newInstance(show);

        FragmentTransaction ft = getFragmentManager().beginTransaction();
        if (animate) {
            ft.setCustomAnimations(R.animator.fade_in_to_left, R.animator.fade_out_to_left);
        }
        ft.replace(R.id.home_screen_main_fragment, podcastPlayerFragment)
            .addToBackStack(null)
            .commit();
    }

    @Override
    public void loadPodcastListFragment(boolean animate) {
        activeFragmentId = R.id.nav_podcast;
        setActionBarBackArrow(false);
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        if (animate) {
            ft.setCustomAnimations(R.animator.fade_in_to_right, R.animator.fade_out_to_right);
        }
        ft.replace(R.id.home_screen_main_fragment, podcastFragment)
            .addToBackStack(null)
            .commit();
    }

    private void replaceFragment(KfjcFragment fragment) {
        setActionBarBackArrow(false);
        getFragmentManager().beginTransaction()
                .replace(R.id.home_screen_main_fragment, fragment, fragment.getFragmentTag())
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
        FragmentManager fm = getFragmentManager();
        if (fm.findFragmentByTag(podcastPlayerFragment.TAG) != null) {
            getFragmentManager().putFragment(outState, "PodcastPlayerFragment", podcastPlayerFragment);
        }
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
        if (activeFragmentId == R.id.nav_podcast_player) {
            loadPodcastListFragment(true);
            return;
        }
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
        Intent intent = getIntent();
        setIntent(null);
        if (intent != null) {
            MediaSource source = intent.getParcelableExtra(PlayerControl.INTENT_SOURCE);
            if (source != null) {
                switch (source.type) {
                    case LIVESTREAM:
                        loadFragment(R.id.nav_livestream);
                        return;
                    case ARCHIVE:
                        loadPodcastPlayer(source.show, false);
                        return;
                }
            }
            long[] downloadIds = intent.getLongArrayExtra(DownloadReceiver.INTENT_DOWNLOAD_IDS);
            if (downloadIds != null && downloadIds.length > 0) {
                for (long id : downloadIds) {
                    if (DownloadUtil.hasDownloadId(id)) {
                        loadPodcastPlayer(DownloadUtil.getDownload(id), false);
                        return;
                    }
                }
                loadFragment(R.id.nav_podcast);
                return;
            }

        }
        // Neither from notification nor download notification.
        loadFragment(activeFragmentId);
        return;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
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

    public void stopPlayer() {
        if (streamService != null) {
            PlayerControl.sendAction(this, PlayerControl.INTENT_STOP);
        }
        notificationUtil.cancelKfjcNotification();
        if (!isForegroundActivity) {
            playlistService.stop();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        notificationUtil.cancelKfjcNotification();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mediaStateReceiver);
        stopPlayer();
        stopService(streamServiceIntent);
        stopService(playlistServiceIntent);
        if (telephonyManager != null) {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
        }
    }

    @Override
    public boolean isStreamServicePlaying() {
        return streamService != null && streamService.isPlaying();
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

    @Override
    public long getPlayerPosition() {
        return streamService.getPlayerPosition();
    }

    @Override
    public void seekPlayer(long positionMillis) {
        streamService.seekOverEntireShow(positionMillis);
    }

}
