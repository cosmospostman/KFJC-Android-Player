package org.kfjc.android.player.dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.ContextThemeWrapper;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;

import org.kfjc.android.player.R;
import org.kfjc.android.player.activity.HomeScreenDrawerActivity;
import org.kfjc.android.player.control.PreferenceControl;
import org.kfjc.android.player.model.Stream;

import java.util.ArrayList;
import java.util.List;

public class SettingsDialog extends KfjcDialog {
	
	public interface StreamUrlPreferenceChangeHandler {
		void onStreamUrlPreferenceChange();
	}
	
	private SeekBar volumeSeekbar;
    private AudioManager audioManager; 
    private Spinner spinner;
    private Stream previousPreference;
    private StreamUrlPreferenceChangeHandler urlPreferenceChangeHandler;
    private ContextThemeWrapper themeWrapper;

    @Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
        previousPreference = PreferenceControl.getStreamPreference();

        themeWrapper = new ContextThemeWrapper(getActivity(), R.style.KfjcDialog);
        View view = View.inflate(themeWrapper, R.layout.layout_settings, null);

        spinner = (Spinner) view.findViewById(R.id.streamPreferenceSpinner);
        spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String streamName = (String) parent.getItemAtPosition(position);
                for (Stream s : HomeScreenDrawerActivity.preferenceControl.getStreams()) {
                    if (s.name.equals(streamName)) {
                        HomeScreenDrawerActivity.preferenceControl.setStreamPreference(s);
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        initVolumeBar(view);
        initStreamOptions();

        AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity(), R.style.KfjcDialog);
        dialog.setView(view);
        dialog.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                boolean urlPreferenceChanged =
                        !previousPreference.equals(PreferenceControl.getStreamPreference());
                if (urlPreferenceChanged && urlPreferenceChangeHandler != null) {
                    urlPreferenceChangeHandler.onStreamUrlPreferenceChange();
                }
            }
        });
        return dialog.create();
	}
	
	public void setUrlPreferenceChangeHandler(StreamUrlPreferenceChangeHandler handler) {
		this.urlPreferenceChangeHandler = handler;
	}

	private void initStreamOptions() {
        List<String> streamUrls = new ArrayList<>();
        List<String> streamNames = new ArrayList<>();
        for (Stream s : HomeScreenDrawerActivity.preferenceControl.getStreams()) {
            streamUrls.add(s.url);
            streamNames.add(s.name);
        }
        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<>(
                themeWrapper, android.R.layout.simple_spinner_item, streamNames);
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(spinnerArrayAdapter);
        int selectedIndex = streamUrls.indexOf(PreferenceControl.getStreamPreference().url);
        spinner.setSelection(Math.max(0, selectedIndex));
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
