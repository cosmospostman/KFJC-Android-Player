package org.kfjc.android.player.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.ContextThemeWrapper;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.kfjc.android.player.R;
import org.kfjc.android.player.activity.HomeScreenInterface;
import org.kfjc.android.player.model.ShowDetails;
import org.kfjc.android.player.util.ExternalStorageUtil;
import org.kfjc.android.player.util.Intents;

public class OfflineDialog extends KfjcDialog {

    public interface OnDismissListener {
        void onDismiss();
    }

    public static final String KEY_SIZE = "filesize";
    public static final String KEY_SHOW_DETAILS = "showDetails";
    public static final String KEY_IS_DOWNLOADED = "isDownloaded";

    private HomeScreenInterface homeScreen;
    private ShowDetails showDetails;
    private boolean isDownloaded;

    private OnDismissListener onDismissListener;

    public static OfflineDialog newInstance(ShowDetails show, long fileSize, boolean isDownloaded) {
        Bundle args = new Bundle();
        args.putParcelable(KEY_SHOW_DETAILS, show);
        args.putLong(KEY_SIZE, fileSize);
        args.putBoolean(KEY_IS_DOWNLOADED, isDownloaded);
        OfflineDialog fragment = new OfflineDialog();
        fragment.setArguments(args);
        return fragment;
    }

    public void setOnDismissListener(OnDismissListener listener) {
        this.onDismissListener = listener;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        onDismissListener.onDismiss();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (!(activity instanceof HomeScreenInterface)) {
            throw new IllegalStateException(
                    "Can only attach to " + HomeScreenInterface.class.getSimpleName());
        }
        this.homeScreen = (HomeScreenInterface) activity;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
    }

    @Override
    public Dialog onCreateDialog(Bundle bundle) {
        ContextThemeWrapper themeWrapper = new ContextThemeWrapper(getActivity(), R.style.KfjcDialog);
        View view = View.inflate(themeWrapper, R.layout.layout_offline, null);
        AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity(), R.style.KfjcDialog);
        dialog.setView(view);

        if (bundle == null) {
            bundle = getArguments();
        }

        if (bundle != null) {
            showDetails = bundle.getParcelable(KEY_SHOW_DETAILS);
            isDownloaded = bundle.getBoolean(KEY_IS_DOWNLOADED, false);
            long filesizeBytes;
            filesizeBytes = isDownloaded
                    ? ExternalStorageUtil.folderSize(showDetails.getPlaylistId())
                    : bundle.getLong(KEY_SIZE);
            int filesizeMb = (int) filesizeBytes / 1024 / 1024;
            boolean canDownload = ExternalStorageUtil.bytesAvailable(filesizeBytes);

            ImageView icon = (ImageView) view.findViewById(R.id.offlineActionIcon);
            TextView text = (TextView) view.findViewById(R.id.dialogText);
            TextView fileSizeView = (TextView) view.findViewById(R.id.fileSize);

            icon.setImageResource(isDownloaded
                    ? R.drawable.ic_offline_pin_white_48dp
                    : R.drawable.ic_file_download_white_48dp);
            text.setText(isDownloaded
                    ? R.string.dialog_podcast_is_downloaded
                    : R.string.dialog_podcast_can_download);
            if (canDownload) {
                fileSizeView.setText(isDownloaded
                        ? getString(R.string.format_file_size_saved, filesizeMb)
                        : getString(R.string.format_file_size_download, filesizeMb));
                dialog.setPositiveButton(isDownloaded ? R.string.button_delete : R.string.button_download, dialogExit);
            } else {
                fileSizeView.setText(getString(R.string.format_file_size_download_no_space, filesizeMb));
            }

        }
        dialog.setNegativeButton(R.string.button_close, dialogExit);

        return dialog.create();
    }

    DialogInterface.OnClickListener dialogExit = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case DialogInterface.BUTTON_NEGATIVE:
                    dismiss();
                    break;
                case DialogInterface.BUTTON_POSITIVE:
                    if (isDownloaded) {
                        Intents.sendAction(getActivity(), Intents.INTENT_STOP);
                        ExternalStorageUtil.deletePodcastDir(showDetails.getPlaylistId());
                    } else {
                        homeScreen.startDownload(showDetails);
                    }
                    break;
                default:
                    break;
            }
        }
    };
}