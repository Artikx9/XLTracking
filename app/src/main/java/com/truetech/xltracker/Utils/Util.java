package com.truetech.xltracker.Utils;

import android.annotation.SuppressLint;
import android.app.*;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.telephony.*;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;
import com.truetech.xltracker.MainActivity;
import com.truetech.xltracker.R;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import java.io.*;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Set;

import static android.content.Context.LOCATION_SERVICE;
import static com.truetech.xltracker.Utils.Constant.*;

/**
 * Created by Ajwar on 09.06.2017.
 */
public class Util {

    private static double getAngleFromCoordinate(double lat1, double long1, double lat2, double long2) {
        double dLon = (long2 - long1);

        double y = Math.sin(dLon) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1)
                * Math.cos(lat2) * Math.cos(dLon);
        double brng = Math.atan2(y, x);
        brng = Math.toDegrees(brng);
        brng = (brng + 360) % 360;
        brng = 360 - brng; // count degrees counter-clockwise - remove to make clockwise

        return brng;
    }


    public static int getSignalStrength() {
        int strength = DEF_VALUE_NULL;
        TelephonyManager telephonyManager = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
        @SuppressLint("MissingPermission") List<CellInfo> cellInfos = telephonyManager.getAllCellInfo();
        //This will give info of all sims present inside your mobile
        if (cellInfos != null) {
            for (int i = 0; i < cellInfos.size(); i++) {
                if (cellInfos.get(i).isRegistered()) {
                    if (cellInfos.get(i) instanceof CellInfoGsm) {
                        CellInfoGsm cellInfogsm = (CellInfoGsm) cellInfos.get(i);
                        CellSignalStrengthGsm cellSignalStrengthGsm = cellInfogsm.getCellSignalStrength();
                        strength = cellSignalStrengthGsm.getAsuLevel();
                    } else if (cellInfos.get(i) instanceof CellInfoLte) {
                        CellInfoLte cellInfoLte = (CellInfoLte) cellInfos.get(i);
                        CellSignalStrengthLte cellSignalStrengthLte = cellInfoLte.getCellSignalStrength();
                        strength = parseLevel(cellSignalStrengthLte.getAsuLevel());
                    } else if (cellInfos.get(i) instanceof CellInfoCdma) {
                        CellInfoCdma cellInfoCdma = (CellInfoCdma) cellInfos.get(i);
                        CellSignalStrengthCdma cellSignalStrengthCdma = cellInfoCdma.getCellSignalStrength();
                        strength = parseLevel(cellSignalStrengthCdma.getAsuLevel());
                    } else if (cellInfos.get(i) instanceof CellInfoWcdma) {
                        CellInfoWcdma cellInfoWcdma = (CellInfoWcdma) cellInfos.get(i);
                        CellSignalStrengthWcdma cellSignalStrengthWcdma = cellInfoWcdma.getCellSignalStrength();
                        strength = cellSignalStrengthWcdma.getAsuLevel();
                    }
                }
            }
        }
        return strength;
    }

    public static int parseLevel(int level) {
        if (level >= 99) {
            return DEF_VALUE_NULL;
        } else if (level >= 93) {
            return 31;
        } else {
            return level / 3;
        }
    }

    public static int getOperatorCode() {
        TelephonyManager tm = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
        return Integer.parseInt(tm.getSimOperator());
    }

    public static int[] getLacAndCid() {
        int cid = DEF_VALUE_NULL;
        int lac = DEF_VALUE_NULL;
        TelephonyManager tm = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
        @SuppressLint("MissingPermission") CellLocation location = tm.getCellLocation();
        if (location != null) {
            if (location instanceof GsmCellLocation) {
                cid = ((GsmCellLocation) location).getCid();
                lac = ((GsmCellLocation) location).getLac();
            } else if (location instanceof CdmaCellLocation) {
                cid = ((CdmaCellLocation) location).getBaseStationId();
                lac = ((CdmaCellLocation) location).getSystemId();
            }
            if (lac == -1) lac = DEF_VALUE_NULL;
            if (cid == -1) cid = DEF_VALUE_NULL;
        }
        return new int[]{lac & 0xffff, cid & 0xffff};
    }

    public static short getAltitudeFromGisdata(Location loc) {
        double longitude = loc.getLongitude();
        double latitude = loc.getLatitude();
        //double result = Double.NaN;
        short result = Short.MIN_VALUE;
        HttpClient httpClient = new DefaultHttpClient();
        HttpContext localContext = new BasicHttpContext();
        String url = "http://gisdata.usgs.gov/"
                + "xmlwebservices2/elevation_service.asmx/"
                + "getElevation?X_Value=" + String.valueOf(longitude)
                + "&Y_Value=" + String.valueOf(latitude)
                + "&Elevation_Units=METERS&Source_Layer=-1&Elevation_Only=true";
        HttpGet httpGet = new HttpGet(url);
        try {
            HttpResponse response = httpClient.execute(httpGet, localContext);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                InputStream instream = entity.getContent();
                int r = -1;
                StringBuffer respStr = new StringBuffer();
                while ((r = instream.read()) != -1)
                    respStr.append((char) r);
                String tagOpen = "<double>";
                String tagClose = "</double>";
                result=parsePage(respStr,tagOpen,tagClose);
                instream.close();
            }
        } catch (ClientProtocolException e) {
        } catch (IOException e) {
        }
        return result;
    }

    public static short getAltitudeFromGoogleMaps(Location loc) {
        double longitude = loc.getLongitude();
        double latitude = loc.getLatitude();
        //double result = Double.NaN;
        short result = Short.MIN_VALUE;
        HttpClient httpClient = new DefaultHttpClient();
        HttpContext localContext = new BasicHttpContext();
        String url = "http://maps.googleapis.com/maps/api/elevation/"
                + "xml?locations=" + String.valueOf(latitude)
                + "," + String.valueOf(longitude)
                + "&sensor=true";
        HttpGet httpGet = new HttpGet(url);
        try {
            HttpResponse response = httpClient.execute(httpGet, localContext);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                InputStream instream = entity.getContent();
                int r = -1;
                StringBuffer respStr = new StringBuffer();
                while ((r = instream.read()) != -1)
                    respStr.append((char) r);
                String tagOpen = "<elevation>";
                String tagClose = "</elevation>";
                result=parsePage(respStr,tagOpen,tagClose);
/*                if (respStr.indexOf(tagOpen) != -1) {
                    int start = respStr.indexOf(tagOpen) + tagOpen.length();
                    int end = respStr.indexOf(tagClose);
                    String value = respStr.substring(start, end);
                    //result = (Double.parseDouble(value)*3.2808399); // convert from meters to feet
                    result = (short) Double.parseDouble(value); // convert from meters to feet
                }*/
                instream.close();
            }
        } catch (ClientProtocolException e) {
        } catch (IOException e) {
        }
        return result;
    }
    private static short parsePage(StringBuffer respStr,String tagOpen,String tagClose){
        if (respStr.indexOf(tagOpen) != -1) {
            int start = respStr.indexOf(tagOpen) + tagOpen.length();
            int end = respStr.indexOf(tagClose);
            String value = respStr.substring(start, end);
            //result = (Double.parseDouble(value)*3.2808399); // convert from meters to feet
            return (short) Double.parseDouble(value); // convert from meters to feet
        }
        return Short.MIN_VALUE;
    }
    public static int getIntFromPref(int key) {
        return Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(getContext()).getString(getContext().getString(key), null));
    }

    public static int getIntFromPref(String key) {
        return PreferenceManager.getDefaultSharedPreferences(getContext()).getInt(key, DEF_VALUE_NULL);
    }

    public static void setIntFromPref(int key, int value) {
        PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putInt(getContext().getString(key), value).apply();
    }

    public static void setIntFromPref(String key, int value) {
        PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putInt(key, value).apply();
    }

    public static void setLongFromPref(String key, long value) {
        PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putLong(key, value).apply();
    }

    public static void setLongFromPref(int key, long value) {
        PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putLong(getContext().getString(key), value).apply();
    }

    public static long getLongFromPref(int key) {
        return Long.parseLong(PreferenceManager.getDefaultSharedPreferences(getContext()).getString(getContext().getString(key), null));
    }

    public static long getLongFromPref(String key) {
        return PreferenceManager.getDefaultSharedPreferences(getContext()).getLong(key, DEF_VALUE_NULL);
    }

    public static boolean getBoolFromPref(int key) {
        return PreferenceManager.getDefaultSharedPreferences(getContext()).getBoolean(getContext().getString(key), DEF_VALUE_BOOL);
    }

    public static String getStringFromPref(String nameKey) {
        return PreferenceManager.getDefaultSharedPreferences(getContext()).getString(nameKey, DEF_VALUE_STRING);
    }
    public static String getStringFromPref(int key) {
        return PreferenceManager.getDefaultSharedPreferences(getContext()).getString(getContext().getString(key), null);
    }
    public static void setStringFromPref(String key, String value) {
        PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putString(key, value).apply();
    }

    // Check that Network Location Provider reports enabled
    static boolean isNetLocEnabled(Context context) {
        LocationManager lm = (LocationManager) context.getSystemService(LOCATION_SERVICE);
        return lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    // Check Wi-Fi is on
    static boolean confirmWiFiAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifiInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return wifiInfo.isAvailable();
    }

    // Check Airplane Mode - we want airplane mode off
    static boolean confirmAirplaneModeOff(Context context) {
        int airplaneSetting = Settings.System.getInt(context.getContentResolver(), Settings.System.AIRPLANE_MODE_ON, 0);
        return airplaneSetting == 0;
    }

    public static boolean isNetLocUsable(Context context) {
        return
                isNetLocEnabled(context) &&
                        confirmAirplaneModeOff(context) &&
                        confirmWiFiAvailable(context);
    }

    public static boolean isOnline(Context context) {

        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        //should check null because in airplane mode it will be null
        return (netInfo != null && netInfo.isConnected());
    }

    public static boolean isServiceRunningInForeground(Context context, Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                if (service.foreground) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isMyServiceRunning(Context context, Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            System.out.println(service.service.getClassName());
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isMyThreadRunning(String name) {
        Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
        for (Thread thread : threadSet) {
            if (thread.getName().contains(name)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isMainActivityRunning(Context context, String packageName) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> tasksInfo = activityManager.getRunningTasks(Integer.MAX_VALUE);
        for (int i = 0; i < tasksInfo.size(); i++) {
            System.out.println(tasksInfo.get(i).baseActivity.getPackageName());
            if (tasksInfo.get(i).baseActivity.getPackageName().toString().equals(packageName))
                return true;
        }
        return false;
    }

    public static byte[] intToByteArray(int value) {
        return new byte[]{
                (byte) (value >>> 24),
                (byte) (value >>> 16),
                (byte) (value >>> 8),
                (byte) value};
    }

    public static byte[] longToByteArray(long value) {
        return intToByteArray((int) value);
    }

    public static int setBitInByte(int data, int number, boolean value) {
        if (number>7) return 0;
        int maxLength=8;
        char[] array = Integer.toBinaryString(data).toCharArray();
        if (array.length!=maxLength){
            char[] arr=new char[maxLength-array.length];
            for (int i = 0; i <arr.length ; i++) {
                arr[i]=CHAR_FALSE;
            }
            array= concat(arr,array);
        }
        number=array.length-1-number;
        if (value) array[number] = CHAR_TRUE;
        else array[number] = CHAR_FALSE;
        String str = String.valueOf(array);
        return Integer.parseInt(str, 2);
    }

    public static char[] concat(char[] a, char[] b) {
        int aLen = a.length;
        int bLen = b.length;
        char[] c= new char[aLen+bLen];
        System.arraycopy(a, 0, c, 0, aLen);
        System.arraycopy(b, 0, c, aLen, bLen);
        return c;
    }
    public static int byteArrayToInt(byte[] bytes) {
        return bytes[0] << 24 | (bytes[1] & 0xFF) << 16 | (bytes[2] & 0xFF) << 8 | (bytes[3] & 0xFF);
    }

    public static int parseFloatToIntInIEEE754(double value) {
        return Float.floatToIntBits((float) value);
    }

    public static long getCountTable(SQLiteOpenHelper helpBd, String nameTable) {
        SQLiteDatabase db = helpBd.getReadableDatabase();
        return DatabaseUtils.queryNumEntries(db, nameTable);
    }

    public static int getCrc16(byte[] buffer) {
        return getCrc16(buffer, 0, buffer.length, 0xA001, 0);
    }

    public synchronized static int getCrc16(byte[] buffer, int offset, int bufLen, int polynom, int preset) {
        preset &= 0xFFFF;
        polynom &= 0xFFFF;
        int crc = preset;
        for (int i = 0; i < bufLen; i++) {
            int data = buffer[i + offset] & 0xFF;
            crc ^= data;
            for (int j = 0; j < 8; j++) {
                if ((crc & 0x0001) != 0) {
                    crc = (crc >> 1) ^ polynom;
                } else {
                    crc = crc >> 1;
                }
            }
        }
        return crc & 0xFFFF;
    }

    public static Application getContext() {
        Application app = null;
        try {
            app = getApplicationUsingReflectionFirst();
            if (app == null) app = getApplicationUsingReflectionTwo();
        } catch (Exception e) {
            Log.e(TAG, "Return global context", e);
        }
        return app;
    }

    private static Application getApplicationUsingReflectionFirst() throws Exception {
        return (Application) Class.forName("android.app.ActivityThread")
                .getMethod("currentApplication").invoke(null, (Object[]) null);
    }

    private static Application getApplicationUsingReflectionTwo() throws Exception {
        return (Application) Class.forName("android.app.AppGlobals")
                .getMethod("getInitialApplication").invoke(null, (Object[]) null);
    }

    public static void closeStream(Closeable... args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i] != null) try {
                args[i].close();
            } catch (IOException e) {
                Log.e(TAG, "Close OutputStream", e);
            }
        }
    }

    public static String getIp() {
        String[] urlGetIp = new String[]{
                "http://checkip.amazonaws.com/",
                "http://icanhazip.com/",
                "http://www.trackip.net/ip",
                "http://myexternalip.com/raw",
                "http://ipecho.net/plain"};
        String ip = "";
        URL url = null;
        for (int i = 0; i < urlGetIp.length; i++) {
            try {
                url = new URL(urlGetIp[i]);
            } catch (MalformedURLException e) {
                continue;
            }
            BufferedReader in = null;
            try {
                in = new BufferedReader(new InputStreamReader(url.openStream()));
                ip = in.readLine();
            } catch (IOException e) {
                continue;
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            if (!ip.isEmpty()) break;
        }
        return ip;
    }

    public static Notification getNotification(Context context, boolean notSwiped, boolean cancel, int smallIcon, int largeIcon, int idTicker, int idTitle, int idMessage) {
//        final Intent notificationIntent = new Intent(context, MainActivity.class);
//        notificationIntent.setAction(Intent.ACTION_MAIN);
//        notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);
//        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
//        //notification.setLatestEventInfo(this, getText(R.string.notification_title), getText(R.string.notification_message), pendingIntent);
//        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
//        builder.setContentIntent(pendingIntent)
//                .setOngoing(notSwiped)   //Can't be swiped out
//                .setAutoCancel(cancel)
//                .setSmallIcon(smallIcon)
//                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), largeIcon))
//                .setTicker(context.getText(idTicker))
//                .setContentTitle(context.getText(idTitle))
//                .setContentText(context.getText(idMessage))
//                .setWhen(System.currentTimeMillis());
//        Notification notification;
//        if (android.os.Build.VERSION.SDK_INT > 15) {
//            notification = builder.build();
//        } else {
//            notification = builder.getNotification();// API-15 and lower
//        }

        final int NOTIFY_ID = 0; // ID of notification
        String id = "1"; // default_channel_id
        String title = "channel"; // Default Channel
        Intent intent;
        PendingIntent pendingIntent;
        NotificationCompat.Builder builder;
        NotificationManager notifManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel mChannel = notifManager.getNotificationChannel(id);
            if (mChannel == null) {
                mChannel = new NotificationChannel(id, title, importance);
                mChannel.enableVibration(true);
                mChannel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
                notifManager.createNotificationChannel(mChannel);
            }
            builder = new NotificationCompat.Builder(context, id);
            intent = new Intent(context, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
            builder.setContentTitle(context.getText(idTitle))
                    .setOngoing(notSwiped)
                    .setSmallIcon(smallIcon)
                    .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), largeIcon))
                    .setContentText(context.getText(idMessage))
                    .setDefaults(Notification.DEFAULT_ALL)
                    .setAutoCancel(cancel)
                    .setContentIntent(pendingIntent)
                    .setTicker(context.getText(idTicker))
                    .setVibrate(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400})
                    .setWhen(System.currentTimeMillis());
        }
        else {
            builder = new NotificationCompat.Builder(context, id);
            intent = new Intent(context, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
            builder.setContentTitle(context.getText(idTitle))
                    .setOngoing(notSwiped)
                    .setSmallIcon(smallIcon)
                    .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), largeIcon))
                    .setContentText(context.getText(idMessage))
                    .setDefaults(Notification.DEFAULT_ALL)
                    .setAutoCancel(cancel)
                    .setContentIntent(pendingIntent)
                    .setTicker(context.getText(idTicker))
                    .setVibrate(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400})
                    .setWhen(System.currentTimeMillis());
        }
        Notification notification = builder.build();
        //notifManager.notify(NOTIFY_ID, notification);
        return notification;
    }

    public static long viewNotification(Context context, Notification notification, int id) {
        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(id, notification);
        return System.currentTimeMillis();
    }

    public static void cancelNotification(Context context, int id) {
        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(id);
    }

    public static int getBatteryPercentage(Context context) {

        IntentFilter iFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, iFilter);

        int level = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) : -1;
        int scale = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1) : -1;

        float batteryPct = level / (float) scale;

        return (int) (batteryPct * 100);
    }

    public static boolean isInternetAvailable() {
        try {
            InetAddress ipAddr = InetAddress.getByName("google.com");
            if (ipAddr.equals("")) {
                return false;
            } else {
                return true;
            }
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean compareDay(long after, long before) {
        return after - before >= DAY;
    }
    public static void showDialog(final Activity activity, String title, String message, String nameButtonOk, DialogInterface.OnClickListener onClickListener) {
        new android.support.v7.app.AlertDialog.Builder(activity)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(nameButtonOk, onClickListener)
                .create()
                .show();
    }
    public static boolean isGPSEnable(){
        return ((LocationManager)getContext().getSystemService(LOCATION_SERVICE)).isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    @SuppressLint("MissingPermission")
    public static Location getLastCopords() {
        LocationManager manager=((LocationManager)getContext().getSystemService(LOCATION_SERVICE));
        String[] providers = new String[]{ LocationManager.GPS_PROVIDER, LocationManager.PASSIVE_PROVIDER, LocationManager.NETWORK_PROVIDER };
        Location loc = null;
        for (String provider : providers) {
            loc = manager.getLastKnownLocation(provider);
            if (loc != null) {
                break;
            }
        }
        return loc;
    }

}




