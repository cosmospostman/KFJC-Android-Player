package org.kfjc.android.player.cast;

import com.google.android.gms.cast.framework.CastOptions;
import com.google.android.gms.cast.framework.OptionsProvider;
import com.google.android.gms.cast.framework.SessionProvider;

import android.content.Context;

import org.kfjc.android.player.R;

import java.util.List;


public class CastOptionsProvider implements OptionsProvider {

    @Override
    public CastOptions getCastOptions(Context context) {
        return new CastOptions.Builder()
                .setReceiverApplicationId(context.getString(R.string.application_cast_id))
                .build();
    }

    @Override
    public List<SessionProvider> getAdditionalSessionProviders(Context context) {
        return null;
    }
}