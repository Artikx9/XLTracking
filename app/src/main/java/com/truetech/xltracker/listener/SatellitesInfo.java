package com.truetech.xltracker.listener;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;
import com.truetech.xltracker.R;
import com.truetech.xltracker.service.TrackerService;

import static com.truetech.xltracker.MainActivity.refreshAdapter;
import static com.truetech.xltracker.Utils.Constant.DEF_VALUE_NULL;
import static com.truetech.xltracker.Utils.Util.*;
import static com.truetech.xltracker.receiver.GpsLocationReceiver.disableLocListener;
import static com.truetech.xltracker.receiver.GpsLocationReceiver.enableListenerForGPS;

/**
 * Created by Ajwar on 12.06.2017.
 */
public class SatellitesInfo implements GpsStatus.Listener {
    private LocationListener locListener = null;
    private LocationManager locManager = null;
    private TrackerService service = null;
    public static volatile int satellitesInFix = 0;

    public SatellitesInfo(TrackerService service) {
        this.service = service;
        this.locManager = service.getLocManager();
        this.locListener = service.getLocationListener();
    }


    @SuppressLint("MissingPermission")
    @Override
    public void onGpsStatusChanged(int event) {
        satellitesInFix=0;
        if (event == GpsStatus.GPS_EVENT_SATELLITE_STATUS){
            for (GpsSatellite sat : locManager.getGpsStatus(null).getSatellites()) {
                if (sat.usedInFix()) {
                    satellitesInFix++;
                }
            }
        }
        //dataListView.set(2,getContext().getString(R.string.listView_sat)+satellitesInFix);
        refreshAdapter(2,getContext().getString(R.string.listView_sat)+satellitesInFix);
        if (satellitesInFix>=4 && !service.getCurrentProvider().equals(LocationManager.GPS_PROVIDER)){
            locManager.removeUpdates(locListener);
            locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,getIntFromPref(R.string.key_reg_data), DEF_VALUE_NULL,locListener);
            service.getAzimuthListener().disable(getContext());
            service.setCurrentProvider(LocationManager.GPS_PROVIDER);
            disableLocListener();
        }else if (service.getCurrentProvider().equals(LocationManager.GPS_PROVIDER) && satellitesInFix<4 && getBoolFromPref(R.string.key_use_gps_and_wifi)){
            locManager.removeUpdates(locListener);
            //GpsLocationReceiver.disableLocListener();
            enableListenerForGPS();
            service.initProvider(false);
            service.getAzimuthListener().enable(getContext());
        }
    }

}