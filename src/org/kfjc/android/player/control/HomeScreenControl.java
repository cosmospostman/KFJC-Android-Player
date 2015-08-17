package org.kfjc.android.player.control;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.os.AsyncTask;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import org.kfjc.android.player.activity.HomeScreenActivity;
import org.kfjc.android.player.activity.HomeScreenActivity.PlayerState;
import org.kfjc.android.player.activity.HomeScreenActivity.StatusState;
import org.kfjc.android.player.dialog.SettingsDialog;
import org.kfjc.android.player.dialog.SettingsDialog.StreamUrlPreferenceChangeHandler;
import org.kfjc.android.player.model.TrackInfo;
import org.kfjc.android.player.service.PlaylistService;
import org.kfjc.android.player.service.StreamService;
import org.kfjc.android.player.service.StreamService.LiveStreamBinder;
import org.kfjc.android.player.service.StreamService.MediaListener;
import org.kfjc.android.player.util.NotificationUtil;

public class HomeScreenControl {
	
	public static PreferenceControl preferenceControl;
		
	private Intent streamServiceIntent;
    private Intent playlistServiceIntent;
	private StreamService streamService;
    private PlaylistService playlistService;
	private ServiceConnection streamServiceConnection;
    private ServiceConnection playlistServiceConnection;
	private final HomeScreenActivity activity;
	private NotificationUtil notificationUtil;
	private AudioManager audioManager;
    private TelephonyManager telephonyManager;
    private OnAudioFocusChangeListener audioFocusListener;
	private StreamUrlPreferenceChangeHandler streamUrlPreferenceChangeListener;
    private PhoneStateListener phoneStateListener;
	
	public HomeScreenControl(final HomeScreenActivity activity) {
        loadAvailableStreams(activity);

        this.activity = activity;
        this.notificationUtil = new NotificationUtil(activity);
		this.audioManager = (AudioManager)
                activity.getSystemService(Context.AUDIO_SERVICE);
        this.telephonyManager = (TelephonyManager)
                activity.getSystemService(Context.TELEPHONY_SERVICE);
        this.audioFocusListener = EventHandlerFactory.onAudioFocusChange(this, audioManager);
        this.phoneStateListener = EventHandlerFactory.onPhoneStateChange(this);
		this.streamUrlPreferenceChangeListener =
				EventHandlerFactory.onUrlPreferenceChange(this, activity);

        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);

		if (streamServiceConnection == null) {
            streamServiceConnection = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    LiveStreamBinder binder = (LiveStreamBinder) service;
                    streamService = binder.getService();
                    streamService.setMediaEventListener(mediaEventHandler);
                    activity.setState(streamService.getPlayerState());
                }

                @Override
                public void onServiceDisconnected(ComponentName arg0) {
                }
            };
		}
        if (playlistServiceConnection == null) {
            playlistServiceConnection = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    PlaylistService.PlaylistBinder binder = (PlaylistService.PlaylistBinder) service;
                    playlistService = binder.getService();
                    playlistService.start();
                    playlistService.registerPlaylistCallback(new PlaylistService.PlaylistCallback() {
                        @Override
                        public void onTrackInfoFetched(TrackInfo trackInfo) {
                            activity.updateTrackInfo(trackInfo);
                            if (streamService != null && streamService.isPlaying()) {
                                notificationUtil.updateNowPlayNotification(trackInfo);
                            }
                        }
                    });
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {}
            };
        }
	}

    public void onCreate() {
        streamServiceIntent = new Intent(activity, StreamService.class);
        playlistServiceIntent = new Intent(activity, PlaylistService.class);
        activity.startService(playlistServiceIntent);
        activity.startService(streamServiceIntent);
    }

    public void onStart() {
        activity.bindService(streamServiceIntent, streamServiceConnection, Context.BIND_AUTO_CREATE);
        activity.bindService(playlistServiceIntent, playlistServiceConnection, Context.BIND_AUTO_CREATE);
    }

    public void onResume() {
        if (playlistService != null) {
            playlistService.start();
        }
    }

    public void onStop() {
        if (!streamService.isPlaying()) {
            playlistService.stop();
        }
        activity.unbindService(streamServiceConnection);
        activity.unbindService(playlistServiceConnection);
    }

    public void destroy() {
        notificationUtil.cancelNowPlayNotification();
        stopStream();
        activity.stopService(streamServiceIntent);
        activity.stopService(playlistServiceIntent);
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
    }

    private void loadAvailableStreams(final HomeScreenActivity activity) {
        new AsyncTask<Void, Void, Void>() {
            @Override protected Void doInBackground(Void... unsedParams) {
                HomeScreenControl.preferenceControl =
                        new PreferenceControl(activity.getApplicationContext());
                return null;
            }

            @Override protected void onPostExecute(Void aVoid) {
                onStreamUrlsLoaded();
            }
        }.execute();
    }

    public void onStreamUrlsLoaded() {
        activity.enableSettingsButton();
        activity.enablePlayStopButton();
    }
	
	private MediaListener mediaEventHandler = new MediaListener() {
        @Override
        public void onBuffer() {
            activity.setState(PlayerState.BUFFER);
        }

        @Override
		public void onPlay() {
			activity.setState(PlayerState.PLAY);
		}
		
		@Override
		public void onError(String message) {
            activity.showDebugAlert(message);
			stopStream();
            activity.setStatusState(HomeScreenActivity.StatusState.CONNECTION_ERROR);
        }

        @Override
        public void onEnd() {
            activity.setState(PlayerState.STOP);
        }
	};

	public void playStream() {
		audioManager.requestAudioFocus(
                audioFocusListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN);
		streamService.play(activity.getApplicationContext(), PreferenceControl.getUrlPreference());
        notificationUtil.updateNowPlayNotification(playlistService.getLastFetchedTrackInfo());
        activity.setStatusState(StatusState.CONNECTING);
	}
	
	public void stopStream() {
        activity.setState(PlayerState.STOP);
		streamService.stop();
		audioManager.abandonAudioFocus(audioFocusListener);
        notificationUtil.cancelNowPlayNotification();
        if (!activity.isForegroundActivity()) {
            playlistService.stop();
        }
	}
	
	public void showSettings() {
		SettingsDialog settingsFragment = new SettingsDialog();
		settingsFragment.setUrlPreferenceChangeHandler(streamUrlPreferenceChangeListener);
		settingsFragment.show(activity.getFragmentManager(), "settings");
	}
	
	public boolean isStreamServicePlaying() {
		return streamService != null && streamService.isPlaying();
	}

}
