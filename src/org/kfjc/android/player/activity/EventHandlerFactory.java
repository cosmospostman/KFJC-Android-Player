package org.kfjc.android.player.activity;

import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

public class EventHandlerFactory {

    /**
     * Listen for telephone events. Stop playback if phone is in use; start it again when phone
     * is hung up.
     */
    static PhoneStateListener onPhoneStateChange(final HomeScreenInterface control) {
        return new PhoneStateListener() {
            private boolean isStoppedDueToPhone = false;
            @Override
            public void onCallStateChanged(int callState, String incomingNumber) {
                switch (callState) {
                    case TelephonyManager.CALL_STATE_RINGING:
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                        if (control.isStreamServicePlaying()) {
                            control.stopPlayer();
                            isStoppedDueToPhone = true;
                        }
                        break;
                    case TelephonyManager.CALL_STATE_IDLE:
                        if (isStoppedDueToPhone) {
                            control.playStream();
                            isStoppedDueToPhone = false;
                        }
                        break;
                }
            }
        };
    }
}
