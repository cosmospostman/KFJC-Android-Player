package org.kfjc.android.player;

import android.app.Application;
import android.os.AsyncTask;
import android.os.Build;

import com.google.android.gms.security.ProviderInstaller;

import org.kfjc.android.player.control.PreferenceControl;
import org.kfjc.android.player.model.Resources;
import org.kfjc.android.player.model.ResourcesImpl;
import org.kfjc.android.player.util.NotificationUtil;

import javax.net.ssl.SSLContext;

public class KfjcApplication extends Application {

    public interface ResourcesLoadedHandler {
        void onResourcesLoaded();
    }

    Resources resources;

    @Override
    public void onCreate() {
        super.onCreate();
        resources = new ResourcesImpl(this);
        PreferenceControl.init(KfjcApplication.this);

        // Need this for proper SSL support with Android < 4.4
        // https://stackoverflow.com/questions/29916962
        try {
            ProviderInstaller.installIfNeeded(getApplicationContext());
            SSLContext sslContext;
            sslContext = SSLContext.getInstance("TLSv1.2");
            sslContext.init(null, null, null);
            sslContext.createSSLEngine();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Resources getKfjcResources() {
        return resources;
    }

    public void loadResources(final ResourcesLoadedHandler handler) {
        new AsyncTask<Void, Void, Void>() {
            @Override protected Void doInBackground(Void... unsedParams) {
                resources.loadResources();
                return null;
            }
            @Override
            protected void onPostExecute(Void aVoid) {
                handler.onResourcesLoaded();
            }
        }.execute();
    }
}
