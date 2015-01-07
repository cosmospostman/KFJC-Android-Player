package org.kfjc.android.player.control;

import org.kfjc.android.player.SettingsDialog.StreamUrlPreferenceChangeHandler;
import org.kfjc.android.player.activity.HomeScreenActivity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;

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
			
			public void onAudioFocusChange(int focusChange) {
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
		};
		
	}
	
	/**
	 * When the user changes stream quality preference, we should restart the stream if it's
	 * currently playing.
	 */
	static StreamUrlPreferenceChangeHandler onUrlPreferenceChange(
			final HomeScreenControl control, final HomeScreenActivity activity) {
		return new StreamUrlPreferenceChangeHandler() {	
			@Override
			public void onStreamUrlPreferenceChange() {
				activity.updateStreamNickname(PreferenceControl.getStreamNamePreference());
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