package com.truetech.xltracker.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.truetech.xltracker.R;
import com.truetech.xltracker.service.TrackerService;

import static com.truetech.xltracker.Utils.CheckPermissions.ARRAY_PERMISSIONS;
import static com.truetech.xltracker.Utils.CheckPermissions.hasPermissions;
import static com.truetech.xltracker.Utils.Util.getBoolFromPref;

public class BootReceived extends BroadcastReceiver {
    public BootReceived() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
            if (hasPermissions(context,ARRAY_PERMISSIONS) && getBoolFromPref(R.string.key_switch_service)) {
                context.startService(new Intent(context, TrackerService.class));
            }
    }
}


