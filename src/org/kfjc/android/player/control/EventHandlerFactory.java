package org.kfjc.android.player.control;

import org.kfjc.android.player.dialog.SettingsDialog.StreamUrlPreferenceChangeHandler;
import org.kfjc.android.player.activity.HomeScreenActivity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.os.Handler;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

public class EventHandlerFactory {

	/**
	 * Audio Focus changes when another app requests access to the audio output stream. It could be
	 * total access (eg. another music player) or transient (eg. spoken directions from maps). In
	 * the latter case, we dip the volume for the other app and raise it again when the other app is
	 * done.
	 */
	static OnAudioFocusChangeListener onAudioFocusChange(
			final HomeScreenControl control, final AudioManager audioManager) {
		return new OnAudioFocusChangeListener() {
			
			private int volumeBeforeDuck;
            private static final String AUDIOFOCUS_KEY =
                    "org.kfjc.android.player.control_AUDIO_FOCUS_CHANGE_LISTENER";
			
			@Override public void onAudioFocusChange(int focusChange) {
				switch (focusChange) {
				case AudioManager.AUDIOFOCUS_LOSS:
					control.stopStream();
					audioManager.abandonAudioFocus(this);
					break;
				case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
					volumeBeforeDuck = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
					audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volumeBeforeDuck/2, 0);
					break;
				case AudioManager.AUDIOFOCUS_GAIN:
					audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volumeBeforeDuck, 0);
					break;
				}
			}

            // Objects are recreated after activity resume. Audio focus is requested and released
            // by reference to an OnAudioFocusChangeListener, and subsequently its toString()
            // method. By returning a constant string, we can consistently refer to the audio
            // focus we requested before the app was paused and resumed.
            @Override public String toString() {
                return AUDIOFOCUS_KEY;
            }
        };
		
	}

    /**
     * Listen for telephone events. Stop playback if phone is in use; start it again when phone
     * is hung up.
     */
    static PhoneStateListener onPhoneStateChange(final HomeScreenControl control) {
        return new PhoneStateListener() {
            private boolean isStoppedDueToPhone = false;
            @Override
            public void onCallStateChanged(int callState, String incomingNumber) {
                switch (callState) {
                    case TelephonyManager.CALL_STATE_RINGING:
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                        if (control.isStreamServicePlaying()) {
                            control.stopStream();
                            isStoppedDueToPhone = true;
                        }
                        break;
                    case TelephonyManager.CALL_STATE_IDLE:
                        if (isStoppedDueToPhone) {
                            control.playStream();
                            isStoppedDueToPhone = false;
                        }
                        break;
                }
            }
        };
    }
	
	/**
	 * When the user changes stream quality preference, we should restart the stream if it's
	 * currently playing.
	 */
	static StreamUrlPreferenceChangeHandler onUrlPreferenceChange(
			final HomeScreenControl control, final HomeScreenActivity activity) {
        final Handler handler = new Handler();
		return new StreamUrlPreferenceChangeHandler() {	
			@Override public void onStreamUrlPreferenceChange() {
				if (control.isStreamServicePlaying()) {
					control.stopStream();
                    control.playStream();
				}
			}
		};
	}
	
	/**
	 * The Becoming Noisy broadcast intent is sent when audio output hardware changes, perhaps
	 * from headphones to internal speaker. In such cases, we stop the stream to avoid
	 * embarrassment.
	 */
	static BroadcastReceiver onAudioBecomingNoisy(final HomeScreenControl control) {
		return new BroadcastReceiver() {
		    @Override
		    public void onReceive(Context context, Intent intent) {
		        if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
		            control.stopStream();
		        }
		    }
		};
	}
	
	static void unregisterReceiver(Activity activity, BroadcastReceiver receiver) {
		try {
			activity.unregisterReceiver(receiver);
		} catch (IllegalArgumentException e) {
			// receiver was already unregistered.
		}
	}

}
