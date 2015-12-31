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
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.ImageView;

import org.kfjc.android.player.R;
import org.kfjc.android.player.control.PreferenceControl;
import org.kfjc.android.player.dialog.SettingsDialog;
import org.kfjc.android.player.fragment.LiveStreamFragment;
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
    private SettingsDialog.StreamUrlPreferenceChangeHandler streamUrlPreferenceChangeListener;
    private PhoneStateListener phoneStateListener;


    private LiveStreamFragment liveStreamFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        setContentView(R.layout.activity_home_screen_drawer);

        setupDrawer();
        setupStreamService();
        setupListenersAndManagers();

        this.liveStreamFragment = new LiveStreamFragment();
        getSupportFragmentManager().beginTransaction()
                .add(R.id.home_screen_main_fragment, liveStreamFragment)
                .commit();

        streamServiceIntent = new Intent(this, StreamService.class);
        startService(streamServiceIntent);
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
//            liveStreamFragment.setState(LiveStreamFragment.PlayerState.BUFFER);
        }

        @Override
        public void onPlay() {
            liveStreamFragment.setState(LiveStreamFragment.PlayerState.PLAY);
        }

        @Override
        public void onError(String message) {
            stopStream();
            liveStreamFragment.setState(LiveStreamFragment.State.ERROR);
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
                preferenceControl = new PreferenceControl(getApplicationContext());
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

        Toolbar toolbar = (Toolbar) findViewById(R.id.home_screen_toolbar);
        setSupportActionBar(toolbar);
        drawerToggle = new ActionBarDrawerToggle(
                this,  drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close
        );

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        drawerToggle.setDrawerIndicatorEnabled(true);
        drawerLayout.setDrawerListener(drawerToggle);
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

    private int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }
}
