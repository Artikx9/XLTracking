package com.truetech.xltracker.activity;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import com.truetech.xltracker.R;
import com.truetech.xltracker.fragments.SettingsFragment;
import com.truetech.xltracker.service.TrackerService;

import static com.truetech.xltracker.Utils.CheckPermissions.*;
import static com.truetech.xltracker.Utils.Util.getBoolFromPref;
import static com.truetech.xltracker.Utils.Util.isServiceRunningInForeground;



public class SettingsActivity extends AppCompatActivity {
    private SettingsFragment fragment;
    private Preference switchOnOff;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        fragment = new SettingsFragment();
        getFragmentManager().beginTransaction().replace(R.id.activity_settings, fragment).commit();
    }

/*    @Override
    public void onBackPressed() {
        restartService();
        super.onBackPressed();
    }*/

    @Override
    protected void onPause() {
        restartService();
        /*if (fragment.isFlagRestartService() || !isServiceRunningInForeground(getApplicationContext(), TrackerService.class)){
            restartService();
            fragment.setFlagRestartService(false);
        }*/
/*        if (getBoolFromPref(R.id.key_switch_service)){

        }*/
        super.onPause();
    }

    private void restartService(){
        String title = getString(R.string.title_location_permission);
        String message = getString(R.string.text_location_permission);
        String butOk = getString(R.string.ok);
        if (!checkAllPermissions(this, PERMISSION_ALL, title, message, butOk, ARRAY_PERMISSIONS)) return;
        Intent intent = new Intent(this, TrackerService.class);
        boolean isServiceRun=isServiceRunningInForeground(getApplicationContext(), TrackerService.class);
        if (getBoolFromPref(R.string.key_switch_service)){
            if (!isServiceRun || fragment.isFlagRestartService()){
                if (isServiceRun) {
                    stopService(intent);
                }
                startService(intent);
                fragment.setFlagRestartService(false);
            }
        }else {
            if (isServiceRun) {
                stopService(intent);
            }
        }
        /*if (isServiceRunningInForeground(getApplicationContext(), TrackerService.class)) {
            stopService(intent);
        }
        if (getBoolFromPref(R.id.key_switch_service) && fragment.isFlagRestartService()){
            startService(intent);
            fragment.setFlagRestartService(false);
        }*/
    }
}
