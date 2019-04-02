package com.truetech.xltracker.receiver;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import com.truetech.xltracker.R;
import com.truetech.xltracker.listener.EmptyLocationListener;
import com.truetech.xltracker.listener.ILocListener;
import com.truetech.xltracker.service.TrackerService;

import static android.content.Context.LOCATION_SERVICE;
import static com.truetech.xltracker.Utils.Constant.DEF_VALUE_NULL;
import static com.truetech.xltracker.Utils.Util.*;

/**
 * Created by Ajwar on 07.07.2017.
 */
public class GpsLocationReceiver extends BroadcastReceiver {
    private static LocationManager locManager = null;
    private static ILocListener locationListener = null;
    private TrackerService service;

    public GpsLocationReceiver(TrackerService service) {
        this.service = service;
        init(service);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        init(context);
        if (intent.getAction().equalsIgnoreCase("android.location.PROVIDERS_CHANGED") && getBoolFromPref(R.string.key_switch_service) && locManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            enableListenerForGPS();
        } else {
            disableLocListener();
        }
        if (intent.getAction().equalsIgnoreCase("android.net.conn.CONNECTIVITY_CHANGE") && getBoolFromPref(R.string.key_switch_service)
                && !locManager.isProviderEnabled(LocationManager.GPS_PROVIDER) && isOnline(context) && getBoolFromPref(R.string.key_use_gps_and_wifi)) {
            service.initProvider(false);
        }
    }

    public static void disableLocListener() {
        if (locationListener != null) locManager.removeUpdates(locationListener);
    }
    @SuppressLint("MissingPermission")
    public static void enableListenerForGPS() {
        if (locationListener != null && locManager!=null) locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, getIntFromPref(R.string.key_reg_data), DEF_VALUE_NULL, locationListener);
    }

    private void init(Context context) {
        if (locManager == null) locManager = (LocationManager) context.getSystemService(LOCATION_SERVICE);
        if (locationListener == null) locationListener = new EmptyLocationListener();
    }

}
