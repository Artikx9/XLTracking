package com.truetech.xltracker.service;

import android.annotation.SuppressLint;
import android.app.*;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.location.Criteria;
import android.location.GpsStatus;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.truetech.xltracker.MainActivity;
import com.truetech.xltracker.R;
import com.truetech.xltracker.data.DBHelper;
import com.truetech.xltracker.listener.*;
import com.truetech.xltracker.receiver.GpsLocationReceiver;
import com.truetech.xltracker.tasks.TimerTaskBD;
import com.truetech.xltracker.tasks.TimerTaskEnableGps;
import com.truetech.xltracker.tasks.TimerTaskToServer;

import java.util.Timer;
import java.util.TimerTask;

import static com.truetech.xltracker.MainActivity.refreshAdapter;
import static com.truetech.xltracker.Utils.Constant.*;
import static com.truetech.xltracker.Utils.Util.*;
import static com.truetech.xltracker.listener.SatellitesInfo.satellitesInFix;

/**
 * Created by Ajwar on 09.06.2017.
 */
public class TrackerService extends Service {
    private LocationManager locManager = null;
    private ILocListener locationListener = null;
    private GpsStatus.Listener satListener = null;
    private IAzimuthSensorListener azimuthListener = null;
    private MyPhoneStateListener mPhoneStateListener=null;
    private TelephonyManager mTelephonyManager;
    private String currentProvider = null;
    private DBHelper dbHelper = null;
    private NotificationManager notificationManager = null;
    private Timer timerBd = null;
    private Timer timerGPS = null;
    private TimerTask taskBd = null;
    private boolean restartFlag=false;
    private GpsLocationReceiver gpsLocationReceiver=null;
    public static Timer timerServer=null;


    @SuppressLint("MissingPermission")
    @Override
    public void onCreate() {
        try {
            super.onCreate();
            notificationManager = (NotificationManager) this.getSystemService(this.NOTIFICATION_SERVICE);
            locManager = (LocationManager) getApplicationContext().getSystemService(LOCATION_SERVICE);
            mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            if (locationListener == null) locationListener = new LocListener();
            if (mPhoneStateListener==null) mPhoneStateListener=new MyPhoneStateListener();
            if (azimuthListener == null) azimuthListener = new AzimuthSensorListener();
            if (satListener == null) satListener = new SatellitesInfo(this);
            dbHelper = DBHelper.getInstance();
            mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
            if (getBoolFromPref(R.string.key_switch_service)) {//on or off switch run service
                locManager.addGpsStatusListener(satListener);
                initProvider(true);
            }
            refreshAdapter(0,getString(R.string.listView_data)+getString(R.string.on));
        } catch (Throwable e) {
            Log.e(TAG,"onCreate service",e);
        }
    }

    @Override
    public void onDestroy() {
        locManager.removeUpdates(locationListener);
        locManager.removeGpsStatusListener(satListener);
        azimuthListener.disable(getApplicationContext());
        taskBd.cancel();
        timerBd.cancel();
        timerServer.cancel();
        timerGPS.cancel();
        mTelephonyManager.listen(mPhoneStateListener,PhoneStateListener.LISTEN_NONE);
        notificationManager.cancelAll();
        GpsLocationReceiver.disableLocListener();
        getApplicationContext().unregisterReceiver(gpsLocationReceiver);
        if (dbHelper!=null) {
            dbHelper.close();
        }
        stopSelf();
        satellitesInFix=0;
        refreshAdapter(0, getString(R.string.listView_data) + getString(R.string.off));
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            regLocBroadCast();
            Notification notification=getNotification(this,true,false,R.mipmap.min_icon_transparent,R.drawable.icon,
                    R.string.ticker_text,R.string.notification_title,R.string.notification_message);
            startForeground(R.string.key_tracking_service, notification);
            timerBd = new Timer();
            taskBd = new TimerTaskBD(this);
            timerBd.schedule(taskBd, DEF_VALUE_NULL, getIntFromPref(R.string.key_reg_data) * THOUSAND);
            timerServer=new Timer();
            TimerTask taskServer=new TimerTaskToServer(this);
            if (TimerTaskToServer.existNotification) {
                cancelNotification(this,R.string.key_notification_task_server);
            }
            TimerTaskToServer.existNotification=false;
            setIntFromPref(LIMIT_TRY_CONNECT, DEF_VALUE_NULL);
            int period=getIntFromPref(R.string.key_send_data);
            if (period<PERIOD_RESTART_TASK) period=PERIOD_RESTART_TASK;
            timerServer.schedule(taskServer, DEF_VALUE_NULL,period*THOUSAND);
            timerGPS = new Timer();
            timerGPS.schedule(new TimerTaskEnableGps(this),HOUR,HOUR);
        } catch (Throwable e) {
            Log.e(TAG,"onStartCommand service",e);
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @SuppressLint("MissingPermission")
    public boolean initProvider(boolean useGps) {
        boolean enable = true;
        int time = getIntFromPref(R.string.key_reg_data) * THOUSAND;
        currentProvider = null;
        if (useGps) {
            Criteria criteria = new Criteria();
            criteria.setAccuracy(Criteria.ACCURACY_FINE);
            currentProvider = locManager.getBestProvider(criteria, true);
        } else if (isOnline(getApplicationContext()) && getBoolFromPref(R.string.key_use_gps_and_wifi)) {
            currentProvider = LocationManager.NETWORK_PROVIDER;
        } else if (locManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER)) {
            currentProvider = LocationManager.PASSIVE_PROVIDER;
        }
        if (currentProvider != null) {
            locManager.requestLocationUpdates(currentProvider, time, getIntFromPref(R.string.key_min_distance_provider), locationListener);
        } else enable = false;
        return enable;
    }

    private void regLocBroadCast(){
        gpsLocationReceiver=new GpsLocationReceiver(this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        intentFilter.addAction(LocationManager.PROVIDERS_CHANGED_ACTION);
        getApplicationContext().registerReceiver(gpsLocationReceiver,intentFilter);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public String getCurrentProvider() {
        return currentProvider;
    }

    public void setCurrentProvider(String currentProvider) {
        this.currentProvider = currentProvider;
    }

    public ILocListener getLocationListener() {
        return locationListener;
    }

    public void setLocationListener(ILocListener locationListener) {
        this.locationListener = locationListener;
    }

    public LocationManager getLocManager() {
        return locManager;
    }

    public void setLocManager(LocationManager locManager) {
        this.locManager = locManager;
    }

    public IAzimuthSensorListener getAzimuthListener() {
        return azimuthListener;
    }

    public void setAzimuthListener(IAzimuthSensorListener azimuthListener) {
        this.azimuthListener = azimuthListener;
    }

    public DBHelper getDbHelper() {
        return dbHelper;
    }

    public void setDbHelper(DBHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    public Timer getTimerBd() {
        return timerBd;
    }

    public void setTimerBd(Timer timerBd) {
        this.timerBd = timerBd;
    }

    public TimerTask getTaskBd() {
        return taskBd;
    }

    public void setTaskBd(TimerTask taskBd) {
        this.taskBd = taskBd;
    }

    public boolean isRestartFlag() {
        return restartFlag;
    }

    public void setRestartFlag(boolean restartFlag) {
        this.restartFlag = restartFlag;
    }


}
