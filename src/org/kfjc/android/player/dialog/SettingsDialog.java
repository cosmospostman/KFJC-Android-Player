package org.kfjc.android.player.dialog;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.kfjc.android.player.control.HomeScreenControl;
import org.kfjc.android.player.control.PreferenceControl;
import org.kfjc.android.player.R;

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
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class SettingsDialog extends DialogFragment {
	
	public interface StreamUrlPreferenceChangeHandler {
		void onStreamUrlPreferenceChange();
	}
	
	private SeekBar volumeSeekbar;
    private AudioManager audioManager; 
    private Context context;
    private List<String> streamNames;
    private RadioGroup radioGroup;
    private Map<String, Integer> streamNameToViewIdMap = new HashMap<String, Integer>();
    private String previousUrlPreference;
    private StreamUrlPreferenceChangeHandler urlPreferenceChangeHandler;
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
    	context = getActivity().getApplicationContext();
    	previousUrlPreference = PreferenceControl.getUrlPreference();
		
	    LayoutInflater inflater = getActivity().getLayoutInflater();
	    View view = inflater.inflate(R.layout.fragment_settings, new LinearLayout(context), false);
		radioGroup = (RadioGroup) view.findViewById(R.id.streamPreferenceRadioGroup);
		radioGroup.setOnCheckedChangeListener(checkChanged);
		
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
	    builder.setView(view);
		builder.setTitle(R.string.settings_dialog_title);
		builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
	       public void onClick(DialogInterface dialog, int id) {
	    	   boolean urlPreferenceChanged =
	    	       !previousUrlPreference.equals(PreferenceControl.getUrlPreference());
	    	   if (urlPreferenceChanged && urlPreferenceChangeHandler != null) {
	    		   urlPreferenceChangeHandler.onStreamUrlPreferenceChange();
	    	   }
	       }
		});

		initVolumeBar(view);
		initStreamOptions();
		return builder.create();
	}
	
	public void setUrlPreferenceChangeHandler(StreamUrlPreferenceChangeHandler handler) {
		this.urlPreferenceChangeHandler = handler;
	}
	
	OnCheckedChangeListener checkChanged = new OnCheckedChangeListener() {
		@Override
		public void onCheckedChanged(RadioGroup radioGroup, int checkedId) {
			RadioButton radioButton = (RadioButton) radioGroup.findViewById(radioGroup.getCheckedRadioButtonId());
			String val = radioButton.getText().toString();
			HomeScreenControl.preferenceControl.setStreamNamePreference(val);
		}
	};
	
	private void initStreamOptions() {
		radioGroup.removeAllViews();
		streamNames = HomeScreenControl.preferenceControl.getStreamNames();
		for (String stream : streamNames) {
			RadioButton button = new RadioButton(context);
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
