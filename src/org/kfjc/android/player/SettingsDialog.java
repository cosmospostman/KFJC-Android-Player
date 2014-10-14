package org.kfjc.android.player;

import org.kfjc.droid.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class SettingsDialog extends DialogFragment {
	
	private SeekBar volumeSeekbar;
    private AudioManager audioManager; 
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
	    LayoutInflater inflater = getActivity().getLayoutInflater();
	    View view = inflater.inflate(R.layout.settings_fragment, null);
		
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
	    builder.setView(view);
		builder.setTitle(R.string.settings_title);
		builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
	       public void onClick(DialogInterface dialog, int id) {}
		});

		initVolumeBar(view);
		return builder.create();
	}
	
	private void initVolumeBar(View view) {
		volumeSeekbar = (SeekBar) view.findViewById(R.id.volumeSeekBar);
        audioManager = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);
        volumeSeekbar.setMax(audioManager
                .getStreamMaxVolume(AudioManager.STREAM_MUSIC));
        volumeSeekbar.setProgress(audioManager
                .getStreamVolume(AudioManager.STREAM_MUSIC));
        volumeSeekbar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override public void onStopTrackingTouch(SeekBar arg0) {}
            @Override public void onStartTrackingTouch(SeekBar arg0) {}
            @Override public void onProgressChanged(
            		SeekBar arg0, int progress, boolean arg2) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0);
            }
        });
        
        ContentObserver mSettingsContentObserver =
    		new ContentObserver(new Handler()){
        		@Override
        		public void onChange(boolean selfChange) {
        			int volumeLevel = audioManager
        	                .getStreamVolume(AudioManager.STREAM_MUSIC);
        			volumeSeekbar.setProgress(volumeLevel);
        		}
        	};
        getActivity().getContentResolver().registerContentObserver(
        		android.provider.Settings.System.CONTENT_URI, true, mSettingsContentObserver);
	}

}
