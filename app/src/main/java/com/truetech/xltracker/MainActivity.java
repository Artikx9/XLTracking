package com.truetech.xltracker;

import android.content.*;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import com.truetech.xltracker.Utils.Util;
import com.truetech.xltracker.activity.SettingsActivity;
import com.truetech.xltracker.service.TrackerService;

import java.util.ArrayList;
import java.util.List;

import static com.truetech.xltracker.Utils.CheckPermissions.*;
import static com.truetech.xltracker.Utils.Constant.*;
import static com.truetech.xltracker.Utils.Util.*;
import static com.truetech.xltracker.listener.SatellitesInfo.*;


public class MainActivity extends AppCompatActivity {

    private BroadcastReceiver broadcastReceiver;
    private ListView lvMain;
    private String imei;
    private static ArrayAdapter<String> adapter = null;
    private static List<String> dataListView = new ArrayList<>(3);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BOOT_COMPLETED);
        intentFilter.addAction(Intent.ACTION_REBOOT);
        intentFilter.addCategory(Intent.CATEGORY_DEFAULT);
        registerReceiver(broadcastReceiver, intentFilter);
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (hasPermissions(context,ARRAY_PERMISSIONS) && getBoolFromPref(R.string.key_switch_service)) {
                    context.startService(new Intent(context, TrackerService.class));
                }
            }
        };
        registerReceiver(broadcastReceiver, intentFilter);
        String title = getString(R.string.title_location_permission);
        String message = getString(R.string.text_location_permission);
        String butOk = getString(R.string.ok);
        if (checkAllPermissions(this, PERMISSION_ALL, title, message, butOk, ARRAY_PERMISSIONS)) {
            beginApp();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (!getBoolFromPref(R.string.key_switch_service) && isServiceRunningInForeground(this, TrackerService.class)) {
            stopService(new Intent(this, TrackerService.class));
        }
    }

    //click button "Options"
    public void viewSettings(View view) {
        if(getStringFromPref(R.string.key_block_password_setting) == null || getStringFromPref(R.string.key_block_password_setting).equals("")){
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
            return;
        }
        LayoutInflater li = LayoutInflater.from(this);
        View promptsView = li.inflate(R.layout.password_dialog, null);
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setView(promptsView);
        final EditText userInput = promptsView
                .findViewById(R.id.editTextDialogPassword);
        alertDialogBuilder
                .setCancelable(false)
                .setNegativeButton(this.getString(R.string.ok),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                if(getStringFromPref(R.string.key_block_password_setting).equals(userInput.getText().toString())) {
                                    Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                                    startActivity(intent);
                                } else Toast.makeText(MainActivity.this,getText(R.string.wrong_data),Toast.LENGTH_SHORT).show();
                            }
                        })
                .setPositiveButton(this.getString(R.string.cancel),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                            }

                        }

                );
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    public void exit(View view) {
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        //super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_ALL: {
                if (grantResults.length > 0) {
                    // permission was granted, yay! Do the
                    // location-related task you need to do.
                    if (hasPermissions(this, ARRAY_PERMISSIONS)) {
                        beginApp();
                    } else {
                        exit(null);
                    }
                }
                return;
            }
        }
    }

    private void saveImeiInPref() {
        TelephonyManager mngr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        imei = "999999999999999";//mngr.getDeviceId();
        PreferenceManager.getDefaultSharedPreferences(this).edit().putString(IMEI, imei).apply();

    }
    private void beginApp(){
        setContentView(R.layout.activity_main);
        saveImeiInPref();
        setIntFromPref(LIMIT_TRY_CONNECT, DEF_VALUE_NULL);
        initListView();
        if (!isGPSEnable()){
            String title = getString(R.string.title_enable_gps);
            String message = getString(R.string.text_enable_gps);
            String butOk = getString(R.string.ok);
            Util.showDialog(this,title,message,butOk,null);
        }
    }
    public static void refreshAdapter(int num,String str){
        if (dataListView.size()>0) {
            dataListView.set(num,str);
            adapter.notifyDataSetChanged();
        }
    }

    private void initListView(){
        dataListView.clear();
        dataListView.add(getString(R.string.listView_data)+(isServiceRunningInForeground(this,TrackerService.class) ? getString(R.string.on) : getString(R.string.off)));
        dataListView.add(getString(R.string.listView_imei)+imei);
        dataListView.add(getString(R.string.listView_sat)+satellitesInFix);
        dataListView.add(getString(R.string.listView_speed) +speed);
        adapter=new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, dataListView);
        lvMain= findViewById(R.id.key_listView_main);
        if(adapter != null)
        lvMain.setAdapter(adapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        initListView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (broadcastReceiver != null)
            unregisterReceiver(broadcastReceiver);
    }
}
