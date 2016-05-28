package org.kfjc.android.player.activity;

import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

/**
 * Listen for telephone events. Stop playback if phone is in use; start it again when phone
 * is hung up.
 */
public class KfjcPhoneStateListener extends PhoneStateListener {

    private HomeScreenInterface homeScreen;
    private static boolean isStoppedDueToPhone = false;

    public KfjcPhoneStateListener(HomeScreenInterface homeScreen) {
        this.homeScreen = homeScreen;
    }

    @Override
    public void onCallStateChanged(int callState, String incomingNumber) {
        switch (callState) {
            case TelephonyManager.CALL_STATE_RINGING:
            case TelephonyManager.CALL_STATE_OFFHOOK:
                if (homeScreen.isStreamServicePlaying()) {
                    homeScreen.pausePlayer();
                    isStoppedDueToPhone = true;
                }
                break;
            case TelephonyManager.CALL_STATE_IDLE:
                if (isStoppedDueToPhone) {
                    homeScreen.unpausePlayer();
                    isStoppedDueToPhone = false;
                }
                break;
        }
    }
}
