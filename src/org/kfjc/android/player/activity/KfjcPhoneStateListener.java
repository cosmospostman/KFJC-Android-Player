package org.kfjc.android.player.activity;

import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import org.kfjc.android.player.intent.PlayerControlIntent;

/**
 * Listen for telephone events. Stop playback if phone is in use; start it again when phone
 * is hung up.
 */
public class KfjcPhoneStateListener extends PhoneStateListener {

    private HomeScreenDrawerActivity homeScreen;
    private static boolean isStoppedDueToPhone = false;

    public KfjcPhoneStateListener(HomeScreenDrawerActivity homeScreen) {
        this.homeScreen = homeScreen;
    }

    @Override
    public void onCallStateChanged(int callState, String incomingNumber) {
        switch (callState) {
            case TelephonyManager.CALL_STATE_RINGING:
            case TelephonyManager.CALL_STATE_OFFHOOK:
                if (homeScreen.isStreamServicePlaying()) {
                    PlayerControlIntent.sendAction(homeScreen, PlayerControlIntent.INTENT_PAUSE);
                    isStoppedDueToPhone = true;
                }
                break;
            case TelephonyManager.CALL_STATE_IDLE:
                if (isStoppedDueToPhone) {
                    PlayerControlIntent.sendAction(homeScreen, PlayerControlIntent.INTENT_UNPAUSE);
                    isStoppedDueToPhone = false;
                }
                break;
        }
    }
}
