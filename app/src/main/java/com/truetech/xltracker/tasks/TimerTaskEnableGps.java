package com.truetech.xltracker.tasks;

import android.app.Notification;
import com.truetech.xltracker.R;
import com.truetech.xltracker.service.TrackerService;

import java.util.TimerTask;

import static com.truetech.xltracker.Utils.Util.*;

/**
 * Created by Ajwar on 27.10.2017.
 */
public class TimerTaskEnableGps extends TimerTask {
    private TrackerService service;

    public TimerTaskEnableGps(TrackerService service) {
        this.service = service;
    }

    @Override
    public void run() {
        cancelNotification(service, R.string.key_notification_enable_gps);
        if (!isGPSEnable()){
            Notification notif=getNotification(service,false,true,R.mipmap.min_icon_transparent,R.drawable.icon,
                    R.string.notification_ticker_enable_gps,R.string.notification_title_enable_gps,R.string.notification_message_enable_gps);
            viewNotification(service,notif,R.string.key_notification_enable_gps);
        }
    }
}
