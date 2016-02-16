package org.kfjc.android.player.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TableRow;
import android.widget.TextView;

import org.kfjc.android.player.R;
import org.kfjc.android.player.model.Playlist;

public class TrackDetailsDialog extends DialogFragment {

    public static final String KEY_ALBUM = "album";
    public static final String KEY_ARTIST = "artist";
    public static final String KEY_LABEL = "label";
    public static final String KEY_TIME = "time";
    public static final String KEY_TRACK = "track";

    Context context;

    public static TrackDetailsDialog newInstance(Playlist.PlaylistEntry entry) {
        Bundle args = new Bundle();
        args.putString(KEY_ALBUM, entry.getAlbum());
        args.putString(KEY_ARTIST, entry.getArtist());
        args.putString(KEY_LABEL, entry.getLabel());
        args.putString(KEY_TIME, entry.getTime());
        args.putString(KEY_TRACK, entry.getTrack());
        
        TrackDetailsDialog fragment = new TrackDetailsDialog();
        fragment.setArguments(args);
        return fragment;
    }

    String artistString = "";
    String trackString = "";
    String albumString = "";
    String labelString = "";

    @Override
    public Dialog onCreateDialog(Bundle bundle) {
        context = getActivity().getApplicationContext();
        if (bundle == null) {
            bundle = getArguments();
        }

        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.layout_trackdetails, new LinearLayout(context), false);

        if (bundle != null) {
            artistString = bundle.getString(KEY_ARTIST);
            trackString = bundle.getString(KEY_TRACK);
            albumString = bundle.getString(KEY_ALBUM);
            labelString = bundle.getString(KEY_LABEL);

            setValueAndShow(view, R.id.trackdetails_artist_row, R.id.trackdetails_artist_value, artistString);
            setValueAndShow(view, R.id.trackdetails_track_row, R.id.trackdetails_track_value, trackString);
            setValueAndShow(view, R.id.trackdetails_album_row, R.id.trackdetails_album_value, albumString);
            setValueAndShow(view, R.id.trackdetails_label_row, R.id.trackdetails_label_value, labelString);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(view)
                .setNegativeButton("Send to", searchClicked)
                .setPositiveButton("Search", searchClicked);
        return builder.create();
    }

    private void setValueAndShow(View view, int tableRowId, int tableValueId, String value) {
        if (TextUtils.isEmpty(value)) {
            return;
        }
        TableRow rowView = (TableRow) view.findViewById(tableRowId);
        TextView valueView = (TextView) view.findViewById(tableValueId);
        rowView.setVisibility(View.VISIBLE);
        valueView.setText(value);
    }

    DialogInterface.OnClickListener searchClicked = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case DialogInterface.BUTTON_NEGATIVE:
                    sendTo();
                    break;
                case DialogInterface.BUTTON_POSITIVE:
                    search();
                    break;
                default:
                    break;
            }
        }
    };

    private void search() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setAction(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH);
        intent.putExtra(SearchManager.QUERY,
                String.format("%s %s %s", artistString, trackString, albumString));
        this.startActivity(intent);
    }

    private void sendTo() {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT,
                String.format("Heard it on KFJC: %s - %s (%s)", artistString, trackString, albumString));
        sendIntent.setType("text/plain");
        startActivity(sendIntent);
    }
}
