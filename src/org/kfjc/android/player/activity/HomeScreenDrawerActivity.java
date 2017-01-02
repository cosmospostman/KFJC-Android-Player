package org.kfjc.android.player.activity;

import android.Manifest;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
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
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import com.google.android.gms.cast.framework.CastButtonFactory;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.SessionManagerListener;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

import org.kfjc.android.player.KfjcApplication;
import org.kfjc.android.player.R;
import org.kfjc.android.player.control.PreferenceControl;
import org.kfjc.android.player.fragment.KfjcFragment;
import org.kfjc.android.player.fragment.LiveStreamFragment;
import org.kfjc.android.player.fragment.PodcastFragment;
import org.kfjc.android.player.fragment.PodcastPlayerFragment;
import org.kfjc.android.player.intent.PlayerControl;
import org.kfjc.android.player.intent.PlayerState;
import org.kfjc.android.player.intent.PlaylistUpdate;
import org.kfjc.android.player.model.KfjcMediaSource;
import org.kfjc.android.player.model.ShowDetails;
import org.kfjc.android.player.receiver.DownloadReceiver;
import org.kfjc.android.player.receiver.MediaStateReceiver;
import org.kfjc.android.player.service.ChromecastPlayback;
import org.kfjc.android.player.service.ChromecastPlayback.CastConnectionChangedCallback;
import org.kfjc.android.player.service.PlaylistService;
import org.kfjc.android.player.service.StreamService;
import org.kfjc.android.player.util.DownloadUtil;
import org.kfjc.android.player.util.HttpUtil;
import org.kfjc.android.player.util.NotificationUtil;

import java.io.IOException;
import java.util.Calendar;

public class HomeScreenDrawerActivity extends AppCompatActivity implements HomeScreenInterface {

    private static final String KEY_ACTIVE_FRAGMENT = "active-fragment";
    private static final int KFJC_PERM_WRITE_EXTERNAL = 0;

    private KfjcApplication application;

    private ServiceConnection streamServiceConnection;
    private ServiceConnection playlistServiceConnection;
    private StreamService streamService;
    private Intent streamServiceIntent;
    private PlaylistService playlistService;
    private Intent playlistServiceIntent;

    private NotificationUtil notificationUtil;

    private PodcastPlayerFragment podcastPlayerFragment;

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
            podcastPlayerFragment = (PodcastPlayerFragment)
                    getFragmentManager().getFragment(savedInstanceState, "PodcastPlayerFragment");
        }

        HttpUtil.installCache(getApplicationContext());
        loadResources();

        setupPlaylistService();
        streamServiceIntent = new Intent(this, StreamService.class);
        startService(streamServiceIntent);

        setupDrawer();
        setupStreamService();
        setupListenersAndManagers();

        CastContext mCastContext = CastContext.getSharedInstance(this);
        CastSession mCastSession = mCastContext.getSessionManager().getCurrentCastSession();
        ChromecastPlayback
                .initialize(this, mCastSession)
                .setupCastListener(castConnectionChangedCallback);


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
                    if (ChromecastPlayback.getInstance().isConnected()) {
                        streamService.setCastPlayback();
                    } else {
                        streamService.setLocalPlayback();
                    }
                }

                @Override
                public void onServiceDisconnected(ComponentName arg0) {}
            };
        }
    }

    private BroadcastReceiver mediaStateReceiver = new MediaStateReceiver() {
        @Override
        protected void onStateChange(PlayerState.State state, KfjcMediaSource source) {
            if (!PlayerState.State.STOP.equals(state)
                    && source != null
                    && source.type == KfjcMediaSource.Type.LIVESTREAM) {
                notificationUtil.updateNowPlayNotification(
                        PlaylistUpdate.getLastPlaylist(), source);
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

    public void requestAndroidWritePermissions() {
        ActivityCompat.requestPermissions(
                HomeScreenDrawerActivity.this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                KFJC_PERM_WRITE_EXTERNAL);
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == KFJC_PERM_WRITE_EXTERNAL) {
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i("MENU", "onCreate");
        getMenuInflater().inflate(R.menu.toolbar, menu);
        CastButtonFactory.setUpMediaRouteButton(
                getApplicationContext(), menu, R.id.media_route_menu_item);
        return true;
    }

    public void setNavigationItemChecked(int navigationItemId) {
        navigationView.setCheckedItem(navigationItemId);
    }

    private void loadFragment(int fragmentId) {
        activeFragmentId = fragmentId;
        switch (fragmentId) {
            case R.id.nav_livestream:
                replaceFragment(new LiveStreamFragment());
                break;
            case R.id.nav_podcast:
                loadPodcastListFragment(false);
                break;
            case R.id.nav_podcast_player:
                if (streamService == null || streamService.getSource() == null) {
                    if (podcastPlayerFragment != null) {
                        replaceFragment(podcastPlayerFragment);
                    } else {
                        Intent i = PlayerState.getLastPlayerState();
                        KfjcMediaSource source = i.getParcelableExtra(PlayerState.INTENT_KEY_PLAYER_SOURCE);
                        if (source.show != null) {
                            loadPodcastPlayer(source.show, false);
                        } else {
                            // Don't load empty podcast player
                            loadPodcastListFragment(false);
                        }
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
        setActionBarBackArrow(true);
    }

    @Override
    public void loadPodcastListFragment(boolean animate) {
        activeFragmentId = R.id.nav_podcast;
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        if (animate) {
            ft.setCustomAnimations(R.animator.fade_in_to_right, R.animator.fade_out_to_right);
        }
        ft.replace(R.id.home_screen_main_fragment, new PodcastFragment())
            .addToBackStack(null)
            .commit();
        setActionBarBackArrow(false);
    }

    private void replaceFragment(KfjcFragment fragment) {
        getFragmentManager().beginTransaction()
                .replace(R.id.home_screen_main_fragment, fragment, fragment.getFragmentTag())
                .addToBackStack(null)
                .commit();
        setActionBarBackArrow(fragment.setActionBarBackArrow());
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
        if (fm.findFragmentByTag(PodcastPlayerFragment.TAG) != null) {
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
            KfjcMediaSource source = intent.getParcelableExtra(PlayerControl.INTENT_SOURCE);
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
        NotificationUtil.cancelKfjcNotification();
        if (!isForegroundActivity) {
            playlistService.stop();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        NotificationUtil.cancelKfjcNotification();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mediaStateReceiver);
        if (isFinishing()) {
            stopPlayer();
            stopService(streamServiceIntent);
            stopService(playlistServiceIntent);
        }
        recycleBackground();
    }

    /**
     * Need to call this from onDestroy to free up background image bitmap
     */
    private void recycleBackground() {
        ImageView backgroundImageView = (ImageView) findViewById(R.id.backgroundImageView);
        Drawable drawable = backgroundImageView.getDrawable();
        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            Bitmap bitmap = bitmapDrawable.getBitmap();
            bitmap.recycle();
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
    public long getPlayerPosition() {
        if (streamService == null) {
            return 0;
        }
        return streamService.getPlayerPosition();
    }

    @Override
    public void seekPlayer(long positionMillis) {
        streamService.seekOverEntireShow(positionMillis);
    }

    CastConnectionChangedCallback castConnectionChangedCallback = new CastConnectionChangedCallback() {
        @Override
        public void onApplicationConnected() {
            invalidateOptionsMenu();
            streamService.setCastPlayback();
        }

        @Override
        public void onApplicationDisconnected() {
            invalidateOptionsMenu();
            streamService.setLocalPlayback();
        }
    };

}
