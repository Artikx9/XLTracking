package com.truetech.xltracker.tasks;


import android.app.Notification;
import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.util.Log;
import com.truetech.xltracker.R;
import com.truetech.xltracker.data.DBHelper;
import com.truetech.xltracker.listener.ILocListener;
import com.truetech.xltracker.service.TrackerService;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.TimerTask;

import static com.truetech.xltracker.Utils.Constant.*;
import static com.truetech.xltracker.Utils.Util.*;
import static com.truetech.xltracker.listener.AzimuthSensorListener.azimuth;
import static com.truetech.xltracker.listener.MyPhoneStateListener.strength;
import static com.truetech.xltracker.listener.SatellitesInfo.satellitesInFix;

/**
 * Created by Ajwar on 12.06.2017.
 */
public class TimerTaskBD extends TimerTask {
    private TrackerService service;
    private ILocListener locListener;
    private DBHelper dbHelper;
    private long timeCreateNotif = DEF_VALUE_NULL;
    private boolean existNotification = false;

    public TimerTaskBD(TrackerService service) {
        this.service = service;
        this.locListener = service.getLocationListener();
        this.dbHelper = DBHelper.getInstance();
    }

    @Override
    public void run() {
        long count = getCountTable(dbHelper, NAME_TABLE_LOC);
        if (count < LIMIT_ROWS_IN_BD) {
            createInsert();
            if (existNotification) {
                cancelNotification(service, R.string.key_notification_task_bd);
                existNotification = false;
            }
        } else if (!existNotification || compareDay(System.currentTimeMillis(), timeCreateNotif)) {
            Notification notification = getNotification(service, false, true, R.mipmap.min_icon_transparent, R.drawable.icon,
                    R.string.notification_ticker_task_bd, R.string.notification_title_task_bd, R.string.notification_message_task_bd);
            timeCreateNotif = viewNotification(service, notification, R.string.key_notification_task_bd);
            existNotification = true;
        }
    }

    private boolean[] readPref(Location loc) {
        boolean[] prefs = new boolean[10];
        prefs[9] = getBoolFromPref(R.string.key_send_without_gps);
        prefs[8] = getBoolFromPref(R.string.key_battery);

        prefs[4] = getBoolFromPref(R.string.key_satellites);
        prefs[5] = getBoolFromPref(R.string.key_loc_area_code);
        prefs[6] = getBoolFromPref(R.string.key_gsm_signal);
        prefs[7] = getBoolFromPref(R.string.key_operator_code);
        if (loc != null) {
            prefs[0] = getBoolFromPref(R.string.key_latitude);
            prefs[1] = getBoolFromPref(R.string.key_altitude);
            prefs[2] = getBoolFromPref(R.string.key_angle);
            prefs[3] = getBoolFromPref(R.string.key_speed);
        }

        return prefs;
    }

    private void createInsert() {
        Location loc = locListener.getCurrentLoc();
        boolean[] prefs = readPref(loc);
        if (loc == null && !prefs[9]) return;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream daos = new DataOutputStream(new BufferedOutputStream(baos));
        ContentValues cv = new ContentValues();
        SQLiteDatabase db;
        try {
            daos.write(getPriorityAndTimeStamp());
            if (prefs[8]) {//battery_energy
                daos.writeByte(GLOBAL_MASK);
            } else {
                daos.writeByte(GLOBAL_MASK_WITHOUT_IO);
            }
            int mask = 255;//in binary string ==1111 111
            mask = setBitInByte(mask, 7, prefs[7]);
            mask = setBitInByte(mask, 6, prefs[6]);
            mask = setBitInByte(mask, 5, prefs[5]);
            mask = setBitInByte(mask, 4, prefs[4]);
            if (loc == null) loc = getLastCopords();
            if (loc != null) {
                mask = setBitInByte(mask, 3, prefs[3]);
                mask = setBitInByte(mask, 2, prefs[2]);
                mask = setBitInByte(mask, 1, prefs[1]);
                mask = setBitInByte(mask, 0, prefs[0]);
                daos.writeByte(mask);
                if (prefs[0]) {     //latitude
                    daos.writeInt(parseFloatToIntInIEEE754(loc.getLatitude()));
                    daos.writeInt(parseFloatToIntInIEEE754(loc.getLongitude()));
                }
                if (prefs[1]) {
                    short altitude = getAltitude(loc);//altitude
                    daos.writeShort(altitude);
                }
                if (prefs[2]) {//angle
                    float angle = getAngle(loc);
                    daos.writeByte((int) (angle * 256 / 360));
                }
                if (prefs[3]) {//speed
                    daos.writeByte((int) loc.getSpeed());
                }
            } else {
                mask = setBitInByte(mask, 3, false);//speed
                mask = setBitInByte(mask, 2, false);//angle
                mask = setBitInByte(mask, 1, false);//altitude
                mask = setBitInByte(mask, 0, false);//latitude,longitude
                daos.writeByte(mask);
            }

            if (prefs[4]) {//satellites
                daos.writeByte(satellitesInFix);
            }
            if (prefs[5]) {//loc_area_code
                int[] array = getLacAndCid();             //cell id
                daos.writeShort(array[0]);//write local area code
                daos.writeShort(array[1]);//write cell ID
            }
            if (prefs[6]) {//gsm_signal
                daos.writeByte(strength);

            }
            if (prefs[7]) {//operator_code
                daos.writeInt(getOperatorCode());
            }
            if (prefs[8]) {//battery_energy
                //Write IOElement
                //Write Battery level in percentage
                daos.writeByte(HEX_ONE);//write Quantity
                daos.writeByte(HEX_ONE);//write ID Battery level
                daos.writeByte(getBatteryPercentage(service));//write battery level percentage
            }
            daos.flush();
            byte[] bytes = baos.toByteArray();
            cv.put(COL_DATA, bytes);
            cv.put(COL_DATE_INSERT, System.currentTimeMillis() / 1000);
            db = dbHelper.getWritableDatabase();
            db.insert(NAME_TABLE_LOC, null, cv);
        } catch (Exception e) {
            Log.e(TAG, "Write byteArrayOutputStream in TimerTaskBD", e);
        } finally {
            closeStream(daos, baos);
        }
    }

    private byte[] getPriorityAndTimeStamp() {
        long time = System.currentTimeMillis() / 1000 - DIFF_UNIX_TIME;
        byte[] bytes = longToByteArray(time);
        byte temp = 64;//01 00 00 00 in a decimal number system(Priority)
        bytes[0] += temp;
        return bytes;
    }

    private LocationProvider getProvider(String name) {
        LocationManager manager = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
        return manager.getProvider(name);
    }

    private short getAltitude(Location loc) {
        short altitude;//altitude
        if (getProvider(loc.getProvider()).supportsAltitude()) {
            altitude = (short) loc.getAltitude();
        } else {
            altitude = getAltitudeFromGoogleMaps(loc);
            if (altitude == Short.MIN_VALUE) altitude = getAltitudeFromGisdata(loc);
        }
        return altitude;
    }

    private float getAngle(Location loc) {
        float angle;//angle
        if (loc.getProvider().equals(LocationManager.GPS_PROVIDER)) {
            angle = loc.getBearing();
        } else {
            angle = azimuth;
        }
        return angle;
    }

}


