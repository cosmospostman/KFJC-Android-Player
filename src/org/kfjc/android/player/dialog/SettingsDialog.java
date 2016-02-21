package org.kfjc.android.player.dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.ContextThemeWrapper;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import org.kfjc.android.player.R;
import org.kfjc.android.player.activity.HomeScreenDrawerActivity;
import org.kfjc.android.player.control.PreferenceControl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SettingsDialog extends DialogFragment {
	
	public interface StreamUrlPreferenceChangeHandler {
		void onStreamUrlPreferenceChange();
	}
	
	private SeekBar volumeSeekbar;
    private AudioManager audioManager; 
    private List<String> streamNames;
    private RadioGroup radioGroup;
    private Map<String, Integer> streamNameToViewIdMap = new HashMap<String, Integer>();
    private String previousUrlPreference;
    private StreamUrlPreferenceChangeHandler urlPreferenceChangeHandler;
    private ContextThemeWrapper themeWrapper;

    @Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
        themeWrapper = new ContextThemeWrapper(getActivity(), R.style.KfjcDialog);
        previousUrlPreference = PreferenceControl.getUrlPreference();

		AlertDialog dialog = new AlertDialog.Builder(getActivity(), R.style.KfjcDialog).create();
        View view = View.inflate(themeWrapper, R.layout.fragment_settings, null);

        dialog.setView(view);
        dialog.setTitle(R.string.settings_dialog_title);
        dialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.ok),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        boolean urlPreferenceChanged =
                                !previousUrlPreference.equals(PreferenceControl.getUrlPreference());
                        if (urlPreferenceChanged && urlPreferenceChangeHandler != null) {
                            urlPreferenceChangeHandler.onStreamUrlPreferenceChange();
                        }
                    }
                });
        dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {}
                });
        radioGroup = (RadioGroup) view.findViewById(R.id.streamPreferenceRadioGroup);
        radioGroup.setOnCheckedChangeListener(checkChanged);
        initVolumeBar(view);
        initStreamOptions();
        return dialog;
	}
	
	public void setUrlPreferenceChangeHandler(StreamUrlPreferenceChangeHandler handler) {
		this.urlPreferenceChangeHandler = handler;
	}
	
	OnCheckedChangeListener checkChanged = new OnCheckedChangeListener() {
		@Override
		public void onCheckedChanged(RadioGroup radioGroup, int checkedId) {
			RadioButton radioButton = (RadioButton) radioGroup.findViewById(radioGroup.getCheckedRadioButtonId());
			String val = radioButton.getText().toString();
			HomeScreenDrawerActivity.preferenceControl.setStreamNamePreference(val);
		}
	};
	
	private void initStreamOptions() {
		radioGroup.removeAllViews();
		streamNames = HomeScreenDrawerActivity.preferenceControl.getStreamNames();
		for (String stream : streamNames) {
			RadioButton button = new RadioButton(themeWrapper);
		    button.setText(stream);
		    radioGroup.addView(button);
		    streamNameToViewIdMap.put(stream, button.getId());
		}
		
		Integer selectedId = streamNameToViewIdMap.get(PreferenceControl.getStreamNamePreference());
		if (selectedId != null) {
			radioGroup.check(selectedId);
		}
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
