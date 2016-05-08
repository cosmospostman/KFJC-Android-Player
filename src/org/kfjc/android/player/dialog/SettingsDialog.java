package org.kfjc.android.player.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.TextView;

import org.kfjc.android.player.R;
import org.kfjc.android.player.activity.HomeScreenInterface;
import org.kfjc.android.player.control.PreferenceControl;
import org.kfjc.android.player.model.MediaSource;

import java.util.List;

public class SettingsDialog extends KfjcDialog {

    public interface StreamUrlPreferenceChangeHandler {
        void onStreamUrlPreferenceChange();
    }

    private SeekBar volumeSeekbar;
    private AudioManager audioManager;
    private Spinner spinner;
    private SwitchCompat backgroundSwitch;
    private MediaSource previousPreference;
    private StreamUrlPreferenceChangeHandler urlPreferenceChangeHandler;
    private ContextThemeWrapper themeWrapper;
    private HomeScreenInterface home;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        previousPreference = PreferenceControl.getStreamPreference();

        themeWrapper = new ContextThemeWrapper(getActivity(), R.style.KfjcDialog);
        View view = View.inflate(themeWrapper, R.layout.layout_settings, null);

        spinner = (Spinner) view.findViewById(R.id.streamPreferenceSpinner);
        spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                MediaSource mediaSource = (MediaSource) parent.getItemAtPosition(position);
                PreferenceControl.setStreamPreference(mediaSource);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        backgroundSwitch = (SwitchCompat) view.findViewById(R.id.backgroundSwitch);
        backgroundSwitch.setChecked(PreferenceControl.areBackgroundsEnabled());
        backgroundSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                PreferenceControl.setEnableBackgrounds(isChecked);
                home.updateBackground();
            }
        });

        initVolumeBar(view);
        initStreamOptions();

        AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity(), R.style.KfjcDialog);
        dialog.setView(view);
        dialog.setPositiveButton(getString(android.R.string.ok), new DialogInterface.OnClickListener() {
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

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (!(activity instanceof HomeScreenInterface)) {
            throw new IllegalStateException(
                    "Can only attach to " + HomeScreenInterface.class.getSimpleName());
        }
        this.home = (HomeScreenInterface) activity;
    }

    public void setUrlPreferenceChangeHandler(StreamUrlPreferenceChangeHandler handler) {
        this.urlPreferenceChangeHandler = handler;
    }

    private void initStreamOptions() {
        List<MediaSource> mediaSources = PreferenceControl.getMediaSources();
        StreamAdapter streamAdapter = new StreamAdapter(
                themeWrapper, android.R.layout.simple_spinner_item, mediaSources);
        spinner.setAdapter(streamAdapter);
        int selectedIndex = mediaSources.indexOf(PreferenceControl.getStreamPreference());
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
            @Override
            public void onStopTrackingTouch(SeekBar arg0) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar arg0) {
            }

            @Override
            public void onProgressChanged(SeekBar arg0, int progress, boolean arg2) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0);
            }
        });

        ContentObserver mSettingsContentObserver =
                new ContentObserver(new Handler()) {
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

    private class StreamAdapter extends ArrayAdapter<MediaSource> {
        private List<MediaSource> mediaSources;
        private Context context;
        public StreamAdapter(Context context, int resource, List<MediaSource> mediaSources) {
            super(context, resource, mediaSources);
            this.context = context;
            this.mediaSources = mediaSources;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = LayoutInflater.from(context).inflate(
                    android.R.layout.simple_spinner_item, parent, false);
            TextView streamName = (TextView) view.findViewById(android.R.id.text1);
            streamName.setText(mediaSources.get(position).name);
            return view;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            View view = LayoutInflater.from(context).inflate(
                    android.R.layout.simple_list_item_2, parent, false);
            TextView streamName = (TextView) view.findViewById(android.R.id.text1);
            TextView streamDesc = (TextView) view.findViewById(android.R.id.text2);
            streamName.setText(mediaSources.get(position).name);
            streamDesc.setText(mediaSources.get(position).description);
            streamDesc.setTextColor(ContextCompat.getColor(context, R.color.kfjc_secondary_text));
            return view;
        }
    }
}