package com.truetech.xltracker.listener;

import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;

/**
 * Created by Ajwar on 05.07.2017.
 */
public class MyPhoneStateListener extends PhoneStateListener {
    public static volatile int strength=0;
    @Override
    public void onSignalStrengthsChanged(SignalStrength signalStrength) {
        super.onSignalStrengthsChanged(signalStrength);
        int signal=signalStrength.getGsmSignalStrength();
        if (signal==99) strength=0;
        else strength=signal;
    }
}
