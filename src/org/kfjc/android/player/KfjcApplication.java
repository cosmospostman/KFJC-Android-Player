package org.kfjc.android.player;

import android.app.Application;
import android.os.AsyncTask;

import org.kfjc.android.player.control.PreferenceControl;
import org.kfjc.android.player.model.Resources;
import org.kfjc.android.player.model.ResourcesImpl;

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
