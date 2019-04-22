package com.truetech.xltracker.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import com.truetech.xltracker.R;
import com.truetech.xltracker.ScannerActivity;

import java.util.regex.Pattern;

import static com.truetech.xltracker.Utils.Util.getStringFromPref;
import static com.truetech.xltracker.Utils.Util.showDialog;


/**
 * Created by Ajwar on 09.06.2017.
 */
public class SettingsFragment extends PreferenceFragment {
    private boolean flagRestartService;
    Button btn;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);
        flagRestartService=false;
        SwitchPreference onSwitch = (SwitchPreference) findPreference(getString(R.string.key_switch_service));

        initEditText();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        LinearLayout v = (LinearLayout) super.onCreateView(inflater, container, savedInstanceState);

        btn = new Button(getActivity().getApplicationContext());
        btn.setText("Scan QR");
        v.addView(btn);
        btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(getView().getContext(), ScannerActivity.class);
                startActivity(intent);
            }
        });

        return v;
    }


    @Override
    public void onStart() {
        super.onStart();
        setPreferenceScreen(null);
        addPreferencesFromResource(R.xml.settings);
        if(getStringFromPref(R.string.key_authorization) == null) btn.setVisibility(View.VISIBLE);
        else if(getStringFromPref(R.string.key_authorization).equals("")) btn.setVisibility(View.VISIBLE);
        else btn.setVisibility(View.GONE);
        initEditText();
    }

    private void initListenerEditText(final EditTextPreference p, final String title){
        p.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                String newValueStr=((String)newValue).trim();
                try {
                    Integer.parseInt(newValueStr);
                    String newTitle = title+newValueStr;
                    if (!preference.getTitle().equals(newTitle) && preference!= findPreference(getString(R.string.key_port))){
                        flagRestartService=true;
                    }
                    preference.setTitle(newTitle);
                } catch (Exception e) {
                    String title = getString(R.string.title_dialog_preference);
                    String message = getString(R.string.text_dialog_preference);
                    String butOk = getString(R.string.ok);
                    showDialog(getActivity(), title, message, butOk, null);
                    return  false;
                }
                return true;
            }
        });
    }
    private void initEditText(){
        try {
            String strServer=getString(R.string.server);
            String strPort=getString(R.string.port);
            String strRegData=getString(R.string.time_reg_data);
            String strSendData=getString(R.string.time_send_data);
            String strMinDistance=getString(R.string.min_distance_provider);
            String strAuthorization = getString(R.string.key_authorization_title);

            EditTextPreference server= (EditTextPreference) findPreference(getString(R.string.key_server));
            server.setTitle(strServer+getStringFromPref(R.string.key_server));


            EditTextPreference port= (EditTextPreference) findPreference(getString(R.string.key_port));
            port.setTitle(strPort+getStringFromPref(R.string.key_port));

            EditTextPreference regData= (EditTextPreference) findPreference(getString(R.string.key_reg_data));
            regData.setTitle(strRegData+getStringFromPref(R.string.key_reg_data));

            EditTextPreference sendData= (EditTextPreference) findPreference(getString(R.string.key_send_data));
            sendData.setTitle(strSendData+getStringFromPref(R.string.key_send_data));

            EditTextPreference minDistance= (EditTextPreference) findPreference(getString(R.string.key_min_distance_provider));
            minDistance.setTitle(strMinDistance+getStringFromPref(R.string.key_min_distance_provider));

            EditTextPreference keyAuthorization= (EditTextPreference) findPreference(getString(R.string.key_authorization));
            keyAuthorization.setTitle(strAuthorization+" "+getStringFromPref(R.string.key_authorization));

            initListenerEditTextServer(server,strServer);
            initListenerEditText(port,strPort);
            initListenerEditText(regData,strRegData);
            initListenerEditText(sendData,strSendData);
            initListenerEditText(minDistance,strMinDistance);
            initListenerEditTextAuthorization(keyAuthorization,strAuthorization);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void initListenerEditTextServer(final EditTextPreference p, final String title){
        p.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                String newValueStr=((String)newValue).trim();
                try {
                    if(!Pattern.matches("([0-9]{1,3}[\\.]){3}[0-9]{1,3}", newValueStr)) throw  new Exception();
                    String newTitle = title+newValueStr;
                    if (!preference.getTitle().equals(newTitle) && preference!= findPreference(getString(R.string.key_port))){
                        flagRestartService=true;
                    }
                    preference.setTitle(newTitle);
                } catch (Exception e) {
                    String title = getString(R.string.title_dialog_preference);
                    String message = getString(R.string.text_dialog_preference);
                    String butOk = getString(R.string.ok);
                    showDialog(getActivity(), title, message, butOk, null);
                    return  false;
                }
                return true;
            }
        });
    }

    private void initListenerEditTextAuthorization(final EditTextPreference p, final String title){
        p.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if(newValue.equals("")) {
                    p.setTitle(title);
                    btn.setVisibility(View.VISIBLE);
                    return true;
                } else {
                    String newValueStr=((String)newValue).trim();
                    String newTitle = title+" "+newValueStr;
                    flagRestartService=true;
                    preference.setTitle(newTitle);
                    return true;
                }
            }
        });
    }

    public boolean isFlagRestartService() {
        return flagRestartService;
    }

    public void setFlagRestartService(boolean flagRestartService) {
        this.flagRestartService = flagRestartService;
    }
}
