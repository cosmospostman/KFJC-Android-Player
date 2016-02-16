package org.kfjc.android.player.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.os.Bundle;
import android.text.Html;
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

    @Override
    public Dialog onCreateDialog(Bundle bundle) {
        context = getActivity().getApplicationContext();
        if (bundle == null) {
            bundle = getArguments();
        }

        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.layout_trackdetails, new LinearLayout(context), false);

        if (bundle != null) {
            setValueAndShow(view, R.id.trackdetails_artist_row, R.id.trackdetails_artist_value,
                    bundle.getString(KEY_ARTIST));
            setValueAndShow(view, R.id.trackdetails_track_row, R.id.trackdetails_track_value,
                    bundle.getString(KEY_TRACK));
            setValueAndShow(view, R.id.trackdetails_album_row, R.id.trackdetails_album_value,
                    bundle.getString(KEY_ALBUM));
            setValueAndShow(view, R.id.trackdetails_label_row, R.id.trackdetails_label_value,
                    bundle.getString(KEY_LABEL));
        }


        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(view);
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
}
