package org.kfjc.android.player.activity;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ProgressBar;

import org.kfjc.android.player.R;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class LavaLampActivity extends AppCompatActivity {

    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private MediaPlayer mediaPlayer;
    private ProgressBar progressBar;
    private View loadingView;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.fadein, R.anim.fadeout);
        setContentView(R.layout.activity_lavaplayer);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        surfaceView = (SurfaceView) findViewById(R.id.lavaSurfaceView);
        loadingView = findViewById(R.id.lavaLoadingView);
        progressBar = (ProgressBar) findViewById(R.id.lavaLoadingProgress);

        surfaceHolder = surfaceView.getHolder();
        mediaPlayer = new MediaPlayer();

        final DownloadTask downloadTask = new DownloadTask(LavaLampActivity.this);
        downloadTask.execute();

        surfaceView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        mediaPlayer.stop();
        mediaPlayer.release();
    }

    private void playLava(final File lavaFile) {
        loadingView.setVisibility(View.GONE);
        surfaceView.setVisibility(View.VISIBLE);
        surfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                Context context = getApplicationContext();

                try {
                    mediaPlayer.setDataSource(lavaFile.getCanonicalPath());
                    mediaPlayer.setDisplay(holder);
                    mediaPlayer.setLooping(true);
                    mediaPlayer.prepare();
                    mediaPlayer.start();
                    setVideoSize();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
            }
        });
    }

    private void setVideoSize() {

        // // Get the dimensions of the video
        int videoWidth = mediaPlayer.getVideoWidth();
        int videoHeight = mediaPlayer.getVideoHeight();
        float videoProportion = (float) videoWidth / (float) videoHeight;

        // Get the width of the screen
        int screenWidth = getWindowManager().getDefaultDisplay().getWidth();
        int screenHeight = getWindowManager().getDefaultDisplay().getHeight();
        float screenProportion = (float) screenWidth / (float) screenHeight;

        // Get the SurfaceView layout parameters
        android.view.ViewGroup.LayoutParams lp = surfaceView.getLayoutParams();
        if (videoProportion > screenProportion) {
            lp.width = screenWidth;
            lp.height = (int) ((float) screenWidth / videoProportion);
        } else {
            lp.width = (int) (videoProportion * (float) screenHeight);
            lp.height = screenHeight;
        }
        // Commit the layout parameters
        surfaceView.setLayoutParams(lp);
    }

    private class DownloadTask extends AsyncTask<String, Integer, File> {

        private Context context;
        private PowerManager.WakeLock wakeLock;

        public DownloadTask(Context context) {
            this.context = context;
        }

        @Override
        protected File doInBackground(String... unused) {
            try {
                File cacheDir = getApplicationContext().getCacheDir();
                File tempFile = cacheDir.createTempFile("lava", ".mp4.tmp");
                final File lavaFile = new File(cacheDir, "lava.mp4");
                if (lavaFile.exists()) {
                    return lavaFile;
                }
                lavaFile.createNewFile();

                URL url = new URL("http://www.kfjc.org/api/drawable/lavalamp.mp4");
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                FileOutputStream fileOutput = new FileOutputStream(tempFile);
                int responseCode = urlConnection.getResponseCode();
                int fileLength = urlConnection.getContentLength();
                if (responseCode != 200) {
                    throw new IOException("got response code " + responseCode);
                }
                try {
                    BufferedInputStream bis = new BufferedInputStream(urlConnection.getInputStream());
                    byte[] buffer = new byte[1024];
                    int bufferLength = 0;
                    int totalLoaded = 0;
                    while ((bufferLength = bis.read(buffer)) > 0) {
                        fileOutput.write(buffer, 0, bufferLength);
                        totalLoaded += bufferLength;
                        int progress = (int) (totalLoaded * 100 / fileLength);
                        publishProgress(progress);
                    }
                } finally {
                    fileOutput.close();
                    urlConnection.disconnect();
                }
                tempFile.renameTo(lavaFile);
                return lavaFile;
            } catch (IOException e) {
                return null;
            }
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            progressBar.setIndeterminate(false);
            progressBar.setMax(100);
            progressBar.setProgress(progress[0]);
        }

        @Override
        protected void onPreExecute() {
            // take CPU lock to prevent CPU from going off if the user
            // presses the power button during download
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    getClass().getName());
            wakeLock.acquire();
        }

        @Override
        protected void onPostExecute(File result) {
            wakeLock.release();
            playLava(result);
        }
    }
	
}
